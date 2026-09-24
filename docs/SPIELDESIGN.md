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
Inventar, sprechen, Hilfe, warten.

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
- **Charakterbogen** (Idee, noch nicht umgesetzt): eine eigene Figur wie bei
  einem Pen-&-Paper-Bogen – Werte (z. B. Stärke, Geschick, Klugheit, Charisma),
  Fertigkeiten, Lebenspunkte, Ausrüstung. Proben mit Würfeln entscheiden, ob
  Handlungen gelingen („du versuchst, das Schloss zu knacken – Geschick-Probe“).
  Dafür braucht die Engine Werte im Spielstand, eine Würfel-Mechanik mit festem
  Zufallsstartwert und eine Charaktererschaffung am Spielbeginn.
- Kürzere Ortsbeschreibung bei wiederholtem Besuch
- Fürwörter im Parser („nimm ihn“, „sprich mit ihr“)
- Gesprächsthemen („frag Marta nach dem Schlüssel“)
- Tag/Nacht, Wetter, Klang
