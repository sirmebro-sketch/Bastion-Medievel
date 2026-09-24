# Attributsymbole

Sechs Symbole für die Attribute, eigens für Bastion Medieval gestaltet (vom
Projektinhaber geliefert am 24.09.2026): Pentagon-Grundform in Pergament,
Bordeauxrot, Altgold und Dunkelbraun.

| Datei | Attribut | Motiv | In der App |
|---|---|---|---|
| `staerke.svg` | Stärke | gekreuzte Hanteln | `ic_ability_strength` |
| `geschicklichkeit.svg` | Geschicklichkeit | Bogen mit Pfeil | `ic_ability_dexterity` |
| `konstitution.svg` | Konstitution | Herz mit Puls | `ic_ability_constitution` |
| `intelligenz.svg` | Intelligenz | offenes Buch mit Stern | `ic_ability_intelligence` |
| `weisheit.svg` | Weisheit | Auge | `ic_ability_wisdom` |
| `charisma.svg` | Charisma | Krone | `ic_ability_charisma` |

Die SVG-Dateien hier sind die Vorlagen. Die App nutzt daraus erzeugte
Vektorgrafiken (`app/src/main/res/drawable/ic_ability_*.xml`), die in jeder
Bildschirmgröße scharf bleiben. Die App zeigt sie vor jeder Tabellenzeile, deren
erste Zelle ein Attribut nennt (Punktkauf, Charakterbogen); die Zuordnung steht in
`app/src/main/kotlin/de/bastion/medieval/ui/AbilityIcons.kt`.

## Umwandeln

1. SVG mit Androids eigenem Konverter umwandeln: `Svg2Vector.parseSvgToXml` aus
   `com.android.tools:sdk-common` in der Version passend zum Android-Gradle-Plugin
   (AGP 9.3.2 → `sdk-common:32.3.2`). Das ist derselbe Konverter wie „Vector Asset“
   in Android Studio.
2. Den Schlagschatten entfernen: Er ist ein SVG-Weichzeichner (`<filter>`), den
   Android-Vektorgrafiken nicht kennen; der Konverter meldet das und lässt nur ein
   hartes, fast verdecktes Vieleck übrig (erster `<path>` mit `M256,44`).
3. Auf das Pentagon zuschneiden, damit das Symbol mittig sitzt: `viewportWidth` und
   `viewportHeight` = 448, alle Pfade in
   `<group android:translateX="-32" android:translateY="-13">`,
   `android:width`/`height` = 24dp.
