# Atlantis City Shield 1.0.4

## Source Branches

- [Forge 1.20.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.20.1-forge)
- [NeoForge 1.21.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.21.1-neoforge)

First NeoForge port of the Atlantis City Shield mod.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219 or newer within the 21.1 series
- Java 21
- Matching SGJourney for NeoForge 1.21.1; the build target is the supplied 0.6.55-alpha

Install the release JAR alongside SGJourney in the instance's `mods` directory.
Do not install the Forge 1.20.1 JAR in a NeoForge instance. Keep only one version
of Atlantis City Shield installed.

## Build

Select Java 21 and provide the matching SGJourney 1.21.1 0.6.55-alpha binary:

```powershell
$env:SGJOURNEY_JAR = 'C:\path\to\Stargate Journey-1.21.1-0.6.55-alpha.jar'
.\gradlew.bat clean release
```

Alternatively, place that supplied binary in `libs/` with the filename shown
above. It is a private release sent by Povstalec and is not bundled, downloaded
from a public Maven coordinate, or replaced with the older 0.6.50 release.
Revalidate against the public beta when its binary becomes available.

The JAR and complete buildable source ZIP are written to `dist/`. The build
checks client renderer event registration and requires Java source in the
source ZIP for every packaged owned class. These checks do not launch Minecraft
or certify a new in-game test.

## Shield Setup

Power draw scales with the surveyed ellipsoid volume. The original
500 x 500 radius, 340-block-height field uses the configured base draw; shrinking
the field reduces it, down to a 1% minimum. Set `power.scaleDrawByVolume=false`
to retain a fixed draw.

For survival power without a ZPM, enable `power.allowGeneratorPower` (on by
default) and attach FE generators or cables directly to the Shield Emitter.
The standard-size field needs 100,000 FE/t, with a 25,000 FE/t limit per face,
so four powered faces are required at default settings. Both values are in the
server TOML. The existing ZPM Hub route remains available.

Set `redstone.mode` to `HIGH_ENABLE` or `HIGH_DISABLE` to make a redstone signal
at the controller switch the shield. `DISABLED` keeps manual GUI control. The
signal cannot activate the shield without a linked emitter and power.
Natural and chunk-generation mob spawns are blocked inside a fully formed,
active shield. Existing mobs and spawner-created mobs are unaffected.

The controller table's Shield Settings page has a live shield-envelope slider
from 10% to 100%. It scales the saved horizontal radii around the surveyed
center, including while the shield is active. The surveyed top and bottom
heights stay intact so the upper spire remains covered. Returning to
100% restores the surveyed full-city shape. The smaller enclosed volume also
reduces power draw when volume scaling is enabled. Set the surveyed center at
the central spire for a spire-focused minimum envelope.

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
