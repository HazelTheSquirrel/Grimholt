# Grimholt — Arbeitsplan: Vom Minestom-Unterbau zum spielbaren, multithreaded Minecraft-Fork

> Dieses Dokument ergänzt `docs/MASTER-WORKPLAN.md`, `docs/ARCHITECTURE.md`, `docs/COMPATIBILITY.md` und
> `docs/FORENSIC-PARITY-AUDIT.md`. Es ersetzt sie nicht, sondern bricht den riesigen Master-Plan auf eine
> **spielbarkeitsgetriebene** Reihenfolge herunter: Zuerst ein stabil erreichbarer, spielbarer Server mit
> funktionierendem Multithreading-Fundament — danach vanilla-Parität in der Tiefe.
>
> Geltungsbereich: Repository `HazelTheSquirrel/Grimholt`, Branch `main`, Java 25, Minestom 26.2 als
> Transport-/Instanz-Substrat (temporär, siehe `docs/ARCHITECTURE.md` Abschnitt "Fork migration").

---

## 0. Kurzaudit — wo das Projekt aktuell wirklich steht

**Fundament (solide, laut Repo-Stand):**
- Lifecycle-Statemaschine (`Lifecycle`, `LifecycleState`) sauber, idempotent, getestet.
- Minestom-Boundary (`MinestomAdapter`) kapselt Minestom-Typen, Public API (`dev.grimholt.api.*`) leakt keine
  Minestom-/SLF4J-Typen.
- Plugin-System (`PluginLoader`, `PluginBoundary`) mit Classloader-Isolation, Dependency-Resolution,
  Zyklen-Erkennung — funktional.
- Grimholt-eigenes Region-Ownership-Modell (`OwnedRegion`, `RegionManager`, `TickOwnership`,
  `VanillaRegionManager`, `VanillaServerKernel`) — **architektonisch der richtige Ansatz für Multithreading**,
  aber (siehe unten) **nicht an die lebende Minestom-Welt angebunden**.
- Viele deterministische Vanilla-"Kernel"-Bausteine existieren bereits als isolierte, getestete Einheiten:
  Physics, Fluids, Redstone, Crafting, Inventory, Combat, Entity-State, NBT, Anvil-Persistenz,
  Paket-/Protokoll-Katalog aus echten Mojang-Reports.

**Kritischste strukturelle Lücke (aus `FORENSIC-PARITY-AUDIT.md`, bestätigt):**
> Der aktuelle Datenfluss ist `Minecraft-Client → Minestom-Netzwerk/Runtime → Minestom-InstanceContainer →
> Grimholt-API + Vanilla-Kernel`. Der Vanilla-Kernel besitzt ein **eigenes**, paralleles Weltmodell
> (`VanillaWorldModel`/`VanillaChunk`), das nicht das tatsächlich an den Client gesendete
> Minestom-`Instance`-Chunk ist. Spielerbewegung kommt zwar im Kernel an (`PlayerMoveEvent` →
> `updatePlayerPosition`), aber Blockänderungen, Physik, Redstone, Fluids laufen aktuell **nicht** gegen die
> echte, sichtbare Welt. Das Multithreading-Fundament ist also gebaut, aber noch nicht "scharf geschaltet".

**Akuter Blocker (neu identifiziert, siehe Abschnitt 1):**
> Start-Reihenfolge-Bug: Der Netzwerk-Socket öffnet, bevor der Vanilla-Kernel läuft und die Welt registriert
> ist → Race Condition beim Login/Spawn.

---

## 1. SOFORTMASSNAHME (Priorität 0 — vor allem anderen) — Login-/Verbindungs-Bug

### 1.1 Root Cause

In `Grimholt.start()`:

```java
minestom.start(config, api, vanillaKernel);   // öffnet intern bereits den Socket!
vanillaKernel.start();                        // Kernel wird ERST DANACH "running"
var worldId = minestom.overworldId();
if (worldId != null) vanillaKernel.registerWorld(worldId); // Welt wird ERST DANACH registriert
```

