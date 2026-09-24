package de.bastion.medieval.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Everything that changes while playing. The [World] itself stays read-only, so a
 * save only needs this small object.
 *
 * [itemPlaces] maps item ids to a location id or [INVENTORY]. Items used up for good
 * are listed in [consumed] instead.
 */
@Serializable
data class GameState(
    val location: String,
    val previous: String? = null,
    val itemPlaces: Map<String, String>,
    val consumed: Set<String> = emptySet(),
    val flags: Set<String> = emptySet(),
    val visited: Set<String> = emptySet(),
    val talkProgress: Map<String, Int> = emptyMap(),
    val turns: Int = 0,
    /** The player character; null until character creation is finished. */
    val character: Character? = null,
    /** Character creation in progress; null once the character exists. */
    val creation: CreationState? = null,
    /** Seed and counter of the deterministic dice, see [Dice]. */
    val seed: Long = 0,
    val rolls: Int = 0,
) {
    val inventory: List<String> get() = itemPlaces.filterValues { it == INVENTORY }.keys.toList()

    fun itemsAt(locationId: String): List<String> = itemPlaces.filterValues { it == locationId }.keys.toList()

    companion object {
        const val INVENTORY = "@inventory"

        /** A new game; it starts with character creation. */
        fun new(world: World, seed: Long = 0): GameState = GameState(
            location = world.data.start,
            itemPlaces = world.initialItemPlaces(),
            visited = setOf(world.data.start),
            creation = CreationState(),
            seed = seed,
        )
    }
}

/**
 * One entry of the story log shown to the player. A paragraph with a [slot] replaces
 * earlier paragraphs of the same slot in the log (used for the point-buy table, which
 * would otherwise repeat after every change).
 */
@Serializable
data class Paragraph(val kind: Kind, val text: String, val slot: String? = null) {
    @Serializable
    enum class Kind {
        /** What the player typed. */
        @SerialName("input") INPUT,

        /** Name of a location the player arrives at or looks around in. */
        @SerialName("title") TITLE,

        /** Description of a location; the UI may decorate its first letter. */
        @SerialName("scene") SCENE,

        @SerialName("text") TEXT,

        @SerialName("dialogue") DIALOGUE,

        /** Exits, help and other guidance outside the story voice. */
        @SerialName("hint") HINT,

        /** A choice or entry with a name: "Name — description". */
        @SerialName("option") OPTION,

        /** A dice roll; the player can hide these. */
        @SerialName("roll") ROLL,

        /** Rows separated by line breaks, columns by tabs. */
        @SerialName("table") TABLE,
    }
}

/** A ready-made command the player can tap instead of typing. */
data class Suggestion(val label: String, val command: String)

/** A saved game: the state plus the recent story log so the player can pick up the thread. */
@Serializable
data class SaveGame(
    val version: Int = VERSION,
    val state: GameState,
    val log: List<Paragraph> = emptyList(),
) {
    companion object {
        const val VERSION = 2
        const val MAX_LOG = 300

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(save: SaveGame): String =
            json.encodeToString(serializer(), save.copy(log = save.log.takeLast(MAX_LOG)))

        /** Returns null if the text is not a usable save for [world]. */
        fun decode(text: String, world: World): SaveGame? {
            val save = try {
                json.decodeFromString(serializer(), text)
            } catch (_: SerializationException) {
                return null
            } catch (_: IllegalArgumentException) {
                return null
            }
            val state = save.state
            if (save.version > VERSION || state.location !in world.locations) return null
            // Content may have changed since the save: drop unknown items and places, and
            // put items that were added to the world since then where they belong.
            val places = state.itemPlaces.filter { (item, place) ->
                item in world.items && (place == GameState.INVENTORY || place in world.locations)
            }
            val added = world.initialItemPlaces().filterKeys { it !in places && it !in state.consumed }
            return save.copy(
                state = state.copy(
                    itemPlaces = added + places,
                    previous = state.previous?.takeIf { it in world.locations },
                ),
            )
        }
    }
}
