# AGENTS.md — Guide for AI coding agents

Purpose: help automated agents quickly understand, modify, and verify the FakePlayerPlugin codebase.

1) Quick repo map
- `src/main/java/me/bill/fakePlayerPlugin/` — main code and entry points
- `src/main/resources/` — default `config.yml`, `plugin.yml`, `bot-names.yml`, `bot-messages.yml`
- `pom.xml` — build + jar metadata (`target/fpp-*.jar` produced)
- `wiki/` — feature docs (Commands.md, Configuration.md, Getting-Started.md)

2) Where to start (entry points)
- `FakePlayerPlugin.java` — plugin lifecycle (`onEnable` / `onDisable`) and orchestration.
- `command/CommandManager.java` and command classes (e.g. `SpawnCommand.java`, `ReloadCommand.java`) — command wiring and tab completion.
- `config/Config.java`, `util/ConfigMigrator.java` — configuration pattern and automatic migration.
- `fakeplayer/FakePlayerManager.java`, `fakeplayer/BotPersistence.java` — bot lifecycle, persistence and restore.

3) Key patterns & concrete examples (copy-ready pointers)
- Migration runs before config init: `ConfigMigrator.migrateIfNeeded(this)` in `onEnable` (see `FakePlayerPlugin.java`).
- Config usage: call `Config.init(this)` then use accessors like `Config.fakeChatEnabled()` in code.
- Command registration: plugin registers commands via `commandManager.register(new SpawnCommand(...))` and then `getCommand("fpp").setExecutor(commandManager)` (see `onEnable`).
- Listener registration: `getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, fakePlayerManager), this)` — listeners receive plugin + manager references.
- Persistence lifecycle: restore on enable via `botPersistence.purgeOrphanedBodiesAndRestore(fakePlayerManager)` and save on disable via `botPersistence.save(fakePlayerManager.getActivePlayers())`.
- Metrics & soft-deps: metrics are initialized with `FppMetrics` when `Config.metricsEnabled()`; PlaceholderAPI expansion registered conditionally in `onEnable`.

4) Common tasks & minimal steps (how to implement typical changes)
- Add a new command:
  - Create `src/main/java/me/bill/fakePlayerPlugin/command/MyCommand.java` implementing the command interface used by `CommandManager`.
  - Register it in `FakePlayerPlugin.onEnable()` with `commandManager.register(new MyCommand(...))`.
  - Ensure `plugin.yml` contains the `fpp` command or sub-command docs (command parsing done by `CommandManager`).
- Add a config option:
  - Add default to `src/main/resources/config.yml`.
  - If needed, update `Config` accessor and `ConfigMigrator` to preserve old configs.
  - New options should be validated in `util/ConfigValidator.java` if they can be misconfigured.

5) Build / run / verify (Windows PowerShell)
Build (skip tests):
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

