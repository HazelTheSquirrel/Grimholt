# Grimholt — priorisierte Implementierungs-Roadmap

**Repository:** `HazelTheSquirrel/Grimholt` · **Branch:** `test`  
**Ziel:** eigenständige, ausführbare Grimholt-JAR für Minecraft Java 26.4 Snapshot 3  
**Referenz:** ausschließlich die im Repository gepinnte Mojang-Referenz `reference/minecraft/26.4/server.jar` (SHA-1 `2d89c95c030e635387448f332961074ce1adbb4b`) als Daten-/Vergleichsquelle.

Diese Roadmap ist nach Abhängigkeiten geordnet. Ein Schritt wird nur abgeschlossen, wenn Tests und der beschriebene Abnahmenachweis existieren. Die Referenz-JAR darf nicht als Grimholt-Implementierung oder Laufzeitabhängigkeit ausgeliefert werden.

## Phase 0 — Build wieder verlässlich machen (P0)

1. **Kotlin-DSL-Fehler korrigieren.** `ZipFile` importieren und den unqualifizierten Typ verwenden; Integritätsprüfung lokal/CI ausführen.
2. **Saubere CI-Kette bestätigen:** checksum → clean test → Mojang-Referenz-Smoke → assemble → forkIntegrityAudit → Artefakt-Upload.
3. **Standalone-JAR starten.** Ein eigener Grimholt-Artefakt-Smoke-Test startet die gebaute JAR mit temporärer Konfiguration, prüft die Ready-Meldung und beendet sie kontrolliert. Referenzserver-Smoke ist kein Ersatz.
4. **Reproduzierbare Toolchain:** Java 25, Gradle 9.8.0, dokumentierte Build-Eingaben; keine lokalen Dateien als versteckte Voraussetzung.
5. **Unabhängigkeit absichern:** Runtime-Abhängigkeitsgraph, Quellimports und tatsächliche JAR-Einträge auf verbotene Serverframeworks und versehentlich mitgelieferte Mojang-JAR prüfen.

**Abnahme:** CI grün auf genau dem geprüften Commit; Grimholt-JAR ist vorhanden, startet und beendet sich kontrolliert.

## Phase 1 — Version und Referenzdaten einfrieren (P0)

1. Protokollversion, DataVersion, Data Pack-/Resource Pack-Version und Known-Packs-Version ausschließlich aus den 26.4-Snapshot-3-Berichten/Prüfungen bestätigen.
2. 26.2-benannte Klassen aufteilen oder neutral umbenennen, wenn sie tatsächlich 26.4-Code enthalten. Keine Versionskonstante darf unbeabsichtigt von `VanillaSnapshot26_2` kommen.
3. Daten-Generator-Ausgaben deterministisch versionieren: Registries, Blocks/States, Items/Components, Entities, Tags, Biomes, Dimensionen, Damage Types, Sounds, Rezepte und Paketberichte.
4. Generatorfehler sichtbar machen: fehlende Reports, leere Dateien, doppelte IDs, ungültige Referenzen oder nicht unterstützte Daten müssen den Build abbrechen.
5. Prüfen, welche generierten Reports tatsächlich vom Runtime-Code gelesen werden. Nicht konsumierte Berichte gelten nicht als implementierte Funktion.

**Abnahme:** deterministische Datenreports; Prüfungen belegen Version und IDs; Runtime-Artefakt enthält keine Referenz-JAR.

## Phase 2 — Netzwerk und Protokoll vollständig absichern (P0)

### 2.1 Framing und Decoder

1. Einheitliche Paket-Reader/-Writer pro Zustand und Richtung.
2. VarInt-Overflow, negative/überlange Längen, abgeschnittene Frames, nachlaufende Bytes und unbekannte IDs testen.
3. Strikte UTF-8-/Identifier-/String-Limits; keine stillschweigende Ersetzung ungültiger Eingaben.
4. Kompressionsschwelle, unkomprimierte Länge, Deflate-/Inflater-Endzustand und maximale dekomprimierte Bytes begrenzen.
5. NBT-Decoder mit maximaler Tiefe, Tag-/Array-/Listengröße und Gesamtbytes.
6. Pro Verbindung Paket-/Byte-Raten, Queue-Limits, Backpressure, Idle-Timeouts und sauberes Close implementieren.
7. Fuzzing gegen jeden Decoder und Property-based Round-Trip-Tests.

### 2.2 Handshake und Status

