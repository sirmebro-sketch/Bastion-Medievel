package de.bastion.medieval.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveGameTest {
    private val world = World.loadDefault()

    @Test
    fun `a save restores state and log`() {
        val game = Game(world)
        val log = game.opening(Language.DE) + game.submit("n", Language.DE) + game.submit("nimm den Pfeil", Language.DE)
        val text = SaveGame.encode(SaveGame(state = game.state, log = log))
        val restored = SaveGame.decode(text, world)!!
        assertEquals(game.state, restored.state)
        assertEquals(log, restored.log)
    }

    @Test
    fun `broken or foreign saves are rejected`() {
        assertNull(SaveGame.decode("not json", world))
        assertNull(SaveGame.decode("""{"state": {"location": "moon", "itemPlaces": {}}}""", world))
    }

    @Test
    fun `saves survive content changes`() {
        val text = """{"state": {"location": "crossroads", "previous": "gone", "itemPlaces": {"ghost": "crossroads"}}}"""
        val state = SaveGame.decode(text, world)!!.state
        assertNull(state.previous)
        assertNull(state.itemPlaces["ghost"])
        // Items the save does not know yet appear where the world puts them.
        assertEquals("old_well", state.itemPlaces["rusty_key"])
    }
}
