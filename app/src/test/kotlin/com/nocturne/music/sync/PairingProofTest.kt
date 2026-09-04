package com.nocturne.music.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingProofTest {

    // Literal digests, not a re-derivation: a test that hashes the same way as the code under test
    // agrees with any construction, including a wrong one. The desktop accepted the second vector
    // over a live handshake, which is what ties these values to the Rust side.
    @Test
    fun `proof matches the digests the desktop accepts`() {
        assertEquals(
            "1755a2d5b309d75bede226f2b6cf858bb14287b917e76ad3b943de0dda4683fb",
            pinProof("abc", "1234"),
        )
        assertEquals(
            "b3598788edf4ee65ebb812b24e17d3ea88e43a6866b377b63c795f2be34c1ccc",
            pinProof("fd63a2173d04fdf561260a32627c38c6", "559691"),
        )
    }

    // A construction that dropped the colon would hash "a" + "bc" and "ab" + "c" identically.
    @Test
    fun `the colon separator is part of the hashed input`() {
        assertNotEquals(pinProof("a", "bc"), pinProof("ab", "c"))
    }

    // This digest starts with 0x0d, so formatting a byte without the zero pad yields 63 chars.
    @Test
    fun `every byte renders as two lowercase hex digits`() {
        val proof = pinProof("nonce52", "1234")
        assertEquals("0df02054a3d7ff5ec21b3dab9b55710b33aeb2d9190888529e6efc7eda4f39bd", proof)
        assertEquals(64, proof.length)
        assertTrue(proof.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `a different nonce or pin gives a different proof`() {
        val base = pinProof("abc", "1234")
        assertNotEquals(base, pinProof("abd", "1234"))
        assertNotEquals(base, pinProof("abc", "1235"))
        assertEquals(base, pinProof("abc", "1234"))
    }

    // The desktop rejects anything outside 4 to 8 digits, but the proof itself must stay total:
    // connect() screens the PIN, and a throw here would crash the handshake instead of failing it.
    @Test
    fun `an empty nonce or pin still produces a well formed proof`() {
        for (proof in listOf(pinProof("", ""), pinProof("", "1234"), pinProof("abc", ""))) {
            assertEquals(64, proof.length)
            assertTrue(proof.all { it in '0'..'9' || it in 'a'..'f' })
        }
    }
}
