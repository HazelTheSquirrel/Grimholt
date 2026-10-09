# Technisches Code-Audit — Grimholt

**Prüfgegenstand:** Branch `test`, Ausgangs-Commit `9c4eec35296310dbd88c2f98780f735df957501e`  
**Zielversion:** Minecraft Java 26.4 Snapshot 3  
**Methode:** Quellcode, tatsächliche Aufrufpfade und Build-Konfiguration prüfen. Vorhandene Markdown-Dateien wurden nicht als Funktionsnachweis verwendet.  
**Wichtig:** Dieses Dokument unterscheidet zwischen vorhandenem Code, in den Startpfad integrierten Komponenten und nachgewiesener Vanilla-Kompatibilität.

## 1. Kurzurteil

Grimholt ist ein eigenständiges Java-Projekt mit eigenem Start-/Stop-Lifecycle, TCP-Listener, Connection-Handler, Packet-Framing, Konfigurations-Sync-Code, eigenem Gameplay-Modell und ersten Welt-/Entity-/Item-/Block-/Tick-/Persistenzkomponenten. In den ausgewerteten Runtime-Abhängigkeiten ist kein Minestom-, Bukkit-, Spigot-, Paper-, Folia-, Purpur- oder Velocity-Artefakt deklariert. Die Netzwerkschicht verwendet Java-Sockets und Virtual Threads; Netty ist im aktiven Transportpfad nicht zu sehen.

Das ist **noch keine vollständige Minecraft-Serverimplementierung**. Entscheidend fehlen bzw. sind nicht belegt: ein wiederholt erfolgreicher echter 26.4-Snapshot-3-Client-Handshake bis in Play, vollständige Paket- und Registry-Parität, autoritative Weltgenerierung und Chunk-Lifecycle, korrekte persistente Welt-/Spielerdaten sowie Vanilla-Differentialtests für reale Gameplay-Szenarien.

