# Spieldesign – Bastion Medieval

Stand: 24.09.2026. Dieses Dokument hält fest, was entschieden ist und was noch
offen ist. Es wird mit dem Spiel fortgeschrieben.

## Entschiedene Grundlagen

| Thema | Entscheidung | Warum |
|---|---|---|
| Plattform | Native Android-App, Kotlin + Jetpack Compose | Beste Kontrolle über Oberfläche, Animationen und Tastatur. Compose ist Googles empfohlenes Toolkit für native Oberflächen. |
| Netz | Vollständig offline, keine Internet-Berechtigung | Wunsch des Projekts; der Build bricht ab, falls je eine Internet-Berechtigung in die APK gerät. |
| Sprache | Deutsch zuerst, Englisch immer vollständig mitgepflegt | Jeder Text hat `de` und `en`; fehlende Übersetzungen lassen die Tests fehlschlagen. |
| Eingabe | Freie Texteingabe als Kern, Vorschläge als Knöpfe zum Ausweichen | Das Spiel ist auf Tippen ausgelegt; die Knöpfe helfen, wenn man nicht weiter weiß. |
| Optik | Pergamentseite im Lederrahmen, Gold und Weinrot; Cinzel, EB Garamond, Fraktur-Initialen | Mittelalterlich, aber gut lesbar für lange Texte. |
| Architektur | Engine (reines Kotlin) getrennt von der App; Welt als Daten (`world.json`) | Die Welt kann wachsen, ohne dass der Code unübersichtlich wird; die Logik ist ohne Android testbar. |

## Der Parser

Der Parser versteht Deutsch und Englisch gleichzeitig. Die aktive Sprache hat
Vorrang, die andere dient als Rückfall.

Was er heute kann:

- Befehle als Imperativ, 1. Person oder Infinitiv am Ende: „nimm den Schlüssel“,
  „ich gehe nach Norden“, „ich will den Schlüssel nehmen“
- trennbare Verben: „heb den Schlüssel auf“, „sieh dich um“, „schließ das Tor auf“
- Kurzformen: n/o/s/w, i, l, z
- gebeugte Wörter und kleine Tippfehler: „rostigen“, „Schlussel“, „Wegstien“
- Ziele über Stichwörter: „geh in die Schänke“, „geh zum Brunnen“
- nur ein Gegenstand ohne Verb wird als Ansehen gedeutet: „Wegstein“

Verben heute: umsehen, untersuchen, gehen, zurück, nehmen, ablegen, öffnen,
Schloss knacken, aufbrechen, Inventar, sprechen, Charakterbogen, Lizenzen, Hilfe,
warten.

## Die Spielfigur

Entschieden am 24.09.2026 mit dem Projektinhaber.

### Regelgrundlage
Die Werte folgen dem etablierten System der fünften Edition, genauer dem
**Systemreferenzdokument 5.2.1** (offizielle deutsche Fassung, CC-BY-4.0):
sechs Attribute (Stärke, Geschicklichkeit, Konstitution, Intelligenz, Weisheit,
Charisma), 18 Fertigkeiten, Proben mit W20 + Modifikator (+ Übungsbonus) gegen
einen Schwierigkeitsgrad (SG), Vorteil/Nachteil, Stufen 1–20 nach
Erfahrungspunkten. Die vorgeschriebene Namensnennung steht im Spiel unter
„Lizenzen“. Der Name „Dungeons & Dragons“ wird nicht verwendet; erlaubt ist
„kompatibel mit der fünften Edition“.

### Die Figur
- ist **jung, etwa 18–20** – das genaue Alter kennt sie selbst nicht. Alle
  Hintergründe sind so geschrieben, dass das zusammenpasst.
- **Geschlecht** ist wählbar (männlich/weiblich) und beeinflusst Anrede,
  Reaktionen und später Entscheidungen und Ereignisse.
- kann grundsätzlich **Magie** wirken, weiß es aber noch nicht. Magie wird später
  relevant und bekommt einen eigenen Namen. Der Charaktereditor enthält deshalb
  bewusst noch nichts dazu.

### Charaktererschaffung (Ablauf im Spiel)
1. Geschlecht → 2. Name → 3. Volk → 4. Werdegang → 5. Hintergrund →
6. Attribute (Punktkauf: 27 Punkte, Werte 8–15, Vorschlag je Werdegang) →
7. Merkmal (Menschen: zwei) → 8. Zusatzfertigkeit (nur Menschen) →
9. Zusammenfassung als Charakterbogen, alles änderbar, dann „los“.