`MinestomAdapter.start(...)` ruft am Ende selbst `initialized.start(config.socketAddress())` auf. Der
Server nimmt also bereits Verbindungen entgegen, **bevor** `vanillaKernel.start()` und `registerWorld(...)`
ausgeführt sind. Sobald ein Client schnell genug durch Login/Configuration/Spawn kommt (lokal/LAN praktisch
immer), feuert `PlayerSpawnEvent` bzw. `PlayerMoveEvent`. Beide Listener rufen auf dem Kernel Methoden auf,
die mit

```java
private void requireRunning() {
    if (!running) throw new IllegalStateException("Vanilla kernel is not running");
}
```

fehlschlagen können (oder mit `IllegalArgumentException("Unknown world: ...")`, solange die Welt noch nicht
registriert ist). Das erklärt exakt das gemeldete Symptom: intermittierend hängende/fehlschlagende Logins,
abhängig vom Timing zwischen Netzwerk-Thread und Haupt-Thread.

### 1.2 Fix — Reihenfolge umkehren: Kernel + Welt MÜSSEN stehen, bevor der Socket öffnet

**`VanillaServerKernel` zuerst starten, dann erst den Netzwerk-Layer:**

```java
// Grimholt.start(Path configPath)
config = configLoader.load(configPath);
Logging.startup(config.bindAddress(), config.port());
plugins.start();
plugins.discover(Path.of("plugins"));

vanillaKernel.start();                 // NEU: Kernel läuft, BEVOR irgendein Socket existiert
minestom.start(config, api, vanillaKernel); // registriert die Welt INTERN, bevor der Socket öffnet

scheduleVanillaTick();
plugins.loadAll();
plugins.enableAll();
lifecycle.started();
events.post(new dev.grimholt.server.event.ServerReadyEvent());
Logging.started();
```

**`MinestomAdapter.start(...)` anpassen — Welt registrieren, bevor `initialized.start(...)` den Socket
öffnet:**

```java
overworld = MinecraftServer.getInstanceManager().createInstanceContainer(DimensionType.OVERWORLD);
overworld.setGenerator(new GrimholtTerrainGenerator(0L));
overworld.enableAutoChunkLoad(true);
api.addWorld(overworld);

if (vanillaKernel != null) {
    vanillaKernel.registerWorld(overworld.getUuid()); // NEU: hier, nicht in Grimholt.start()
}

var events = MinecraftServer.getGlobalEventHandler();
events.addListener(AsyncPlayerConfigurationEvent.class, ...);
events.addListener(PlayerSpawnEvent.class, ...);
events.addListener(PlayerMoveEvent.class, ...);
events.addListener(PlayerDisconnectEvent.class, ...);

initialized.start(config.socketAddress()); // Socket öffnet GANZ ZULETZT
```

`Grimholt.java` ruft dann `minestom.overworldId()` weiterhin ab (z. B. für Logging), registriert die Welt
aber nicht mehr selbst doppelt.

### 1.3 Verifikations-Checkliste (vor dem nächsten Feature-Merge erledigen)

- [ ] Reihenfolge wie oben umsetzen (`vanillaKernel.start()` vor `minestom.start()`, `registerWorld()` vor
      `initialized.start(socketAddress)`).
- [ ] Integrationstest ergänzen: `VanillaServerKernel` muss `running()==true` und die Welt muss registriert
      sein, **bevor** `MinestomAdapter.isStarted()==true` zurückgibt. (Erweiterung von
      `MinestomAdapterIntegrationTest`.)
- [ ] Lasttest: 10–20 Clients (oder simulierte Verbindungen) verbinden sich *sofort* nach Serverstart
      (Skript/CI-Step), kein `IllegalStateException`/`IllegalArgumentException` im Log.
- [ ] Log-Zeile ergänzen: `"Vanilla kernel ready, world registered"` unmittelbar vor
      `"Starting Grimholt on <addr>:<port>"`-äquivalentem Socket-Open-Log, damit die Reihenfolge im
      laufenden Betrieb sichtbar/überprüfbar bleibt.
- [ ] Prüfen, ob `PlayerMoveEvent`-Listener (`updatePlayerPosition`) defensiv werden soll
      (z. B. still ignorieren statt werfen, falls `vanillaKernel` zwischen Shutdown-Beginn und letztem
      Move-Event kurzzeitig nicht mehr `running` ist) — das ist ein Robustheits-Fix *zusätzlich* zur
      Reihenfolge-Korrektur, kein Ersatz dafür.

