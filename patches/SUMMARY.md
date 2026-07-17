# VoxelMap – Code-Analyse: Zusammenfassung

Read-Only-Analyse vom 2026-07-17 (Branch `master`, Stand `776cb0a1`).
Alle Änderungsvorschläge liegen als einzeln anwendbare Unified-Diff-Patches in diesem
Verzeichnis (`git apply patches/NNN-*.patch` vom Repository-Root aus). Jeder Patch wurde
einzeln **und** kumulativ mit `git apply --check` verifiziert; die resultierenden
Java-Dateien wurden mit `javac` auf syntaktische Korrektheit geprüft (verbleibende
javac-Meldungen betreffen ausschließlich fehlende Minecraft-/Loader-Abhängigkeiten im
Prüf-Setup). Die neue Versionsvergleichslogik (008) und `CompressionUtils` (005) wurden
zusätzlich funktional getestet (Roundtrip + Fehlerpfad).

## Übersicht der Issues

| Nr. | Titel | Schweregrad | Kategorie | Datei |
|-----|-------|-------------|-----------|-------|
| 001 | Nicht thread-sichere Biome-Farb-Caches (HashMap/TreeMap) von mehreren Worker-Threads beschrieben | High | Race Condition | `util/BiomeRepository.java` |
| 002 | Copy-Paste-Fehler in `Map.getBlockHeight()`: FluidState des falschen Blocks ausgewertet | High | Bug | `Map.java` |
| 003 | `synchronized` auf reassigniertem Feld `lastRegionsArray` – kein gegenseitiger Ausschluss | High | Race Condition | `persistent/PersistentMap.java` |
| 004 | Fehlende Sichtbarkeit (JMM) für Thread-Flags `doFullRender`/`imageChanged`/`zoomChanged` | Medium | Race Condition | `Map.java` |
| 005 | Deflater/Inflater: nativer Speicher wird bei Exceptions nicht per `end()` freigegeben | Medium | Resource Leak | `util/CompressionUtils.java` |
| 006 | ZipFile-Handle-Leak in `loadCachedData()` bei korrupten Region-Cache-Dateien | Medium | Resource Leak | `persistent/CachedRegion.java` |
| 007 | Stream-Leak in `doSave()` bei IO-Fehlern während des Regionsspeicherns | Medium | Resource Leak | `persistent/CachedRegion.java` |
| 008 | Update-Checker vergleicht Versionen lexikografisch (`"1.16.10" < "1.16.9"`) | Medium | Bug | `util/ModrinthUpdateChecker.java` |
| 009 | NPE-Risiko: `CompressibleMapData.getBiome()` nutzt `Minecraft.level` statt eigener Welt-Referenz | Medium | Bug | `persistent/CompressibleMapData.java` |
| 010 | `CompressibleMapData.moveX()/moveZ()` rechnen im veralteten Interleaved-Layout (latente Datenkorruption) | Low | Bug | `persistent/CompressibleMapData.java` |
| 011 | String-Allokationen pro Tick für den „Ether“-Dimensionscheck | Low | Performance | `Map.java` |
| 012 | Reader-/Stream-Leaks in `BiomeRepository.loadBiomeColors()` bei IO-Fehlern | Low | Resource Leak | `util/BiomeRepository.java` |
| 013 | `NoSuchElementException` beim CTM-/Custom-Colors-Laden: `Optional.get()` ohne Präsenzprüfung | Medium | Bug | `ColorManager.java` |
| 014 | CTM-/Custom-Colors-Verarbeitung läuft vor Abschluss des asynchronen Terrain-Atlas-Readbacks (NPE, falsche/veraltete Farben) | Medium | Bug | `ColorManager.java` |

Pfade relativ zu `common/src/main/java/com/mamiyaotaru/voxelmap/`.

**Verteilung:** 3× High, 8× Medium, 3× Low — davon 6× Bug, 3× Race Condition, 4× Resource Leak, 1× Performance.

*Nachtrag 2026-07-18:* Issues 013 und 014 wurden anhand von Runtime-Logs identifiziert und sind
unabhängig von den Patches 001–012. 013: Stacktraces „error loading CTM No value present" /
„error loading custom color properties No value present" bei jedem Weltbeitritt mit aktivierter
„Connected Textures"-Option ohne OptiFine-Format-Pack. 014: nach 013 sichtbar gewordene
Folge-NPEs („terrainImage is null", „failed getting color: Lily Pad"), verursacht durch die
asynchrone GPU-Rückleseoperation des Block-Atlas — der Fehler existierte schon vorher, wurde
aber durch den früheren Abbruch aus 013 verdeckt. **014 setzt 013 voraus** (gleiche Datei,
Anwendung in Nummernreihenfolge).

