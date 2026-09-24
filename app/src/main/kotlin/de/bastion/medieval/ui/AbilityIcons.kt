package de.bastion.medieval.ui

import androidx.annotation.DrawableRes
import de.bastion.medieval.R
import de.bastion.medieval.engine.Ability

/** The pentagon symbol of each ability; the SVG masters are in art/attribute-icons. */
@get:DrawableRes
val Ability.icon: Int
    get() = when (this) {
        Ability.STRENGTH -> R.drawable.ic_ability_strength
        Ability.DEXTERITY -> R.drawable.ic_ability_dexterity
        Ability.CONSTITUTION -> R.drawable.ic_ability_constitution
        Ability.INTELLIGENCE -> R.drawable.ic_ability_intelligence
        Ability.WISDOM -> R.drawable.ic_ability_wisdom
        Ability.CHARISMA -> R.drawable.ic_ability_charisma
    }

/**
 * The ability a table cell names, in either language: the story log keeps the language a
 * table was written in, so a switched language must still find its symbols.
 */
fun abilityNamed(text: String): Ability? =
    Ability.entries.firstOrNull { text == it.displayName.de || text == it.displayName.en }