### 1.4 Weitere Verdächtige, falls der Fix allein nicht reicht

Falls nach 1.2 weiterhin Login-Probleme auftreten, in dieser Reihenfolge prüfen:
1. **Online-Mode/Mojang-Session**: `config.onlineMode()` vs. tatsächlicher Client (Premium/Offline)
   — Mismatch führt zu Disconnect direkt nach Login, nicht zu einem Hänger.
2. **Firewall/Portbindung**: `bindAddress`/`port` aus `grimholt.properties` vs. tatsächlich erreichbarer
   Port (insbesondere bei `0.0.0.0` vs. öffentlicher IP).
3. **Erster-Chunk-Generierung blockiert Spawn**: `GrimholtTerrainGenerator` ist aktuell günstig
   (Hash-basiert, kein I/O), sollte keinen Timeout verursachen — aber bei sehr hoher `view-distance`
   gegen `dispatcher-threads` prüfen (`GrimholtConfig.dispatcherThreads()`).
4. **Plugin-Fehler beim Start**: Ein fehlschlagendes Plugin in `plugins.enableAll()` wirft eine Exception,
   die den gesamten Start abbricht (`Grimholt.start()` catch-Block) — das ist kein "hängender Login",
   sondern ein klarer Serverstart-Abbruch; im Log eindeutig von 1.1 unterscheidbar.

---

## 2. Ziel-Definition "Spielbar" (Minimal Viable Server)

Ein Vanilla-Client kann sich verbinden und:
1. zuverlässig einloggen (kein Timing-Fenster wie in Abschnitt 1),
2. in einer generierten Welt spawnen und sich bewegen (Kollision/Schwerkraft korrekt),
3. Blöcke abbauen/platzieren — sichtbar für alle verbundenen Spieler,
4. mit anderen Spielern denselben Weltzustand sehen (Chunk-Sync konsistent),
5. Chat/Commands benutzen,
6. sich trennen und erneut verbinden, ohne dass der Server in einen inkonsistenten Zustand gerät,
7. die Welt übersteht einen Neustart (Persistenz-Rundlauf).

Das ist die Ziellinie für **Phase A–C** unten. Erst danach lohnt sich Tiefenarbeit an Redstone, Mobs,
Villagern etc.

---

## 3. Architektur-Entscheidung: Multithreading-Modell

Ihr habt bereits das richtige Primitive gebaut: `OwnedRegion` (bounded Handoff-Queue, Single-Owner-Thread-
Zugriff, `TickOwnership` als Entwicklungszeit-Guard) + `RegionManager`/`VanillaRegionManager` (Region-Keys
über `CHUNKS_PER_REGION = 8`) + `VanillaServerKernel` als Fassade. Das ist konzeptionell sehr nah an
Folia-artigem Region-Ownership, ohne Minestom zu verändern (siehe `docs/COMPATIBILITY.md`,
"Multithreading-Kontrakt").

**Das fehlende Stück ist die Kopplung an die echte Minestom-Welt.** Zwei Optionen:

**Option A — Grimholt-Ownership als Gate vor Minestom-Mutationen (empfohlen für den nächsten Schritt):**
Jede Blockänderung/Interaktion, die aus einem Minestom-Event kommt (Player-Block-Break, Block-Place,
Redstone-Tick etc.), wird über `VanillaServerKernel.execute(worldId, chunkX, chunkZ, action)` geroutet.
Innerhalb von `action` wird **sowohl** der Grimholt-`VanillaChunk` **als auch** der echte
Minestom-`Instance`-Chunk mutiert (z. B. `instance.setBlock(...)`). Dadurch bleiben beide Modelle
synchron, Minestoms eigener Dispatcher bleibt Transport-Layer, Grimholt bestimmt aber die *Reihenfolge*
und *Exklusivität* der Mutation. Das ist der mit dem geringsten Risiko verbundene nächste Schritt und
passt zum bestehenden Code.

