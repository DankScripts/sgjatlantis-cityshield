# Atlantis City Shield 1.0.2

## Source Branches

- [Forge 1.20.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.20.1-forge)
- [NeoForge 1.21.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.21.1-neoforge)

First NeoForge port of the Atlantis City Shield mod.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219 or newer within the 21.1 series
- Java 21
- SGJourney 0.6.50 or newer for NeoForge 1.21.1

Install the release JAR alongside SGJourney in the instance's `mods` directory.
Do not install the Forge 1.20.1 JAR in a NeoForge instance. Keep only one version
of Atlantis City Shield installed.

## Build

With Java 21 selected, run `gradlew.bat release` on Windows or
`./gradlew release` elsewhere. The JAR and complete buildable source ZIP are
written to `dist/`. SGJourney is a mandatory dependency and is not bundled.

## Shield Setup

Place the Shield Controller and a Shield Emitter within controller range.
Connect the emitter to a loaded ZPM Hub using Large Naquadah Cable. Native hub
energy is used without requiring the optional ZPM item energy capability.
The default draw remains 100,000,000 FE per tick.

With the shield off, sneak-use the Shield Surveyor on the controller to bind it.
Sneak-use the air to select CENTER, NORTH, SOUTH, EAST, WEST, TOP, and BOTTOM,
and use blocks to mark each point. CENTER is the middle of the city at water
level. Open the settings with the Surveyor, check Preview, and select Commit.

Unprogrammed standard communication crystals add 16 blocks of controller range;
advanced crystals add 32 blocks. Programmed crystals do not extend that range.

## Port Scope

Retains the accepted controller geometry, textures, powered rear lightbar,
activation sound, survey workflow, projected hologram, shield defenses, and
weather barrier. Networking, energy capabilities, chunk tickets, item data,
menus, rendering APIs, recipes, and loot directories use the 1.21.1 APIs.

This is not a world-conversion tool. Forge 1.20.1 saves are not certified for
migration to Minecraft 1.21.1. Back up saves before upgrading platforms.
