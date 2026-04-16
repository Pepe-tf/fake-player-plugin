# FakePlayerPlugin - Build Guide

## Quick Build

```bash
mvn clean package
```

**Output:** `target/fpp-1.6.5.jar` (~16.95 MB)

---

## Build Outputs

| Location | File | Auto-deployed |
|----------|------|---------------|
| `target/` | `fpp-1.6.5.jar` | ✅ Main output |
| `build/` | `fpp.jar` | ✅ Project-local copy |
| `${deploy.dir}/` | `fpp.jar` | ✅ Server plugins folder |

Default deploy location: `%USERPROFILE%\Desktop\dmc\plugins\fpp.jar`

Override with: `mvn clean package -Ddeploy.dir=C:\path\to\server\plugins`

---

## Requirements

- **JDK 21+** (for compilation)
- **Maven 3.8+**

---

## Build Process

1. **Compile** - Java source → bytecode (JDK 21)
2. **Shade** - Bundle SQLite & MySQL JDBC drivers
3. **Package** - Create final JAR with resources
4. **Deploy** - Copy to `build/` and server plugins folder

---

## FastStats Note

FastStats JARs are **not shaded** - they're bundled as binary resources in `src/main/resources/faststats/` and loaded via URLClassLoader at runtime.

---

## Skip Tests

```bash
mvn clean package -DskipTests
```

---

## Clean Build

```bash
mvn clean
mvn package
```

Or combined:
```bash
mvn clean package
```

---

## Validate Configuration

```bash
mvn validate
```

Checks that `pom.xml` is well-formed.

---

## IDE Integration

### IntelliJ IDEA
1. Open **Maven** tool window
2. Expand project → **Lifecycle**
3. Double-click **package**

Or use the **"Build Plugin"** run configuration.

---

## Troubleshooting

### Build fails with "package does not exist"
- Ensure JDK 21+ is configured
- Check `JAVA_HOME` environment variable
- Run `mvn clean` to clear stale classes

### JAR not deployed to server
- Check `deploy.dir` property in `pom.xml`
- Verify folder exists and is writable
- Check console output for `[FPP] Deployed JAR to:` message

### Dependency errors
- Run `mvn clean install` to refresh dependencies
- Check internet connection (Maven needs to download deps)
- Verify Maven repositories are accessible

---

## Version Information

- **Plugin Version:** 1.6.5 (see `pom.xml`)
- **MC Version:** 1.21.11
- **API:** Paper 1.21.11-R0.1-SNAPSHOT

---

## Build Time

Typical clean build: **~10-15 seconds**