**Option B — Virtual Threads / Structured Concurrency pro Region (Java 25):**
Für rein asynchrone, nicht tick-kritische Arbeit (Chunk-Generierung, Speichern, Pathfinding-Berechnung)
Virtual Threads/`StructuredTaskScope` nutzen, Ergebnis aber immer über `OwnedRegion.execute(...)` an den
Owner zurückgeben (Snapshot/Compute/Apply-Pattern, wie in `docs/COMPATIBILITY.md` gefordert). Das ist
**Ergänzung**, kein Ersatz für Option A — niemals Tick-kritische Mutation direkt aus einem Virtual Thread.

**Entscheidung für diesen Plan:** Option A zuerst umsetzen (Phase D), Option B danach gezielt für I/O-lastige
Operationen (Chunk-Save, Pathfinding) einführen (Phase D.3).

---

## 4. Phasenplan

Jede Phase hat: Ziel, Voraussetzungen, konkrete Tasks, Exit-Kriterium ("das kannst du danach tun"), Tests.
Die Nummerierung ist unabhängig von `docs/MASTER-WORKPLAN.md`, verweist aber auf dessen Abschnitte, wo
sinnvoll.

### Phase A — Verbindung stabilisieren (BLOCKER, zuerst)
- **Ziel:** Login ist 100 % zuverlässig, keine Race Conditions beim Start.
- **Tasks:**
  - Fix aus Abschnitt 1.2 umsetzen.
  - `VanillaServerKernel.start()` idempotent/robust gegen doppelten Aufruf absichern (bereits vorhanden,
    testen).
  - Graceful-Kick statt harter Exception, falls doch einmal ein Timing-Fenster auftritt (Verteidigung in
    der Tiefe).
  - CI-Step: "connect immediately after start"-Test (siehe 1.3).
- **Exit-Kriterium:** 50 parallele Verbindungsversuche direkt nach Serverstart, 0 Fehler im Log,
  0 unerwartete Disconnects.
- **Tests:** Erweiterung `MinestomAdapterIntegrationTest`, neuer `VanillaServerKernelStartupRaceTest`.

### Phase B — Minimal spielbare Welt
- **Ziel:** Punkt 2–5 aus Abschnitt 2 erfüllt.
- **Tasks:**
  - `VanillaPhysicsEngine`/`VanillaBlockInteraction` an echte Minestom-Player-Events hängen
    (`PlayerBlockBreakEvent`, `PlayerBlockPlaceEvent`, Movement-Validation) statt nur gegen
    `VanillaWorldModel` zu laufen.
  - Block-Break/Place über `VanillaServerKernel.execute(...)` routen und **beide** Modelle synchron halten
    (siehe Abschnitt 3, Option A).
  - Respawn-Point/Gamemode/Health-Sync zwischen `VanillaPlayerState` und Minestom-`Player` herstellen
    (aktuell laufen Health/Hunger/XP nur im Kernel, nicht am Client sichtbar).
  - Chat-Pipeline minimal verdrahten (Adventure-Components, kein ChatColor — bereits Projektkonvention).
- **Exit-Kriterium:** Zwei Clients verbinden sich, sehen sich gegenseitig, einer baut einen Block ab, der
  andere sieht die Änderung sofort.
- **Tests:** Manuelle Client-Verifikation + Regressionstest auf `VanillaBlockInteraction`.

### Phase C — Persistenz-Rundlauf
- **Ziel:** Punkt 7 aus Abschnitt 2.
- **Tasks:**
  - `VanillaAnvilRegion` an echten Chunk-Save/-Load-Zyklus der Minestom-`Instance` hängen (aktuell nur
    isoliert getestet, siehe `VanillaParityInfrastructureTest`).
  - Spieler-Daten (`VanillaPersistenceModel.PlayerData`) beim Disconnect speichern, beim Reconnect laden.
  - Crash-sicheres Schreiben nutzen (`AtomicFileStore` ist bereits vorhanden und getestet — wiederverwenden
    statt neu bauen).
- **Exit-Kriterium:** Server neu starten → platzierte Blöcke und Spieler-Inventar sind noch da.
- **Tests:** Round-Trip-Integrationstest: Block setzen → Server stop/start → Block lesen.