## Hinweise zur Anwendung

- Die Patches sind unabhängig voneinander anwendbar; bei Anwendung **aller** Patches die
  Nummernreihenfolge einhalten (001 und 012 bzw. 002/004/011 und 009/010 berühren jeweils
  dieselben Dateien in disjunkten Bereichen — die Reihenfolge stellt saubere Kontext-Offsets sicher).
- 010 ändert derzeit toten Code (die Methoden werden für Regionen nie aufgerufen), beseitigt
  aber eine scharfe Falle, da die Basisklasse `AbstractMapData` die Operation als Teil des
  Vertrags definiert und `FullMapData` sie regulär nutzt.

## Beobachtungen ohne Patch (bewusst nicht geändert)

- `CachedRegion.loadAnvilData()` wartet mit `Thread.onSpinWait()`-Busy-Loops auf Futures —
  funktioniert, bindet aber unnötig einen Worker-Kern; ein `future.get()` mit Timeout wäre
  sauberer, ist aber eine größere Verhaltensänderung.
- `Map`-Scratch-Felder `surfaceBlockState`/`transparentBlockState` werden vom Berechnungs-
  Thread und (indirekt über `processChunk`) potenziell parallel genutzt; langfristig sollten
  sie durch lokale Variablen ersetzt werden (Patch 002 behebt nur die akute Fehlnutzung).
- `ColorManager.blockColors`-Arrays werden bei Resize unsynchronisiert getauscht; Verlust
  einzelner Cache-Einträge ist möglich, aber selbstheilend (Sentinel-Werte werden neu berechnet).
- `DynamicMutableTexture.setRGB()` läuft ohne das `bufferLock`, während der Render-Thread
  hochlädt — verursacht schlimmstenfalls einen Frame mit „zerrissenen“ Pixeln, keinen Crash.
- `CompressibleMapData.decompress()` schluckt `DataFormatException`; die Daten bleiben dann
  komprimiert und nachfolgende Index-Zugriffe können `ArrayIndexOutOfBoundsException` werfen
  (wird von den Aufrufern gefangen). Eine robustere Behandlung (Region als leer markieren)
  wäre wünschenswert, benötigt aber eine Design-Entscheidung.

## Gesamteinschätzung der Codebasis

Die Codebasis ist ein gewachsenes, funktional reichhaltiges Projekt mit klarer Modul-Trennung
(`common`/`fabric`/`forge`/`neoforge`/`paper`/`server-common`) und einer durchdachten
Threading-Architektur: Die Minimap rechnet auf einem dedizierten Daemon-Thread mit
Wait/Notify-Handshake und Watchdog, die Weltkarte auf einem begrenzten Thread-Pool mit
separatem Save-Pool und geordnetem Shutdown. Positiv fallen außerdem die konsequente
Wiederverwendung von `MutableBlockPos`-Objekten im Pixel-Hot-Path, das Kompressions-Schema
für inaktive Regionen und die Versionsmigration der Cache-Formate auf.

Die Schwächen liegen typisch für historisch gewachsenen Mod-Code in den Details der
Thread-Kommunikation: Es wird viel mit einfachen booleschen Flags zwischen Threads
signalisiert, teils ohne `volatile`/gemeinsames Lock (Issues 003, 004), und statische
Utility-Caches sind nicht durchgängig thread-sicher (001). Beim Ressourcen-Handling fehlt
an mehreren Stellen `try-with-resources` — auf Happy Paths korrekt geschlossen, auf
Fehlerpfaden leckend (005–007, 012); neuerer Code (`ComparisonCachedRegion`,
`CompressionUtils`-Streams) macht es bereits richtig, das Muster sollte auf die älteren
Stellen übertragen werden. Echte Logikfehler sind selten und eher Copy-Paste-Natur (002).
GL-/GPU-Ressourcen werden insgesamt diszipliniert verwaltet (Texture-Cache mit Größen- und
Resize-Invalidierung, `close()`-Ketten in den Textur-Klassen, Thread-Checks vor
Upload/Delete). Insgesamt: solide Substanz, empfehlenswert wären ein Durchgang mit
statischer Analyse (SpotBugs/ErrorProne mit Concurrency-Checks) und die schrittweise
Umstellung der Flag-Kommunikation auf explizite, dokumentierte Synchronisation.
