# Grimholt Build

Requirements: JDK 25 and Gradle 9.7. CI installs the pinned versions.

Build:

    gradle clean test
    gradle assemble

Run:

    gradle run

The first run creates grimholt.properties with defaults. A custom configuration path can be supplied as the first application argument.

Phase 1 uses Minestom 2026.10.05-26.2 internally. No Bukkit, Spigot, Paper or Folia dependency is declared. Minestom classes are confined to the internal adapter package.