### Phase D — Multithreading scharf schalten
- **Ziel:** Das Region-Ownership-Modell ist nicht mehr nur isoliert getestet, sondern bestimmt tatsächlich,
  welcher Thread welchen Chunk mutieren darf.
- **Tasks:**
  - D.1: Jede Spieler-Interaktion (Block, Redstone, Fluid-Tick) läuft durch
    `VanillaServerKernel.execute(worldId, chunkX, chunkZ, action)`.
  - D.2: `scheduleVanillaTick()` (bereits in `Grimholt.java` vorhanden) so erweitern, dass
    `VanillaRegionManager.tickAll()` tatsächlich die live gebundenen Regionen tickt, inkl. Redstone-
    (`VanillaRedstoneEngine`) und Fluid-Engine (`VanillaFluidSimulation`) — beide existieren bereits, sind
    aber nicht an den Minestom-Tick gekoppelt.
  - D.3: Virtual Threads/`StructuredTaskScope` (Java 25) für Chunk-Generierung und -Speicherung einführen,
    Ergebnis über `OwnedRegion.execute(...)` zurückreichen (Option B aus Abschnitt 3).
  - D.4: Automatische Hardware-Erkennung: `Runtime.getRuntime().availableProcessors()` für
    `dispatcherThreads`-Default existiert bereits in `GrimholtConfig.defaults()` — erweitern um
    RAM-basierte Chunk-Cache-Größe (`Runtime.getRuntime().maxMemory()`).
  - D.5: `maxHandoffs` (aktuell `1024` fest in `Grimholt.java`) dynamisch aus Hardware-Erkennung ableiten.
- **Exit-Kriterium:** Mehrere Regionen werden nachweislich auf unterschiedlichen Threads parallel geticked
  (Nachweis über `TickOwnership`-Assertions + Thread-Name-Logging), ohne Datenraces (kein
  `IllegalStateException` aus `TickOwnership.assertOwner()` im Normalbetrieb).
- **Tests:** Erweiterung `VanillaRegionManagerTest`, neuer Stresstest mit echten (simulierten)
  Spieler-Bewegungen über mehrere Regionen.

### Phase E — Inventory, Items, Crafting (live)
- Vorhandene Modelle (`VanillaInventory`, `VanillaContainer`, `VanillaCraftingEngine`,
  `VanillaParityRuntime.ContainerProtocol`) an echte Minestom-Inventory-Packets/Events anbinden.
- Exit-Kriterium: Spieler kann craften, Items droppen/aufheben, Container öffnen — sichtbar synchron
  zwischen Server-Modell und Client.

### Phase F — Combat, Entities, einfache Mobs
- `VanillaEntityEngine`, `VanillaCombatEngine`, `VanillaParityRuntime.MobAi`/`Combat` an echte
  Minestom-Entity-Spawns/-Ticks anbinden.
- Exit-Kriterium: Ein Zombie spawnt, pathfindet zum Spieler (`VanillaPathfinder` existiert bereits), greift
  an, Spieler nimmt Schaden sichtbar im Client.

### Phase G — Redstone/Fluids vollständig live
- `VanillaRedstoneEngine`/`VanillaFluidSimulation` sind funktional fertig (eigene Tests vorhanden) — hier
  nur noch die Live-Kopplung an Block-Update-Events aus Phase D vervollständigen (Observer-Pulse,
  Piston-Extend, Fluid-Spread sichtbar am Client).
- Exit-Kriterium: Eine einfache Redstone-Uhr und ein Wasserfall funktionieren wie in Vanilla 26.2.

### Phase H — Plugin-API vertiefen
- Erst **nachdem** die obigen Phasen stehen: API um Block-/Item-/World-Capabilities erweitern (aktuell laut
  `docs/API.md` nur Server/Player/World/Event/Scheduler/Service/Command-Grundgerüst).
- Exit-Kriterium: `examples/hello-plugin` plus ein zweites Beispielplugin, das tatsächlich einen Block
  setzt, funktionieren gegen die neue API.

