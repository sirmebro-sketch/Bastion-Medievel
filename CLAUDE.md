# Hinweise für Claude-Sitzungen

Bastion Medieval ist ein Text-RPG für Android (Kotlin, Jetpack Compose). Lies zuerst
`README.md` und `docs/SPIELDESIGN.md`.

## Arbeitsweise mit dem Projektinhaber

- Kommunikation auf Deutsch, ausführlich, geprüft und mit verifizierten Quellen.
  Das gilt für **alles**, was der Projektinhaber liest: Antworten, kurze
  Zwischenmeldungen während der Arbeit, Beschreibungen von Befehlen und
  Bildunterschriften. Englisch nur in Code, Bezeichnern und Commit-Nachrichten.
- Bei jeder Änderung, die in `main` landet, baut GitHub Actions eine APK. Claude ist
  dafür verantwortlich, dass dieser Build grün ist und eine APK entsteht.
- Neue Arbeit zuerst auf dem Arbeitsbranch pushen und den Beta-Build abwarten, dann
  erst nach `main` übernehmen.
- Englisch im Code und in Bezeichnern, korrekt geschrieben: „Medieval“ (der
  Repository-Name „Bastion-Medievel“ bleibt bewusst so).

## Regeln für Inhalte

- Die Welt und ihre Geschichte bestimmt allein der Projektinhaber. Alle bisherigen
  Spieltexte (Testgebiet in `world.json`, Beschreibungen von Völkern, Werdegängen,
  Hintergründen und Merkmalen in `character.json`) sind Platzhalter von Claude und
  werden nach der Welt des Projektinhabers neu geschrieben. Eigene Ideen immer als
  Vorschlag kennzeichnen, nie als Teil der Welt behandeln.
- Jeder Spieltext existiert auf Deutsch **und** Englisch (`LocalizedText`). Deutsch ist
  die Hauptsprache.
- Dinge und Figuren nur mit Grundformen anlegen (`noun`, `gender`, Adjektiv-Stämme);
  Artikel und Fälle erzeugt `GermanNoun`.
- Nach Änderungen an `world.json` müssen `WorldTest` (Konsistenz) und die übrigen
  Engine-Tests grün sein.
- Die App darf keine Internet-Berechtigung bekommen; CI prüft das.
- Figur-Inhalte stehen in `engine/src/main/resources/rules/character.json`
  (`CharacterRulesTest` prüft sie). Texte können mit `variants`/`when`,
  `[[männlich|weiblich]]` und `{name}` auf die Figur reagieren (siehe
  `docs/SPIELDESIGN.md`, Abschnitt „Die Spielfigur“).
- Regeln folgen dem SRD 5.2.1 (CC-BY-4.0); die Namensnennung in README und im Spiel
  („lizenzen“) muss erhalten bleiben. Kein „Dungeons & Dragons“ im Spiel.
- Grafiken: SVG-Vorlagen in `art/`, in der App als Vektorgrafik (`res/drawable`).
  Umwandlung mit Androids `Svg2Vector` (Schritte in `art/attribute-icons/README.md`).

## Bauen und Prüfen

- Engine: `./gradlew -p engine test` – läuft ohne Android-SDK.
- App lokal: Die Cloud-Umgebung des Projekts hat vollen Netzzugriff (seit 24.09.2026).
  Das Android-SDK ist in einer neuen Sitzung nicht vorinstalliert:
  ```
  export ANDROID_HOME=/opt/android-sdk
  # Kommandozeilentools: neueste commandlinetools-linux-*_latest.zip von dl.google.com
  # nach $ANDROID_HOME/cmdline-tools/latest entpacken, dann:
  yes | $ANDROID_HOME/cmdline-tools/latest/bin/android --sdk=$ANDROID_HOME sdk install \
    platforms/android-37.0 build-tools/37.0.0 platform-tools
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
  ```
  Die Plattform heißt `android-37.0`, nicht `android-37`. Screenshots lokal:
  `./gradlew :app:recordRoborazziDebug` → `app/build/outputs/roborazzi/`.
- CI-Workflow: `.github/workflows/android.yml`.
- Screenshots der Oberfläche erzeugt CI mit Roborazzi und legt sie auf den Branch
  `ci-screenshots` (wird bei jedem Push überschrieben):
  `git fetch origin ci-screenshots && git show origin/ci-screenshots:01_opening_de.png > /tmp/x.png`
- Versionen stammen aus Googles eigenen Beispielen (compose-samples, nowinandroid);
  beim Aktualisieren wieder dort abgleichen.
- In Cloud-Sitzungen mit gedrosseltem Maven Central hilft lokal (nicht im Repo) ein
  Gradle-Init-Skript, das auf `https://maven-central.storage-download.googleapis.com/maven2/`
  umleitet.

## Signieren

- Beta: öffentlicher Testschlüssel `signing/beta.keystore`, App-ID `de.bastion.medieval.beta`.
- Release (`main`): privater Schlüssel nur als Repository-Secret (`BASTION_*`), geprüft
  gegen `signing/release-certificate-sha256.txt`. Niemals einen privaten Schlüssel
  committen.