1. Handshake-Payload mit Protokollversion, Host, Port und Next-State exakt validieren.
2. Status JSON in Form und Pflichtfeldern gegen die Zielversion prüfen.
3. Ping/Pong-Payload exakt zurückgeben; ungültige Zustände und Timeouts schließen.
4. Protokollversionen außerhalb des unterstützten Ziels kontrolliert ablehnen.

### 2.3 Login, Authentifizierung, Verschlüsselung

1. Login Start, Username-/UUID-Regeln, doppelte Namen/Sessions und maximale Spielerzahl validieren.
2. Online-Mode Session-Server-Antworten, Timeouts, Fehler, Properties und UUID normalisieren und testen.
3. Encryption Request/Response, Verify Token, AES-CFB8-Übergang und Umschaltzeitpunkt durch Integrationstest absichern.
4. Compression-Setzen und erste komprimierte/uncompressed Pakete an der exakten Grenze testen.
5. Offline-Mode-UUID-Regeln und Sicherheitsfolgen dokumentieren; Disconnect-Gründe für jeden Login-Zustand testen.

### 2.4 Configuration

1. Alle erforderlichen 26.4-Paket-IDs, Felder, Registry-Reihenfolge und NBT-Formate aus generierten Referenzdaten ableiten.
2. Known Packs korrekt aushandeln; Version nicht aus Legacy-Konstanten beziehen.
3. Registry Data, Feature Flags, Tags, Cookies, Resource Packs und weitere für die Zielversion erforderliche Konfiguration synchronisieren.
4. Client-Acknowledgements, Configuration Finish, frühe/doppelte Pakete und Disconnect während Configuration testen.

### 2.5 Play und Lifecycle

1. Join-Game/Player-Info/Abilities/Spawn-/Teleport-/Keepalive-Sequenz gegen den echten Client prüfen.
2. Client bestätigt Teleports; Movement wird validiert und in das autoritative Spielermodell geschrieben.
3. Chunk-/Light-Payloads rendern; Entities und Metadaten erscheinen korrekt.
4. Reconnect, Timeout, Disconnect, Server-Shutdown und fehlerhafte Clients end-to-end testen.

**Abnahme:** echter Vanilla-Client 26.4 Snapshot 3 verbindet sich wiederholt, erreicht Play, sieht die Welt, bewegt sich, trennt sich und verbindet sich erneut. Client-Build, Logs und Testschritte als Artefakt sichern.

## Phase 3 — Autoritativer Welt- und Spieler-Kern (P0)

1. Genau eine autoritative Session-/Player-Repräsentation definieren. Duplikate zwischen Connection-Feldern, API-Spieler und `VanillaPlayerState` synchronisieren oder beseitigen.
2. Welt-/Dimension-Registry und World-Lifecycle etablieren.
3. Chunk-State-Machine: absent → loading → loaded → saving → unloaded; Fehler, Cancellation und Shutdown definieren.
4. Chunk Sections, Paletten, Block Entities, Heightmaps, Licht, Dirty Flags und Lade-/Save-Queues integrieren.
5. Region-Owner pro Welt/Chunk/Entity festlegen; Cross-Owner-Mutationen nur über begrenzte, geordnete Handoffs.
6. Network-Thread validiert Pakete und erzeugt Intents; Weltmutation findet auf dem zuständigen Owner statt.
7. Konfiguriertes `world-directory` tatsächlich verwenden; existierende Welt laden und beim Stop synchronisiert flushen.
8. Race-/Stress-Tests für gleichzeitigen Join/Quit, Chunk Load/Unload, Region-Handoff, Save, Teleport und Shutdown.

**Abnahme:** Welt- und Spielerzustand haben eine eindeutige Autorität; Ownership-Invarianten und Lifecycle-Stresstests bestehen.

## Phase 4 — Weltdateien, NBT und Anvil (P0)

1. Vollständigen NBT-Reader/Writer mit Limits, unbekannten Tags, korrekten UTF-Formaten und fehlerhaften Eingaben absichern.
2. Region-Datei-Header, Chunk-Offsets/Längen, Kompression, Überschreiben, Fragmentierung und beschädigte Sektoren unterstützen.
3. Chunk-NBT nach Zielversion abbilden: DataVersion, Sections/Paletten, Block Entities, Biome, Heightmaps, Light, Ticks und Status.
4. `level.dat`, Dimension-/World-Info, Playerdata, Entities und POI entsprechend der Zielversion implementieren.
5. Atomic write, fsync/flush, Backup-/Recovery-Strategie, Save-Queue und Crash-Injection testen.
6. Vorhandene Mojang-Testwelten laden/speichern und mit einem Vanilla-Server erneut öffnen lassen, sofern der Testaufbau dies zulässt.
7. Round-Trip- und Restart-Differentialtests für Blöcke, Spielerposition, Inventar, Entities und Weltdaten.

