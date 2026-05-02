# 📦 Extensions

> **Addon API for third-party developers — v1.6.6.8**

FPP provides a lightweight extension API that lets other plugins hook into the bot system without modifying FPP itself. Drop compiled `.jar` files into `plugins/FakePlayerPlugin/extensions/` and they are loaded automatically on startup.

---

## 🏗️ Extension Loader

The `ExtensionLoader` scans `plugins/FakePlayerPlugin/extensions/` for `.jar` files on every startup and reload (`/fpp reload`). Valid extensions implement the `FppExtension` interface and declare a service provider.

### Loading order

1. JAR files are discovered in `plugins/FakePlayerPlugin/extensions/`.
2. Each JAR is inspected for classes implementing `FppExtension` via direct JAR scanning (no `META-INF/services` required).
3. Valid extensions are sorted by `getPriority()` (lower = earlier), then alphabetically by `getName()`.
4. Dependencies (`getDependencies` / `getSoftDependencies`) are resolved before `onEnable` is called.
5. Each extension receives a `FppApi` reference in `onEnable(FppApi)`.
6. The extension can then register commands, tick handlers, settings tabs, or interact with the bot registry.

### Directory layout

```text
plugins/FakePlayerPlugin/
└── extensions/
    ├── my-extension.jar
    └── another-addon.jar
```

> Extensions are **not** Bukkit plugins. They are FPP-specific addons that live inside the FPP extensions folder.

---

## 🧩 `FppExtension` Interface

The entry point every extension must implement:

```java
public interface FppExtension {

    String getName();

    String getVersion();

    default String getDescription() { return ""; }

    default List<String> getAuthors() { return List.of(); }

    default List<String> getDependencies() { return List.of(); }

    default List<String> getSoftDependencies() { return List.of(); }

    default int getPriority() { return 100; }

    void onEnable(FppApi api);

    default void onDisable() {}
}
```

| Method | Default | Description |
|--------|---------|-------------|
| `getName()` | — | **Required.** Unique extension name |
| `getVersion()` | — | **Required.** Extension version string |
| `getDescription()` | `""` | Short description |
| `getAuthors()` | empty | List of author names |
| `getDependencies()` | empty | Extension names that **must** be loaded first |
| `getSoftDependencies()` | empty | Extension names that should be loaded first if present |
| `getPriority()` | `100` | Load order — **lower = earlier**. Extensions are sorted by priority, then name |
| `onEnable(FppApi)` | — | **Required.** Called after all dependencies are resolved |
| `onDisable()` | no-op | Called during server stop or `/fpp reload extensions` |

### Config & Resource Methods

Extensions can ship their own config and resource files. The `FppExtension` base provides:

| Method | Description |
|--------|-------------|
| `getDataFolder()` | Returns the extension's private data directory (`plugins/FakePlayerPlugin/extensions/<name>/`) |
| `getConfig()` | Loads and returns the extension's `config.yml` from its data folder |
| `saveDefaultConfig()` | Copies the bundled `extension-config.yml` from the JAR to the data folder (does not overwrite) |
| `saveDefaultResources()` | Extracts all files under `extension-resources/` in the JAR to the data folder (does not overwrite existing) |
| `saveResource(jarPath)` | Extracts a specific file from the JAR into the data folder |
| `reloadConfig()` | Reloads the extension config from disk |

### FppApi Cross-Extension Methods

Access another extension's config or data from your own extension:

| Method | Description |
|--------|-------------|
| `getExtensionDataFolder(name)` | Get another extension's data directory |
| `saveDefaultExtensionConfig(name)` | Extract another extension's default config (useful for compatibility) |
| `getExtensionConfig(name)` | Load another extension's config file as a `YamlConfiguration` |

### Extension JAR Layout

```text
my-extension.jar
├── com/example/MyExtension.class
├── extension-config.yml          # Default config (extracted by saveDefaultConfig)
├── extension-resources/          # All files here auto-extracted by saveDefaultResources
│   ├── data.json
│   └── templates/
│       └── welcome.txt
└── META-INF/services/
    └── me.bill.fakePlayerPlugin.extension.FppExtension
```

---

## 📚 What Extensions Can Do

Because the extension receives the live `FppApi` reference, it can:

- Access the active bot registry (`FppApi.getBots()`, `getBot(name)`, etc.)
- Register custom Bukkit event listeners
- Register FPP addon commands (`api.registerCommand(...)`)
- Register bot tick handlers (`api.registerTickHandler(...)`)
- Register settings tabs (`api.registerSettingsTab(...)`, `api.registerBotSettingsTab(...)`)
- Hook into the bot lifecycle via FPP events (`FppBotSpawnEvent`, `FppBotDespawnEvent`, etc.)
- Read and react to configuration values
- Use per-extension config and resource files

### Common use cases

| Use case | Approach |
|----------|----------|
| Custom bot AI | Listen for spawn events and attach custom runnable logic |
| External data sync | On spawn, push bot state to an external dashboard or database |
| Custom commands | Register subcommands through Bukkit's command map or intercept existing ones |
| Economy integration | Reward players when their bots reach milestones |
| Discord bridging | Mirror bot chat messages to a Discord channel |

---

## 🛠️ Building an Extension

### Gradle setup

```groovy
repositories {
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT'
    compileOnly files('path/to/FakePlayerPlugin.jar')
}
```

### Minimal example

```java
package com.example;

import me.bill.fakePlayerPlugin.api.FppApi;
import me.bill.fakePlayerPlugin.extension.FppExtension;

public class MyExtension implements FppExtension {

    @Override
    public String getName() { return "MyExtension"; }

    @Override
    public String getVersion() { return "1.0.0"; }

    @Override
    public void onEnable(FppApi api) {
        api.getPlugin().getLogger().info("MyExtension loaded!");
        saveDefaultConfig();
        saveDefaultResources();
    }

    @Override
    public void onDisable() {
        api.getPlugin().getLogger().info("MyExtension disabled!");
    }
}
```

---

## 🔄 Reloading

Extensions are fully reloaded when `/fpp reload extensions` runs:

1. All existing extensions receive `onDisable()`.
2. JARs are re-scanned.
3. Valid extensions are sorted by priority then name, and receive `onEnable(FppApi)`.

---

## 📝 Notes

- Extensions must be compiled for **Java 21+** to match FPP's runtime.
- Paper API classes used by extensions should target **1.21.x**.
- FPP does **not** currently expose a Maven repository; compile against the FPP JAR directly.
- Errors during extension load are logged but do **not** prevent FPP from starting.

---

## 🔗 Related Pages

- [Home](Home)
- [Commands](Commands)
- [Configuration](Configuration)
