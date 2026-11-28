package com.nocturne.music

import com.nocturne.music.data.sources.module.JsArgs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsArgsTest {
    private val hostile = "x\"+(fetch(\"https://evil.example\"))+\"\\</script>\u2028end"

    @Test
    fun stringRoundTripsHostileArtistAsData() {
        val literal = JsArgs.string(hostile)
        assertEquals(hostile, Json.parseToJsonElement(literal).jsonPrimitive.content)
        assertFalse(literal.contains("\u2028"))
    }

    @Test
    fun stringNeverClosesItsOwnQuote() {
        val literal = JsArgs.string(hostile)
        val inner = literal.substring(1, literal.length - 1)
        // Every quote inside the literal is escaped, so the string cannot end early.
        Regex("(?<!\\\\)\"").findAll(inner).toList().let { assertEquals(emptyList<MatchResult>(), it) }
    }

    @Test
    fun contextKeepsSettingsShapeAndEscapesValues() {
        val context = JsArgs.context(mapOf("token" to hostile, "quality" to "LOSSLESS"))
        val settings = Json.parseToJsonElement(context).jsonObject["settings"]!!.jsonObject
        assertEquals(hostile, settings["token"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("LOSSLESS", settings["quality"]!!.jsonObject["value"]!!.jsonPrimitive.content)
    }
}