Der derzeitige CI-Stand ist zusätzlich blockiert: Im Workflow-Lauf [37862668556](https://github.com/HazelTheSquirrel/Grimholt/actions/runs/37862668556) schlägt der Gradle-Kotlin-DSL-Compile bei `java.util.zip.ZipFile` fehl. Daher wurden die nachfolgenden Workflow-Schritte nicht ausgeführt; dieser Lauf liefert keinen Nachweis für erfolgreiche Tests, Assembly oder JAR-Audit.

## 2. Reale Projektstruktur und Startpfad

Der Java-Quellbaum umfasst 143 Dateien unter `src/main/java/dev/grimholt` in folgenden funktionalen Gruppen:

- `api`, `server/api`: eigene API-Verträge, Server-/World-/Player-Implementierungen, Event-Bus, Service-Registry und Scheduler.
- `server/command`: eigener `GrimholtCommandDispatcher`; daneben existiert ein weiterer Vanilla-Dispatcher.
- `server/network`: Socket-Listener, Connection-State-Machine, Transport, Cipher, Online-Authentifizierung, Play- und Command-/Entity-/Chunk-Wire-Code sowie Konfigurations-Sync.
- `server/concurrency`, `server/runtime`, `server/vanilla/VanillaRegion*`: Region-Zuordnung, Tick-Ownership, bounded handoffs und Tick-Engine.
- `server/world`, `server/vanilla`: Chunk-/Section-/Block-/Item-/Entity-/Physics-/Fluid-/Redstone-/Recipe-/Combat-/World-/Protocol-/NBT-/Anvil-Modelle.
- `server/persistence`: atomare Dateioperationen.
- `server/plugin`, `metrics`, `security`, `ops`, `config`, `lifecycle`: Erweiterungsgrenze, Telemetrie-/Grenzwerte, Konfiguration und Lifecycle.

Der Einstieg ist `dev.grimholt.server.Grimholt.main`. Der Startpfad lädt Konfiguration, setzt Spielerlimits, startet den Vanilla-Kernel, registriert eine Overworld-ID und eine API-World, startet Plugins, bindet den Netzwerkserver, startet die Region-Tick-Engine und markiert den Lifecycle als gestartet. Bei Fehlern versucht der Code, Teilsysteme wieder herunterzufahren. `stop()` schließt Plugins, Tick-Engine, Netzwerk, Kernel und Scheduler.

**Integrationslücke:** Der ausgewertete Startpfad verwendet `world-directory` nicht zum Laden einer gespeicherten Welt und ruft keinen sichtbaren Worldgen-/Chunk-Load-/Save-Ablauf auf. Die API-World-Registrierung und die Kernel-World-Registrierung allein erzeugen noch keine spielbare, persistente Overworld.

## 3. Netzwerk und Protokoll

### Vorhanden

- `GrimholtNetworkServer`: bindet einen `ServerSocket`, akzeptiert Verbindungen über einen Virtual Thread und begrenzt Verbindungen vor Authentifizierung auf `maxPlayers + 64`.
- `GrimholtConnection`: besitzt Zustände für Handshake, Status, Login, Configuration, Play und Closed; behandelt Verbindungen in einem eigenen Virtual Thread. Vor Play gilt ein 30-Sekunden-Socket-Timeout.
- `GrimholtPacketTransport`: liest/schreibt geframte Pakete, hat eine konfigurierbare maximale Frame-Größe (im Connection-Konstruktor 2 MiB), unterstützt optionale Zlib-Kompression und AES-Transportverschlüsselung.
- `VanillaProtocol26_2`: implementiert VarInt- und Frame-Codierung, Kompressionsrahmen und Protokollzustände. Der Dateiname ist historisch, der Kommentar behauptet inzwischen 26.4 Snapshot 3.
- `GrimholtCipher` und `GrimholtOnlineAuthentication`: Kryptografie- und Online-Authentifizierungsbausteine.
- `VanillaConfigurationSync`: erzeugt Known-Packs-/Tag-bezogene Payloads und verarbeitet generierte Daten.
- `GrimholtPlayProtocol`, `GrimholtEntityWire`, `GrimholtCommandTreeWire`, `VanillaChunkWireCodec`: erste Payload-Encoder für Play, Entities, Command-Tree und Chunk/Light.
- `VanillaPacketCatalog` wird aus generierten Mojang-Daten geladen; der Listener verweigert den Start, wenn die benötigten generierten Daten nicht verfügbar sind.

### Befunde und Risiken

1. **Kein Netty:** Der sichtbare Transport ist `java.net.ServerSocket`/`Socket` mit Virtual Threads. Das ist eine valide eigene Architekturentscheidung, aber keine Netty-Implementierung. Es fehlen Nachweise für Skalierung, Backpressure und Paket-Ratenkontrolle unter Last.
2. **Versionsdrift:** Mehrere zentrale Typen heißen `VanillaProtocol26_2`, `VanillaSnapshot26_2`, `VanillaReferenceRunner26_2` und `VanillaPlayProtocol` mit 26.2-Kommentar, während der Build 26.4 Snapshot 3 als Ziel festlegt. In `VanillaConfigurationSync` wird die Known-Packs-Version über `VanillaSnapshot26_2.VERSION` bezogen. Das muss gegen die tatsächlich generierten 26.4-Berichte verifiziert und bereinigt werden.
3. **Paketparität unbewiesen:** Ein Packet-Catalog und einzelne Encoder belegen keine vollständige Zustands-/Richtungs-/ID-Parität. Der Masterpfad benötigt Fixture- und Clienttests für jedes Übergangspaket sowie unbekannte, verfrühte, doppelte und malformed Pakete.
4. **Frame-/Payload-Grenzen:** Der Frame hat eine Obergrenze und VarInt-Lesecode; separat zu auditieren sind negative/überlaufende Werte, maximale komprimierte und dekomprimierte Längen, Inflater-Endzustände, UTF-8-Striktheit, NBT-Rekursion und Collection-Limits. Eine maximale äußere Frame-Größe allein schützt nicht vor allen Kompressionsbomben.
5. **Connection-Fehlerdiagnose:** `run()` fängt `IOException | RuntimeException` und ignoriert sie im sichtbaren Pfad. Das verhindert noisy disconnects, erschwert aber Fehlersuche und Metriken. Fehlerursache und Disconnect-Kategorie sollten begrenzt und ohne sensible Daten protokolliert werden.
6. **Status/Login/Configuration/Play:** Die State-Machine und einzelne Codecs existieren. Ein durchgehender realer Clientablauf bis Play samt Join, Registry-Handshake, Chunk-Rendering, Movement, Reconnect und sauberem Disconnect ist durch den betrachteten Code/CI-Lauf nicht nachgewiesen.
7. **Encryption/Compression ordering:** Cipher- und Kompressionsmethoden sind vorhanden. Der Protokollzustandswechsel und die exakte Reihenfolge des Umschaltens müssen anhand der 26.4-Referenz durch Integrationstests abgesichert werden.

## 4. Welt, Chunks und Persistenz

### Vorhanden

- `VanillaChunk`, `VanillaChunkSection`, `BlockState`, `BlockPos`, `Aabb`: Datenmodelle für Chunk/Section/Blockposition und Geometrie.
- `VanillaBlockRegistry`, `VanillaBlockStateRegistry`, `VanillaGeneratedRegistryLoader`: Registry-/State-Daten können aus Mojang-generierten Berichten eingelesen werden.
- `VanillaOverworldGenerator`: eigener Generator-Ansatz.
- `VanillaRegionManager`, `VanillaRegionRuntime`, `RegionManager`, `OwnedRegion`, `TickOwnership`: erste Region-/Ownership-Abstraktionen.
- `BlockTickScheduler`, `VanillaTickEngine`, `GrimholtRegionTickEngine`: geplante Ticks und Engine-Hooks.
- `VanillaNbt`, `VanillaAnvilRegion`, `VanillaPersistenceModel`, `AtomicFileStore`: NBT-/Anvil-/Persistenzgrundlagen.

### Befunde und Risiken

1. Die Existenz eines Anvil-Region-Modells belegt nicht die komplette Vanilla-Dateistruktur. Zu beweisen sind Region-Header/Offsets, Kompressionsart, Chunk-NBT-Schema, DataVersion, Heightmaps, Paletten, Block Entities, Licht, POI, Entities, level.dat, Spielerdateien, Dimensionen und Crash-Recovery.
2. `VanillaNbt` bietet einen eigenen NBT-Codec; Sicherheitslimits, tiefe verschachtelte Strukturen, negative oder riesige Längen und korrekte Fehlerbehandlung müssen explizit geprüft werden.
3. Der Chunk-Wire-Codec nennt feste Konstanten (`SECTION_COUNT = 24`, `LIGHT_SECTION_COUNT = 26`). Diese Werte und die konkrete Payload-Struktur dürfen nicht ohne Abgleich mit den 26.4-Generierungsdaten als universell korrekt angenommen werden.
4. Das Vorhandensein von `VanillaOverworldGenerator` beweist keine Seed-Parität. Noise router, Density Functions, Aquifer, Carver, Biome, Features, Strukturen, Oberflächenregeln und Zufallsreihenfolge brauchen reproduzierbare Vergleichsfixtures.
5. Tick- und Region-Klassen sind erste Bausteine. Es fehlt Nachweis, dass jede mutable Welt-/Entity-Operation eindeutig einem Owner zugeordnet ist und keine Datenrennen bei Chunk-Load, Unload, Save, Teleport oder Shutdown auftreten.
6. Der aktuelle Serverstart lädt aus dem konfigurierten Weltverzeichnis im sichtbaren Startpfad keine existierende Welt und zeigt keinen vollständigen Save-Flush beim Herunterfahren.

## 5. Entity-, Spieler- und Gameplay-Systeme

### Vorhanden

- Eigene API-Spieler-/World-Modelle und `GrimholtEntityLifecycle`.
- `VanillaPlayerState`, `VanillaEntityState`, `VanillaEntityEngine`, `VanillaEntityRegistry`, `VanillaPathfinder`, `VanillaCombatEngine`, `VanillaPhysicsEngine`, `VanillaDamage`, `VanillaEffect`, `VanillaPortal`, `VanillaRaid`.
- `VanillaInventory`, `VanillaItemStack`, `VanillaItemRegistry`, `VanillaContainer`, `VanillaCraftingEngine`, `VanillaRecipeBook`.
- `VanillaFluidEngine`, `VanillaFluidSimulation`, `VanillaRedstoneEngine`, `VanillaBlockBehaviors`, `VanillaInteractionEngine`, `VanillaDataPack`, `VanillaScoreboard`, `VanillaWeather`, `VanillaWorldBorder`.

### Befunde und Risiken

- Diese Klassen zeigen Breite der Implementierungsansätze, aber nicht die vollständige Abdeckung aller Vanilla-Entitäten, Metadaten, Attribute, AI-Ziele, Pathfinding-Sonderfälle, Damage Types, Inventar-Menüs, Items/Components, Rezepte, Loot, Advancements, Datapacks und Gamerules.
- `GrimholtConnection` hält eigene Sitzungsfelder (UUID, Username, Position, Entity-ID), während der Kernel `VanillaPlayerState` separat führt. Die Synchronisierung dieser Zustände muss als eine einzige autoritative Session-/Player-Quelle definiert und getestet werden.
- Globale Entity-ID-Vergabe allein ist kein Entity-Tracking. Spawn/Remove, Sichtbarkeitsbereiche, Metadaten, relative Bewegungen, Teleports, Spielerinfo und Weltwechsel müssen zusammenpassen.
- Die Client-Movement-Pakete sind nur Eingaben; serverseitige Kollisionsauflösung, Validierung, Geschwindigkeit, Teleport-Bestätigung und Rückkorrektur müssen mit dem autoritativen Zustand verknüpft werden.
- Die vorhandenen Gameplay-Modelle dürfen erst als implementiert gelten, wenn Tests den realen Runtime-Pfad aus Paket → Validierung → Weltmutation → Synchronisierung abdecken.

## 6. Build, Datenbasis und Verpackung

- Gradle Kotlin DSL; Java-Toolchain-Version aus `gradle/libs.versions.toml` (CI setzt Java 25), Maven Central.
- Runtime-Abhängigkeiten sind auf SLF4J begrenzt; JUnit wird für Tests verwendet.
- `standaloneJar` erstellt ein ausführbares Fat-JAR und setzt `Main-Class: dev.grimholt.server.Grimholt`.
- Mojang-Referenz: `reference/minecraft/26.4/server.jar`, SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`; Gradle-Tasks prüfen die Prüfsumme, starten den Referenzserver als Smoke-Test und generieren Vanilla-Datenberichte.
- `dependencyAudit` prüft verbotene Gruppen; `forkIntegrityAudit` soll zusätzlich Imports, JAR-Inhalte, Einstiegspunkt und Manifest prüfen.
- Die Referenz-JAR wird von den Artefakt-Audit-Regeln ausgeschlossen und darf nicht als Grimholt-Laufzeit implementierung eingebettet werden.

**Aktueller Build-Defekt:** `forkIntegrityAudit` verwendet `java.util.zip.ZipFile` in Kotlin-DSL. Der Bezeichner `java` wird im Script-Kontext als Gradle-Java-Erweiterung aufgelöst; die Referenz ist deshalb nicht als Paketpfad auflösbar. Den Typ per Import (`java.util.zip.ZipFile`) einführen und als `ZipFile(...)` verwenden. Erst ein neuer grüner CI-Lauf zählt als Beleg.

**Mappings:** Im ausgewerteten Build ist kein Mojang-Mappings-/Remapping-Task erkennbar. Das ist für eine eigene, unabhängig benannte Implementierung nicht automatisch ein Defekt. Wenn Namen/Protokolldaten aus der Referenz extrahiert werden, müssen Generator, Datenformat, Versionierung und Herkunft nachvollziehbar bleiben. Keine fremde Serverimplementierung zur Laufzeit einbinden; Referenzdaten und generierte Protokoll-/Registry-Berichte strikt vom ausführbaren Artefakt trennen.

## 7. Priorisierte Risikomatrix

| Priorität | Risiko | Begründung | Nachweis für Abschluss |
|---|---|---|---|
| P0 | CI kompiliert nicht | Integritätsprüfung verhindert bereits den Testlauf | kompletter CI-Lauf grün und Artefakt hochgeladen |
| P0 | 26.2/26.4 Versionsdrift | Protokoll-/Snapshot-Klassen und Known-Packs-Version haben 26.2-Namen/Referenzen | alle IDs/Versionen aus 26.4-Berichten/Fixtures verifiziert |
| P0 | Echter Client-Login nicht bewiesen | Einzelcodecs decken keine End-to-End-Zustandsmaschine ab | wiederholbarer Clienttest bis Play |
| P0 | Keine nachgewiesene Welt-Persistenzintegration | Startpfad verwendet Weltverzeichnis nicht sichtbar zum Laden/Speichern | Neustarttest erhält Welt und Spielerzustand |
| P1 | Unvollständige/ungeprüfte Paketvalidierung | Fehlerhafte Pakete/Kompression können Prozess oder Speicher belasten | Fuzz-/Grenzwerttests und Rate-Limits |
| P1 | Chunk- und Registry-Parität | generierte Daten allein bedeuten keine korrekten Runtime-Payloads | Client rendert Chunks; Fixtures stimmen mit Referenz überein |
| P1 | Split-brain Player-State | Connection und Kernel haben separate Player-Zustände | ein autoritatives Modell und Race-/Lifecycle-Tests |
| P1 | Tick-/Region-Races | parallele Mutationen/Übergaben müssen deterministisch sein | Ownership assertions, stress/soak und Fehlerpfadtests |
| P2 | Vanilla-Mechanikbreite | viele Engines sind Modelle oder Teilimplementierungen | systematische Feature-Matrix plus differential tests |
| P2 | Skalierungsbehauptungen | Benchmark-Klassen beweisen keine echte Netzwerk-Last | reproduzierbarer Netzwerk-Benchmark für definierte Playerzahlen |

## 8. Audit-Fazit

Die belastbare Aussage lautet: **Grimholt hat eigenständige Infrastruktur und zahlreiche Gameplay-Prototypen, aber die vollständige eigenständige, vanilla-kompatible 26.4-Snapshot-3-Server-JAR ist noch nicht nachgewiesen.** Die nächsten Schritte müssen vom Build und Protokoll ausgehend bis zu einer echten spielbaren Welt sequenziert werden. Keine Klasse, ein grüner Unit-Test oder ein gestarteter Mojang-Referenzserver ersetzt den echten Client-/Gameplay-/Persistenznachweis.
