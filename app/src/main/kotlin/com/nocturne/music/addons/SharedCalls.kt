/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Adapted from BitChord (SharedCalls.kt)
 */

package com.nocturne.music.addons

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * One in-flight-or-recent answer per key, shared by everyone who asks for it.
 * Prevents redundant concurrent requests and caches successful responses with a TTL.
 */
internal class SharedCalls<T>(
    private val ttlMs: Long,
    private val scope: CoroutineScope,
    private val log: (String) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) {
    private class Pending<T>(val work: Deferred<Result<T>>, val startedAtMs: Long)

    private val entries = ConcurrentHashMap<String, Pending<T>>()
    private val lock = Mutex()

    /**
     * The answer behind [key], running [fetch] in [scope] if no usable answer is
     * in hand or already in flight.
     */
    suspend fun get(
        key: String,
        describe: () -> String = { key },
        fetch: suspend () -> T,
    ): Result<T> {
        val existing = entries[key]
        if (existing != null) {
            val ageMs = now() - existing.startedAtMs
            if (existing.work.isActive) {
                log("  ↺ in flight (${describe()})")
                return existing.work.await()
            }
            if (ttlMs > 0 && ageMs < ttlMs) {
                val outcome = existing.work.getCompleted()
                if (outcome.isSuccess) {
                    log("  ✓ cached (${describe()})")
                    return outcome
                }
            }
        }

        val pending = lock.withLock {
            val race = entries[key]
            if (race != null) {
                val ageMs = now() - race.startedAtMs
                if (race.work.isActive) return@withLock race
                if (ttlMs > 0 && ageMs < ttlMs && race.work.getCompleted().isSuccess) return@withLock race
            }
            val started = scope.async {
                runCatching { fetch() }
            }
            val created = Pending(started, now())
            entries[key] = created
            created
        }

        val result = pending.work.await()
        if (result.isFailure && entries[key] === pending) {
            entries.remove(key)
        }
        return result
    }

    fun clear() {
        entries.clear()
    }
}
