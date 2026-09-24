package de.bastion.medieval.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [displayName] is the bare direction ("Norden", "north"), [toward] is how a path is shown
 * in the exit list and on buttons ("nach Norden", "north").
 */
@Serializable
enum class Direction(val displayName: LocalizedText, val toward: LocalizedText) {
    @SerialName("north") NORTH(LocalizedText("Norden", "north"), LocalizedText("nach Norden", "north")),
    @SerialName("northeast") NORTHEAST(LocalizedText("Nordosten", "northeast"), LocalizedText("nach Nordosten", "northeast")),
    @SerialName("east") EAST(LocalizedText("Osten", "east"), LocalizedText("nach Osten", "east")),
    @SerialName("southeast") SOUTHEAST(LocalizedText("Südosten", "southeast"), LocalizedText("nach Südosten", "southeast")),
    @SerialName("south") SOUTH(LocalizedText("Süden", "south"), LocalizedText("nach Süden", "south")),
    @SerialName("southwest") SOUTHWEST(LocalizedText("Südwesten", "southwest"), LocalizedText("nach Südwesten", "southwest")),
    @SerialName("west") WEST(LocalizedText("Westen", "west"), LocalizedText("nach Westen", "west")),
    @SerialName("northwest") NORTHWEST(LocalizedText("Nordwesten", "northwest"), LocalizedText("nach Nordwesten", "northwest")),
    @SerialName("up") UP(LocalizedText("oben", "up"), LocalizedText("hinauf", "up")),
    @SerialName("down") DOWN(LocalizedText("unten", "down"), LocalizedText("hinab", "down")),
    @SerialName("in") IN(LocalizedText("drinnen", "in"), LocalizedText("hinein", "in")),
    @SerialName("out") OUT(LocalizedText("draußen", "out"), LocalizedText("hinaus", "out")),
}