**Abnahme:** Grimholt kann eine Welt anlegen, ändern, sauber speichern, nach Neustart laden und die Daten verlustfrei erhalten.

## Phase 5 — Chunk-Generierung und Clientdarstellung (P0/P1)

1. Chunk-Anforderung, Priorisierung, Spieler-Sichtweite, Abbruch bei Disconnect und begrenzte parallele Generation implementieren.
2. Weltgen-Reihenfolge und RNG deterministisch aus der Zielreferenz ableiten: Noise router, Density Functions, Aquifer, Carver, Biome, Features, Strukturen und Surface Rules.
3. Höhenkarten, Beleuchtung, Nachbargrenzen und Chunk-Status erzeugen.
4. Chunk-/Light-Pakete gegen 26.4-Client und generierte Referenzdaten validieren; feste Sections-/Light-Konstanten verifizieren.
5. Streaming, Chunk unload, Teleport, Dimension-Wechsel und viele Spieler auf angrenzenden Chunks testen.
6. Seed-Tests mit festgelegten Seeds und Koordinaten; Block-/Biome-/Heightmap-Differenzen berichten.

**Abnahme:** Zielclient rendert generierte Chunks und Licht stabil; reproduzierbare Seedszenarien zeigen dokumentierte Übereinstimmung mit der Referenz.

## Phase 6 — Spielerbewegung, Blockinteraktion und Inventar (P1)

1. AABB-Kollision, Gravitation, Reibung, Sprung, Sprint, Sneak, Schwimmen, Klettern, Fall-/Flüssigkeitsschaden und Bewegungskorrektur.
2. Serverseitige Movement-Validierung: Speed-/Position-Sanity, Teleport-Confirm, on-ground Plausibilität und Dimension-Checks.
3. Blockbreak: Reichweite, Face, Werkzeug/Harvest, Fortschritt, Abbruch, Drops und Synchronisierung.
4. Placement: Replaceability, Support, Orientierung, Kollision, Nachbarupdates und Block Entities.
5. Items/Components/Stacks, Pickup/Drop, Durability und Inventartransaktionen.
6. Container: Slot-/Cursor-Semantik, Shift-Click, Drag, Number-Key, Drop, Resync und Menü-Lifecycle.
7. Persistenz für Position, Gamemode, Health/Hunger, Effekte, Inventar und Respawn.
8. Echte Client-Szenarien mit Block setzen/abbauen, Item einsammeln, Inventar öffnen, Tod/Respawn und Neustart.

**Abnahme:** geschlossener Survival-Kern von Bewegung bis persistenter Block-/Inventaränderung.

## Phase 7 — Blocks, Tick-System, Fluide und Redstone (P1)

1. Vollständige Block-/State-/Shape-Registry und korrekte Default-/Property-Werte.
2. Scheduled Ticks und Random Ticks mit Reihenfolge, Persistenz und Owner-Lokalität.
3. Neighbor Updates mit Schutz vor Rekursion/Update-Stürmen.
4. Wasser/Lava, Flussstufen, Quellen, Mischungen, Fallflüssigkeit und Block-/Entity-Kollision.
5. Feuer, Pflanzen, Crop Growth, Block Entities und ihre Tick-/Persistenzzyklen.
6. Redstone-Power, Dust, Torch, Repeater, Comparator, Lever/Button/Plate, Observer, Piston/Sticky Piston, Hopper, Dispenser/Dropper und Quasi-Connectivity.
7. Chunk-Grenzen, Tick-Reihenfolge und adversariale Clock-/Farm-/Piston-Szenarien vergleichen.

**Abnahme:** Feature-Matrix pro Blockfamilie, deterministische Tests und Differentialergebnisse.

## Phase 8 — Items, Rezepte und Datapacks (P1)

1. Vollständige Items/Components, Attribute, Verzauberungen, Food, Potions, Equipment und Durability.
2. Rezepte, Recipe Book, Loot Tables, Conditions/Functions, XP und Drops.
3. Alle Container-/Menu-Typen samt Serverbound-Click-Semantik.
4. Furnace-Varianten, Brewing, Enchanting, Anvil, Smithing, Stonecutter, Grindstone, Loom, Cartography, Beacon, Hopper und Storage.
5. Datapack parsing/reload, Tags, Predicates, Functions, Schedules, Advancements, Statistics, Scoreboards, Teams, Bossbars und Gamerules.
6. Fehlerisolation beim Laden/Reload und deterministische Referenz-Fixtures.

