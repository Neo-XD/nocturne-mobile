/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch

import com.music.musixmatch.models.RichSyncEntry
import com.music.musixmatch.models.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MusixmatchTest {

    @Test
    fun testConvertRichSyncToLrc() {
        val entries = listOf(
            RichSyncEntry(
                ts = 32.24,
                te = 34.033,
                l = listOf(
                    WordEntry("This", 0.0),
                    WordEntry(" ", 0.391),
                    WordEntry("is", 0.436),
                    WordEntry(" ", 0.559),
                    WordEntry("the", 0.627),
                    WordEntry(" ", 1.227),
                    WordEntry("end", 1.287)
                ),
                x = "This is the end"
            )
        )

        val lrc = Musixmatch.convertRichSyncToLrc(entries)
        
        // Expected formatted LRC (inline RichSync):
        // [00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end
        val expected = "[00:32.240]<00:32.240>This <00:32.676>is <00:32.867>the <00:33.527>end\n"
        assertEquals(expected, lrc)
    }

    @Test
    fun testConvertSubtitleJsonToLrc() {
        val json = """[{"text":"This is the first line","time":{"total":12.345}},{"text":"Second line","time":{"total":15.0}}]"""
        val lrc = Musixmatch.convertSubtitleJsonToLrc(json)
        
        val expected = "[00:12.345]This is the first line\n[00:15.000]Second line\n"
        assertEquals(expected, lrc)
    }
    
    @Test
    fun testMultiArtistClean() {
        val input = "Taylor Swift, Ed Sheeran & Someone Else (feat. Guest) [Remix]"
        val cleaned = Musixmatch.cleanText(input)
        
        // Should preserve space for each word separated by comma or ampersand
        val expected = "taylor swift ed sheeran someone else"
        assertEquals(expected, cleaned)
    }

    // Ignored in CI, not deleted: it calls musixmatch.com and has no assertions, so it can never fail and only adds a third-party dependency to every pull request.
    @org.junit.Ignore("hits the live Musixmatch API; a debug aid, not a test")
    @Test
    fun debugKaliUchis() = kotlinx.coroutines.runBlocking {
        println("STARTING DEBUG KALI UCHIS")
        Musixmatch.getAllLyrics("all i can say", "kali uchis", 258) {
            println("LYRICS FOUND:")
            println(it.take(100))
        }
        println("FINISHED DEBUG KALI UCHIS")
    }
}
