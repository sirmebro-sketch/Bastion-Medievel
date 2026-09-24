package de.bastion.medieval.engine

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorldTest {
    @Test
    fun `bundled world is consistent in both languages`() {
        val world = World.loadDefault()
        assertEquals(emptyList(), world.validate())
    }

    @Test
    fun `a missing translation fails to load`() {
        val json = """
            {"title": {"de": "T"}, "intro": {"de": "I", "en": "I"}, "start": "a",
             "locations": [{"id": "a", "name": {"de": "A", "en": "A"}, "description": {"de": "D", "en": "D"}}]}
        """.trimIndent()
        assertFailsWith<SerializationException> { World.parse(json) }
    }

    @Test
    fun `validation reports broken references and blank texts`() {
        val json = """
            {"title": {"de": "T", "en": "T"}, "intro": {"de": "I", "en": " "}, "start": "a",
             "locations": [
               {"id": "a", "name": {"de": "A", "en": "A"}, "description": {"de": "D", "en": "D"},
                "exits": [{"direction": "north", "to": "nowhere"}], "items": ["ghost"]},
               {"id": "b", "name": {"de": "B", "en": "B"}, "description": {"de": "D", "en": "D"}}
             ]}
        """.trimIndent()
        val errors = World.parse(json).validate()
        assertTrue(errors.any { "intro: English text is missing" in it }, errors.toString())
        assertTrue(errors.any { "unknown 'nowhere'" in it }, errors.toString())
        assertTrue(errors.any { "unknown item 'ghost'" in it }, errors.toString())
        assertTrue(errors.any { "'b' cannot be reached" in it }, errors.toString())
    }
}
