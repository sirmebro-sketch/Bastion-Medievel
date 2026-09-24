package de.bastion.medieval.engine

/** Fixed texts of the game voice. `{0}` marks where a name or list is inserted. */
internal object Messages {
    private fun t(de: String, en: String) = LocalizedText(de, en)

    val and = t("und", "and")
    val or = t("oder", "or")

    val notUnderstood = t("Das verstehe ich nicht.", "I don't understand that.")
    val notUnderstoodHint = t(
        "Schreib einfach, was du tun möchtest – etwa „geh nach Norden“, „untersuche …“ oder „sprich mit …“. " +
            "Unten findest du Vorschläge, „hilfe“ zeigt mehr.",
        "Just write what you want to do – for example “go north”, “examine …” or “talk to …”. " +
            "There are suggestions below, and “help” shows more.",
    )
    val help = t(
        "Du spielst, indem du schreibst, was deine Figur tun soll. Zum Beispiel:\n" +
            "• geh nach Norden – oder kurz n, o, s, w\n" +
            "• schau dich um\n" +
            "• untersuche … / sieh dir … an\n" +
            "• nimm … / leg … ab\n" +
            "• öffne …\n" +
            "• sprich mit …\n" +
            "• inventar – oder kurz i\n" +
            "• zurück\n" +
            "Wenn du nicht weiterweißt, tippe auf einen der Vorschläge.",
        "You play by writing what your character should do. For example:\n" +
            "• go north – or just n, e, s, w\n" +
            "• look around\n" +
            "• examine … / look at …\n" +
            "• take … / drop …\n" +
            "• open …\n" +
            "• talk to …\n" +
            "• inventory – or just i\n" +
            "• back\n" +
            "If you are stuck, tap one of the suggestions.",
    )

    val whatExamine = t("Was möchtest du untersuchen?", "What do you want to examine?")
    val whatTake = t("Was möchtest du nehmen?", "What do you want to take?")
    val whatDrop = t("Was möchtest du ablegen?", "What do you want to put down?")
    val whatOpen = t("Was möchtest du öffnen?", "What do you want to open?")
    val whoTalk = t("Mit wem möchtest du sprechen?", "Who do you want to talk to?")
    val whereGo = t("Wohin möchtest du gehen?", "Where do you want to go?")

    val noSuchThing = t("Das siehst du hier nirgends.", "You don't see that here.")
    val ambiguous = t("Was genau meinst du: {0}?", "Which do you mean: {0}?")

    val taken = t("Du nimmst {0}.", "You take {0}.")
    val alreadyCarrying = t("{0} trägst du bereits bei dir.", "You already have {0}.")
    val notPortable = t("{0} lässt sich nicht mitnehmen.", "{0} can't be taken.")
    val personNotPortable = t("Das würde sich {0} kaum gefallen lassen.", "{0} would hardly stand for that.")
    val dropped = t("Du legst {0} ab.", "You put down {0}.")
    val notCarrying = t("{0} trägst du nicht bei dir.", "You aren't carrying {0}.")
    val inventoryEmpty = t("Du trägst nichts bei dir.", "You aren't carrying anything.")
    val inventory = t("Du trägst bei dir: {0}.", "You are carrying {0}.")

    val talkSilent = t("{0} schweigt.", "{0} says nothing.")
    val talkToThing = t("{0} antwortet nicht. Was hattest du erwartet?", "{0} doesn't answer. What did you expect?")
    val nobodyHere = t("Hier ist niemand, mit dem du sprechen könntest.", "There is no one here to talk to.")

    val noWay = t("In diese Richtung führt kein Weg.", "There is no way in that direction.")
    val noWayThere = t("Dorthin führt von hier aus kein Weg.", "No path leads there from here.")
    val noBack = t("Du weißt nicht mehr genau, woher du gekommen bist.", "You're no longer sure where you came from.")
    val lockedDefault = t("Der Weg ist versperrt.", "The way is blocked.")
    val unlockDefault = t("Du öffnest den Weg.", "You open the way.")
    val alreadyOpen = t("Der Weg dorthin ist bereits offen.", "That way is already open.")
    val notLocked = t("Da gibt es nichts aufzuschließen – der Weg ist frei.", "There is nothing to unlock – the way is clear.")
    val cannotOpen = t("{0} lässt sich nicht öffnen.", "{0} can't be opened.")
    val wait = t("Du hältst inne. Die Zeit verstreicht.", "You pause. Time passes.")

    val youSee = t("Du siehst hier {0}.", "You see {0} here.")
    val personHere = t("{0} ist hier.", "{0} is here.")
    val peopleHere = t("Hier sind {0}.", "{0} are here.")
    val exitOne = t("Ein Weg führt {0}.", "A path leads {0}.")
    val exitsMany = t("Wege führen {0}.", "Paths lead {0}.")
    val noExits = t("Kein Weg führt von hier fort.", "No path leads away from here.")

    // Suggestion buttons: label and the command they send.
    val goLabel = t("{0}", "Go {0}")
    val goCommand = t("geh {0}", "go {0}")
    val talkLabel = t("Mit {0} sprechen", "Talk to {0}")
    val talkCommand = t("sprich mit {0}", "talk to {0}")
    val takeLabel = t("{0} nehmen", "Take {0}")
    val takeCommand = t("nimm {0}", "take {0}")
    val examineLabel = t("{0} ansehen", "Examine {0}")
    val examineCommand = t("untersuche {0}", "examine {0}")
    val lookLabel = t("Umsehen", "Look around")
    val lookCommand = t("schau dich um", "look around")
    val inventoryLabel = t("Inventar", "Inventory")
    val inventoryCommand = t("inventar", "inventory")
}
