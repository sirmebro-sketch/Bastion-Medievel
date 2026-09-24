# Bastion Medieval

Ein textbasiertes Fantasy-Rollenspiel im mittelalterlichen Stil für Android.
Man spielt, indem man frei schreibt, was die eigene Figur tun soll („geh nach
Norden“, „sieh dir den Wegstein an“, „sprich mit der Wirtin“). Wer nicht weiter
weiß, tippt auf einen der Vorschläge unter dem Text.

- Native Android-App: Kotlin und Jetpack Compose
- Läuft komplett offline, die App hat keine Internet-Berechtigung (wird im Build geprüft)
- Deutsch als Hauptsprache, Englisch als vollständige Übersetzung, im Spiel umschaltbar
- Der Stand wird nach jedem Zug automatisch gespeichert
- Zu Beginn erschafft man seine Figur im Dialog: Geschlecht, Name, Volk, Werdegang,
  Hintergrund, Attribute (Punktkauf) und Merkmal. Die Werte folgen dem System der
  fünften Edition (SRD 5.2.1); Würfelproben sind sichtbar und abschaltbar.

Die eigentliche Geschichte ist noch nicht geschrieben. Im Spiel steckt derzeit
ein kleines **Testgebiet**: Kreuzweg, Dorf mit Schänke, Wald mit Brunnen und das
verschlossene Tor der Bastion. Es dient dazu, Parser, Oberfläche und Speichern
auszuprobieren.

## APK herunterladen

Jeder Push baut automatisch eine getestete APK (GitHub Actions, Workflow
„Android-APK“):

| Wo gepusht | Was entsteht | App auf dem Handy |
|---|---|---|
| `main` | Release unter **Releases** (`build-<Nummer>`), direkt als APK herunterladbar | „Bastion Medieval“ (`de.bastion.medieval`) |
| jeder andere Branch | Artefakt im jeweiligen Actions-Lauf | „Bastion Beta“ (`de.bastion.medieval.beta`) |

Beta und Release sind zwei getrennte Apps und lassen sich nebeneinander
installieren. Neue Builds installieren sich über die vorherigen, der Spielstand
bleibt erhalten.

### Einmalig: Signierschlüssel für `main` hinterlegen

Damit `main` die echte App baut, braucht das Repository vier Secrets
(*Settings → Secrets and variables → Actions → New repository secret*):

| Secret | Inhalt |
|---|---|
| `BASTION_KEYSTORE_BASE64` | der Keystore als Base64-Text |
| `BASTION_KEYSTORE_PASSWORD` | Passwort des Keystores |
| `BASTION_KEY_ALIAS` | `bastion` |
| `BASTION_KEY_PASSWORD` | Passwort des Schlüssels (gleich dem Keystore-Passwort) |

Solange die Secrets fehlen, baut `main` trotzdem – dann aber als Beta-App, und
der Lauf zeigt eine Warnung. Der Build prüft den Schlüssel gegen den
Fingerabdruck in `signing/release-certificate-sha256.txt`, damit nie
versehentlich mit einem falschen Schlüssel signiert wird.

**Den Keystore sicher aufbewahren.** Geht er verloren, lassen sich Updates der
installierten App nicht mehr einspielen.

## Aufbau

```
engine/   Spiellogik in reinem Kotlin, ohne Android – eigener Gradle-Build
  src/main/kotlin/…/engine/   Parser, Grammatik, Welt, Spielstand, Spiel
  src/main/resources/world/world.json   die Spielwelt (Orte, Dinge, Figuren)
  src/main/resources/rules/character.json   Völker, Werdegänge, Hintergründe, Merkmale
  src/test/…                  Tests der Spiellogik
app/      Android-App (Jetpack Compose): Oberfläche, Speichern, Sprache
  src/test/…                  Oberflächentests mit Robolectric, erzeugen Screenshots
signing/  öffentlicher Beta-Schlüssel und Fingerabdrücke (kein privater Schlüssel!)
docs/     Spieldesign und Entscheidungen
```

### Welt-Inhalte

Die Welt steht in `engine/src/main/resources/world/world.json`. Jeder Text hat
ein `de`- und ein `en`-Feld; fehlt eines, lädt die Welt nicht und die Tests
schlagen fehl. Für Dinge und Figuren werden nur Grundformen angegeben:

```json
"de": { "noun": "Schlüssel", "gender": "m", "adjectives": ["rostig"] },
"en": { "noun": "key", "adjectives": ["rusty"] }
```

Daraus erzeugt die Engine selbst „der rostige Schlüssel“, „den rostigen
Schlüssel“, „einen rostigen Schlüssel“ usw. Der Parser erkennt dieselben Wörter,
auch gebeugt und mit kleinen Tippfehlern.

## Selbst bauen

Voraussetzungen: JDK 17 oder neuer, Android-SDK (für die App).

```
./gradlew -p engine test          # Spiellogik testen, braucht kein Android-SDK
./gradlew :app:testDebugUnitTest  # Oberflächentests
./gradlew :app:assembleDebug      # Debug-APK
```

## Lizenzen

Bastion Medieval ist kompatibel mit der fünften Edition (5E).

Dieses Werk enthält Material aus dem Systemreferenzdokument 5.2.1 („SRD 5.2.1“)
von Wizards of the Coast LLC, verfügbar unter https://www.dndbeyond.com/srd. Das
SRD 5.2.1 ist lizenziert gemäß Creative Commons Namensnennung 4.0 International
Public License (verfügbar unter https://creativecommons.org/licenses/by/4.0/legalcode.de).

Cinzel, EB Garamond und UnifrakturMaguntia stehen unter der SIL Open Font
License 1.1. Die Lizenztexte liegen in `app/src/main/assets/licenses/` und werden
mit der App ausgeliefert.
