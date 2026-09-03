# Building

This port targets Java 21, Minecraft 1.21.1, NeoForge 21.1.219, and Stargate Journey 0.6.48-hotfix1.

A normal development machine with network access can build it with Gradle 8.x / the NeoForge ModDevGradle toolchain:

```bash
gradle clean build
```

The output JAR will be under `build/libs/`.

If you add a Gradle wrapper, use it instead:

```bash
./gradlew clean build
```

Do not substitute Forge 47.x dependencies: Stargate Journey's 1.21.1 line is NeoForge.