### Völker
Mensch, Zwerg, Elf, Halbling (Merkmale nach SRD, für ein Textspiel übersetzt) und
der **Erdling** – ein eigenes Volk, das noch ausgearbeitet wird (derzeit
Platzhalter ohne Sonderregeln).

### Werdegänge statt Klassen
Keine Klassen, sondern eine Vorerfahrung. Jeder Werdegang gibt zwei geübte
Fertigkeiten, eine besondere Fähigkeit, Startausrüstung und – automatisch
passend – eine **Schwäche** (Nachteil auf eine Fertigkeit):

| Werdegang | Fähigkeit | Schwäche |
|---|---|---|
| Raufbold | Kampferprobt | Hitzkopf (Überzeugen) |
| Langfinger | Schlösser knacken | Ungebildet (Nachforschungen) |
| Fährtenleser | Spuren lesen | Menschenscheu (Auftreten) |
| Kräuterkundige(r) | Wunden versorgen | Sanftmütig (Einschüchtern) |
| Schreiber | Belesen | Stubenhocker (Athletik) |
| Spielmann/Spielfrau | Silberzunge | Großmaul (Heimlichkeit) |
| Handwerker | Kundiges Auge | Bodenständig (Arkane Kunde) |
| Händler | Feilschen | Verwöhnt (Überlebenskunst) |
| Künstler | Auge für Details | Träumer (Wahrnehmung) |
| Knappe/Knappin | Ritterliche Schule | Grundehrlich (Täuschen) |
| Chorknabe/Chormädchen | Kirchenkind | Weltfremd (Motiv erkennen) |

### Hintergründe
Geben wie im SRD +2/+1 auf Attribute und setzen **Story-Flags**, auf die die Welt
reagiert: Verstoßenes Ziehkind, Deserteur, Gossenkind, Entlaufenes
Klosterkind, Grenzlandflüchtling, Schuldner.

### Wie die Welt reagiert (Werkzeuge für Inhalte)
- Texte mit `variants` und Bedingungen (`gender`, `species`, `career`,
  `background`, `trait`, `feature`, `flag`) – z. B. begrüßt Marta einen
  Chorknaben anders als eine Elfe.
- `[[männlich|weiblich]]` im Text wählt die Form nach Geschlecht, `{name}`
  setzt den Namen ein.
- Hindernisse mit Proben: verschlossene Wege lassen sich knacken (nur mit der
  Fähigkeit „Schlösser knacken“) oder aufbrechen – je ein Versuch.
- Geheimnisse an Gegenständen: der erste genaue Blick löst eine Probe aus.
- Erfahrungspunkte für geöffnete Wege, Geheimnisse und erste Besuche.
- Würfelproben sind sichtbar und im Menü abschaltbar.

### Stufenaufstieg
Erfahrungspunkte und Stufen nach SRD-Tabelle sind eingebaut (Trefferpunkte und
Übungsbonus steigen). Wie sich die Figur beim Aufstieg sonst entwickelt
(„Skalierung“), wird später festgelegt.

## Die Welt – „so groß wie lokal möglich“

Geplanter Weg zu einer sehr großen, tiefen Welt ohne Internet:

1. **Handgeschriebener Kern**: Hauptgeschichte, wichtige Orte und Figuren – von
   Hand in beiden Sprachen.
2. **Baukasten statt Einzeltexte**: Orte, Dinge und Figuren aus Bausteinen
   (Grammatik wird generiert, Beschreibungen aus Textbausteinen mit Varianten).
3. **Erzeugte Regionen**: Wildnis, Dörfer, Höhlen und Nebenaufgaben werden aus
   einem festen Startwert (Seed) erzeugt. Dieselbe Welt entsteht so jedes Mal
   gleich und braucht kaum Speicher, kann aber praktisch endlos sein.
4. **Zustand statt Kopie**: Gespeichert wird nur, was sich verändert hat
   (`GameState`), nie die ganze Welt.

## Offene Punkte

- **Story**: Grundidee, Hauptkonflikt und Ton der Geschichte kommen vom Projekt
  selbst (in Arbeit). Das jetzige Testgebiet wird danach ersetzt.
- **Erdling**: Beschreibung und Regeln des eigenen Volks
- **Magie**: Name, Entdeckung und Regeln
- **Skalierung beim Stufenaufstieg**
- Ausrüstung als echte Gegenstände (derzeit nur auf dem Charakterbogen), Münzen ausgeben
- Kürzere Ortsbeschreibung bei wiederholtem Besuch
- Fürwörter im Parser („nimm ihn“, „sprich mit ihr“)
- Gesprächsthemen („frag Marta nach dem Schlüssel“)
- Tag/Nacht, Wetter, Klang
