package com.nocturne.music.data.sources.module

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Turns host values into JavaScript source for a module call.
 *
 * Arguments are pasted into code the engine evaluates, so every value goes through JSON
 * encoding here: a quote or backslash in an artist name must stay data, never become code.
 */
internal object JsArgs {
    /** [value] as a JS string literal; U+2028 and U+2029 are escaped too, since older parsers end a line on them. */
    fun string(value: String): String =
        JsonPrimitive(value).toString()
            .replace(" ", "\\u2028")
            .replace(" ", "\\u2029")

    /** The `context` argument every export takes: `{settings:{<key>:{value:<value>}}}`. */
    fun context(settings: Map<String, String>): String =
        buildJsonObject {
            putJsonObject("settings") {
                settings.forEach { (key, value) -> putJsonObject(key) { put("value", value) } }
            }
        }.toString()
            .replace(" ", "\\u2028")
            .replace(" ", "\\u2029")
}