### Phase I — Performance-Benchmark 500+ Spieler
- Erst sinnvoll, wenn Phase D–G stehen. `VanillaMultithreadingBenchmark`/`VanillaBenchmarkMain` sind bereits
  vorhanden, messen aber aktuell nur das isolierte Weltmodell, nicht den echten Netzwerk-Pfad — Benchmark
  um echte (simulierte) Verbindungen erweitern (`docs/PERFORMANCE.md`-Matrix: 50/100/250/500/750/1000).
- Exit-Kriterium: Dokumentierte Zahlen für CPU/RAM/Weltgröße/View-Distance/Entity-Dichte bei jeder
  Laststufe — keine Garantie, aber belastbare Evidenz.

### Phase J — Production Hardening / Release Candidate
- Deckt sich mit `docs/MASTER-WORKPLAN.md` Phase 7–8: Crash-Diagnose, Backup/Recovery, Security-Review,
  Doku-Vervollständigung.

---

## 5. Teststrategie (projektweit)

- **Unit-Tests**: Jede bestehende isolierte Kernel-Klasse behält ihre Tests (bereits hohe Abdeckung laut
  Repo-Stand).
- **Integrationstests**: Für jede Phase B–G mindestens ein Test, der den echten Minestom-Event-Pfad
  durchläuft (nicht nur das `VanillaWorldModel` isoliert).
- **Race-Condition-Tests**: Für jede neue Kopplung Kernel ↔ Minestom (Phase D) explizit auf
  `TickOwnership`-Verletzungen testen (siehe `TickOwnershipTest` als Vorlage).
- **CI-Gate**: `gradle --no-daemon clean test` + `dependencyAudit` + `vanillaReferenceSmoke26_2` müssen bei
  jedem Merge grün sein (bereits in `.github/workflows/ci.yml` vorhanden) — für Phase A zusätzlich den
  neuen Startup-Race-Test aufnehmen.
- **Manuelle Client-Verifikation**: Ab Phase B nach jedem Feature mit einem echten Vanilla-26.2-Client
  gegenprüfen — Protokoll-/Paket-Korrektheit allein ist keine Spielbarkeits-Evidenz (siehe
  `docs/COMPATIBILITY.md`, "Non-negotiable rule").

---

## 6. Diagnose-Leitfaden für künftige Verbindungsprobleme

Falls erneut "Verbindung hängt/schlägt fehl" auftritt, in dieser Reihenfolge prüfen:

1. **Logs auf `IllegalStateException`/`IllegalArgumentException` aus `VanillaServerKernel` durchsuchen** —
   das ist das erste Anzeichen für eine Reihenfolge-/Race-Condition wie in Abschnitt 1.
2. **Ist der Fehler reproduzierbar bei jedem Connect oder nur manchmal?** Immer = Konfigurations-/Auth-
   Problem (Abschnitt 1.4, Punkt 1–2). Manchmal = Timing-Problem (Abschnitt 1.4, Punkt 3, oder neue
   Race Condition nach Phase D).
3. **Serverstart-Log auf Reihenfolge prüfen**: "Vanilla kernel ready" muss vor dem Socket-Open-Log stehen
   (siehe Log-Ergänzung in 1.3).
4. **Bei Phase D+**: `TickOwnership.assertOwner()`-Verletzungen im Log = jemand mutiert Weltzustand
   außerhalb seines Owner-Threads — sofortiger Stopp, das ist strukturell derselbe Fehlertyp wie der
   ursprüngliche Login-Bug, nur in einer anderen Subsystem-Kopplung.

---

## 7. Nächste konkrete Schritte (diese Woche)

1. [ ] Fix aus Abschnitt 1.2 umsetzen und mergen (Phase A).
2. [ ] Startup-Race-Test schreiben und in CI aufnehmen.
3. [ ] Mit echtem Vanilla-26.2-Client gegen den gefixten Server verbinden, 10× hintereinander, Log
   prüfen.
4. [ ] Erst danach mit Phase B (Block-Break/Place live) beginnen.

---

*Letzte Aktualisierung: wird bei jeder abgeschlossenen Phase in diesem Dokument fortgeschrieben
(Abschnitt "Change Log" nach Vorbild von `docs/MASTER-WORKPLAN.md` Abschnitt 14 ergänzen, sobald Phase A
abgeschlossen ist).*
