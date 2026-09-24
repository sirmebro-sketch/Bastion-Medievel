package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind

/** The character sheet as story-log paragraphs (summary of creation and the "charakter" command). */
object SheetView {
    private fun t(de: String, en: String) = LocalizedText(de, en)

    private val people = t("Volk", "People")
    private val career = t("Werdegang", "Former life")
    private val background = t("Hintergrund", "Background")
    private val gender = t("Geschlecht", "Gender")
    private val age = t("Alter", "Age")
    private val ageValue = t("unbekannt, etwa 18–20", "unknown, about 18–20")
    private val level = t("Stufe", "Level")
    private val levelValue = t("{0} ({1} / {2} EP)", "{0} ({1} / {2} XP)")
    private val levelMax = t("{0} ({1} EP)", "{0} ({1} XP)")
    private val hp = t("Trefferpunkte", "Hit points")
    private val proficiency = t("Übungsbonus", "Proficiency bonus")
    private val coins = t("Münzen", "Coins")
    private val luck = t("Glückspunkte", "Luck points")
    private val trained = t("Geübt — {0}", "Trained — {0}")
    private val advantage = t("Im Vorteil bei: {0}", "Advantage on: {0}")
    private val disadvantage = t("Im Nachteil bei: {0}", "Disadvantage on: {0}")
    private val weakness = t("Schwäche", "Weakness")
    private val trait = t("Merkmal", "Trait")
    private val equipment = t("Ausrüstung: {0}", "Equipment: {0}")
    private val colAbility = t("Attribut", "Ability")
    private val colScore = t("Wert", "Score")
    private val colMod = t("Mod.", "Mod.")

    fun render(sheet: Sheet, language: Language): List<Paragraph> {
        val c = sheet.character
        val ctx = TextContext(c)
        fun r(text: LocalizedText) = text.render(language, ctx)
        val nextXp = Rules.xpFor(sheet.level + 1)

        val general = buildList {
            add(people[language] to r(sheet.species.name))
            add(career[language] to r(sheet.career.name))
            add(background[language] to r(sheet.background.name))
            add(gender[language] to c.gender.displayName[language])
            add(age[language] to ageValue[language])
            add(
                level[language] to if (nextXp != null) {
                    levelValue.format(language, "${sheet.level}", "${c.xp}", "$nextXp")
                } else {
                    levelMax.format(language, "${sheet.level}", "${c.xp}")
                },
            )
            add(hp[language] to "${sheet.hp} / ${sheet.maxHp}")
            add(proficiency[language] to Rules.signed(sheet.proficiencyBonus))
            add(coins[language] to "${c.coins}")
            if (sheet.traits.any { it.luck }) add(luck[language] to "${sheet.luckLeft}")
        }.joinToString("\n") { (key, value) -> "$key\t$value" }

        val abilities = (listOf("${colAbility[language]}\t${colScore[language]}\t${colMod[language]}") +
            Ability.entries.map { a -> "${a.displayName[language]}\t${sheet.score(a)}\t${Rules.signed(sheet.modifier(a))}" })
            .joinToString("\n")

        val skills = sheet.proficientSkills.sortedBy { it.displayName[language] }
            .joinToString(", ") { "${it.displayName[language]} ${Rules.signed(sheet.skillBonus(it))}" }
        val withAdvantage = Skill.entries.filter { sheet.edge(it) == Edge.ADVANTAGE }.map { it.displayName[language] }
        val withDisadvantage = Skill.entries.filter { sheet.edge(it) == Edge.DISADVANTAGE }.map { it.displayName[language] }

        return buildList {
            add(Paragraph(Kind.TITLE, c.name))
            add(Paragraph(Kind.TABLE, general))
            add(Paragraph(Kind.TABLE, abilities))
            add(Paragraph(Kind.OPTION, trained.format(language, skills)))
            if (withAdvantage.isNotEmpty()) add(Paragraph(Kind.HINT, advantage.format(language, withAdvantage.joinToString(", "))))
            if (withDisadvantage.isNotEmpty()) add(Paragraph(Kind.HINT, disadvantage.format(language, withDisadvantage.joinToString(", "))))
            for (feature in sheet.species.features + sheet.career.feature) {
                add(Paragraph(Kind.OPTION, "${r(feature.name)} — ${r(feature.description)}"))
            }
            for (t in sheet.traits) {
                add(Paragraph(Kind.OPTION, "${trait[language]}: ${r(t.name)} — ${r(t.description)}"))
            }
            val w = sheet.career.weakness
            add(Paragraph(Kind.OPTION, "${weakness[language]}: ${r(w.name)} — ${r(w.description)}"))
            add(Paragraph(Kind.OPTION, "${r(sheet.background.name)} — ${r(sheet.background.consequences)}"))
            if (sheet.equipment.isNotEmpty()) {
                add(Paragraph(Kind.HINT, equipment.format(language, sheet.equipment.joinToString(", ") { r(it) })))
            }
        }
    }
}
