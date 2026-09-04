package com.nocturne.music.sync

import java.security.MessageDigest

// Must match expected_pin_hash on the desktop: lowercase hex SHA-256 over "nonce:pin". UTF-8 is
// explicit because the desktop hashes raw bytes and a JVM default charset would diverge silently.
internal fun pinProof(nonce: String, pin: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$nonce:$pin".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