**Abnahme:** getestete Rezepte/Loot-/Datapack-Fixtures und echte Containerinteraktionen stimmen mit dem Referenzverhalten überein.

## Phase 9 — Entities, AI, Schaden und Kampf (P1)

1. Vollständige Entity-Registry, Spawn-/Remove-/Tracking-Bereiche und Metadaten.
2. Attribute, Kollisionskörper, Movement, Passenger/Vehicle und Entity-Persistenz.
3. Damage Sources/Types, Rüstung, Toughness, Enchantment-Modifier, Invulnerability Frames und Knockback.
4. Nahkampf, Schilde, Projektile, Pfeile/Tridents und Zielversions-Sonderfälle.
5. Mob-Goals, Target Selection, Navigation/Pathfinding, Despawn, Breeding, Taming, Leash, Loot und XP.
6. Passive, neutrale, hostile, aquatische, fliegende, Utility- und Boss-Familien stufenweise integrieren.
7. Entity-Tracking/Metadata gegen echte Clientpakete testen und Entityzustand nach Restart prüfen.

**Abnahme:** priorisierte Entityfamilien haben vollständige Lifecycle-/Gameplay-/Wire-/Persistence-Tests; nicht implementierte Familien sind transparent ausgewiesen.

## Phase 10 — Parallelität, Sicherheit und Stabilität (P1)

1. Mutable-State-Ownership assertions im Debug-/Testmodus.
2. Bounded queues, Queue-Auslastung, Reject-/Timeout-Verhalten und saubere Backpressure.
3. Race-Tests für Region-Handoffs, Player-Transfer, Chunk Save/Unload und Scheduler-Shutdown.
4. Packet-Fuzzing, Verbindungsfluten, langsame Clients, ungültige Authentifizierung, Compression Bombs und NBT-Limits.
5. Telemetrie für MSPT/TPS, Queue-Latenz, GC, Heap, Netzwerkbytes, Chunk-Generation, Entity-Ticks und Disconnect-Gründe.
6. Soak-Test, Crash-Injection, Thread-/FD-Leaks und Graceful Shutdown.
7. Keine Kapazitätsangabe von 500–1000 Spielern ohne dokumentierten echten Netzwerk-Benchmark und Hardware-/World-/View-Distance-Profil.

**Abnahme:** wiederholbare Last-/Soak-Tests mit veröffentlichten Bedingungen, Grenzen und Fehlerquote.

## Phase 11 — Release, Artefakt und Wartung (P0 bis Release)

1. `clean test`, Protokolltests, Persistence-/Worldgen-Differentialtests, Referenz-Smoke, `assemble`, Dependency Audit, `forkIntegrityAudit` und eigener Grimholt-JAR-Smoke in CI.
2. JAR-Inhalt prüfen: nur Grimholt plus notwendige Laufzeitbibliotheken; keine Mojang-Referenz-JAR, kein Serverframework.
3. Manifest, Java-Version, Version/Commit, Zielprotokoll und Prüfsumme ausgeben.
4. Build-Artefakt aus CI hochladen und Installations-/Start-/Konfigurations-/Backup-/Upgradepfad testen.
5. Release nur mit Feature-Matrix, bekannten Lücken und Clienttest-Belegen erstellen.

**Release-Gate:** Der konkrete Release-Commit ist reproduzierbar baubar; JAR startet; echter 26.4-Client kann den dokumentierten Umfang spielen; Welt-/Spielerdaten überstehen Neustart; alle verpflichtenden CI-Gates sind grün.

## Reihenfolge in einem Satz

**Build reparieren → 26.4-Daten einfrieren → Netzwerk bis Play beweisen → autoritative Welt-/Spielerzustände → Anvil/NBT → Worldgen/Chunks → Survival-Kern → Redstone/Datapacks → Entities/AI → Last-/Sicherheitstests → Release.**

Diese Roadmap beansprucht keine sofortige Vollständigkeit: Minecraft-Vanilla-Parität ist eine große Implementierungs- und Testaufgabe. Jeder Meilenstein muss durch ausführbare Nachweise abgeschlossen werden, nicht durch Dokumentation oder Klassenanzahl.
