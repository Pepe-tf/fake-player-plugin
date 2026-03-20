# AGENTS.md — Guide for AI coding agents

Purpose: help automated agents quickly understand, modify, and verify the FakePlayerPlugin codebase.

1) Quick repo map
- `src/main/java/me/bill/fakePlayerPlugin/` — main code and entry points
- `src/main/resources/` — default `config.yml`, `plugin.yml`, `bot-names.yml`, `bot-messages.yml`
- `pom.xml` — build + jar metadata (`target/fpp-*.jar` produced)
- `wiki/` — feature docs (Commands.md, Configuration.md, Getting-Started.md)
- `src/main/java/me/bill/fakePlayerPlugin/util/` — helper subsystems and utilities (e.g. `FppMetrics`, `FppLogger`, `FppPlaceholderExpansion`, `TabListManager`, `BackupManager`, `UpdateChecker`, `ConfigValidator`, `DataMigrator`).
- `src/main/java/me/bill/fakePlayerPlugin/fakeplayer/` — extra bot subsystems (e.g. `ChunkLoader`, `SkinRepository`, `SkinFetcher`, `NmsHelper`, `BotSwapAI`, `BotHeadAI`, `BotChatAI`, `BotBroadcast`, `PacketHelper`).

2) Where to start (entry points)
- `FakePlayerPlugin.java` — plugin lifecycle (`onEnable` / `onDisable`) and orchestration.
- `command/CommandManager.java` and command classes (e.g. `SpawnCommand.java`, `ReloadCommand.java`) — command wiring and tab completion.
- `config/Config.java`, `util/ConfigMigrator.java` — configuration pattern and automatic migration.
- `fakeplayer/FakePlayerManager.java`, `fakeplayer/BotPersistence.java` — bot lifecycle, persistence and restore.
- `config/BotNameConfig.java`, `config/BotMessageConfig.java` — bot name & message pools which are initialised in `onEnable`.
- `fakeplayer/SkinRepository.java` / `fakeplayer/SkinFetcher.java` — skin-loading subsystem (called during startup).
- `util/TabListManager.java`, `util/UpdateChecker.java` — tab header/footer management and async update checker initialized during startup.

3) Key patterns & concrete examples (copy-ready pointers)
- Migration runs before config init: `ConfigMigrator.migrateIfNeeded(this)` in `onEnable` (see `FakePlayerPlugin.java`).
- Config usage: call `Config.init(this)` then use accessors like `Config.fakeChatEnabled()` in code.
- Command registration: plugin registers commands via `commandManager.register(new SpawnCommand(...))` and then `getCommand("fpp").setExecutor(commandManager)` (see `onEnable`).
- Listener registration: `getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, fakePlayerManager), this)` — listeners receive plugin + manager references.
- Persistence lifecycle: restore on enable via `botPersistence.purgeOrphanedBodiesAndRestore(fakePlayerManager)` and save on disable via `botPersistence.save(fakePlayerManager.getActivePlayers())`.
- Metrics & soft-deps: metrics are initialized with `FppMetrics` when `Config.metricsEnabled()`; PlaceholderAPI expansion registered conditionally in `onEnable`.
- Compatibility checks: `FakePlayerPlugin.onEnable()` now performs server compatibility detection (Paper server detection + Minecraft minimum version 1.21.9 + Mannequin class presence). When unsupported the plugin sets `compatibilityRestricted = true` and disables chunk loading, physical bodies and some listeners / AI (`BotHeadAI`, Mannequin-dependent listeners) — look for `compatibilityRestricted` and `compatibilityWarningMessage` in `FakePlayerPlugin.java`.
- Bot pools: `BotNameConfig.init(this)` and `BotMessageConfig.init(this)` are called during startup — the plugin loads `bot-names.yml` and `bot-messages.yml` early.
- Metrics / FastStats packaging: `FppMetrics` is initialized before the startup banner to reflect metrics status. FastStats jars are bundled under `src/main/resources/faststats/` (the POM treats them as binary resources, not shaded). Note: building requires JDK 21 (see `pom.xml` <java.version>21).</p>

4) Common tasks & minimal steps (how to implement typical changes)
- Add a new command:
  - Create `src/main/java/me/bill/fakePlayerPlugin/command/MyCommand.java` implementing the command interface used by `CommandManager`.
  - Register it in `FakePlayerPlugin.onEnable()` with `commandManager.register(new MyCommand(...))`.
  - Ensure `plugin.yml` contains the `fpp` command or sub-command docs (command parsing done by `CommandManager`).
- Add a config option:
  - Add default to `src/main/resources/config.yml`.
  - If needed, update `Config` accessor and `ConfigMigrator` to preserve old configs.
  - New options should be validated in `util/ConfigValidator.java` if they can be misconfigured.

    - If changing skin behaviour or adding skins: ensure `SkinRepository`/`SkinFetcher` are respected — add skin files to `src/main/resources/skins/` for defaults or `plugins/FakePlayerPlugin/skins/` at runtime and call `/fpp reload` to refresh. The startup code also drops a `skins/README.txt` inside the plugin data folder on first run.

5) Build / run / verify (Windows PowerShell)
Build (skip tests):
```powershell
mvn -DskipTests clean package
```
Note: the project requires JDK 21 to compile (see `pom.xml` <java.version>21). FastStats jars are bundled as binary resources in `src/main/resources/faststats/` (not shaded). Build command remains:
```powershell
mvn -DskipTests clean package
```
Copy built jar to your Paper server `plugins` folder (example):
```powershell
Copy-Item -Path ".\target\fpp-*.jar" -Destination "C:\path\to\paper\plugins\" -Force
```
Start server (attach debugger):
```powershell
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms512M -Xmx2G -jar .\paper-1.21.11.jar nogui
```
Tail logs:
```powershell
Get-Content .\logs\latest.log -Wait
```
In-server quick checks: `/fpp list`, `/fpp spawn`, `/fpp reload` and watch console for startup banner messages printed by `FppLogger`.

6) Debugging tips
- To reproduce startup ordering bugs, inspect `onEnable()` in `FakePlayerPlugin.java` — migration → config → skins → database → subsystems → commands → listeners → persistence.
- To debug config migrations, read `util/ConfigMigrator.java` and check backups under `plugins/FakePlayerPlugin/backups/` created by `BackupManager`.
- For packet-level issues (spawn / entity problems) inspect `fakeplayer/PacketHelper.java` and `fakeplayer/FakePlayerManager.java`.

- Startup compatibility checks (Paper / MC version / Mannequin) can set `compatibilityRestricted` and silently skip chunk-loading & some listeners — inspect `FakePlayerPlugin.onEnable()` for `compatibilityRestricted` usage and the generated `compatibilityWarningMessage`.
- Tab list header/footer and update checker: `TabListManager.start()` and `UpdateChecker.check(this)` are run during `onEnable()` — useful when diagnosing missing tab headers or unexpected update-checker output.

7) Integration & soft-dependencies
- PlaceholderAPI: `util.FppPlaceholderExpansion` registered if plugin present.
- LuckPerms: detected for prefix formatting (`FakePlayerPlugin` defers detection 1 tick).
- Metrics: `util/FppMetrics` wraps FastStats and is conditional via `config.yml` flag (`Config.metricsEnabled()`).

8) Useful repo search queries (quick semantic/grep targets)
- "onEnable(" — find startup flow
- "Config.init(" — find config usage
- "getCommand(\"fpp\")" — command wiring
- "registerEvents(" — listener registration
- "purgeOrphanedBodiesAndRestore(" — persistence restore
- "spawnFakePlayer(" or "PacketHelper" — low-level spawn

9) Where to read docs
- `wiki/` — developer-facing docs (Commands.md, Configuration.md, Getting-Started.md)
- `README.md` — general usage and release notes

Contact / notes
- This file focuses on discoverable, actionable patterns only. For design rationale, see `wiki/Migration.md` and issues in the project tracker.

