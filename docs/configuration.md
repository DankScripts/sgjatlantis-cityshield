---
layout: default
title: Configuration
nav_order: 4
---

# Configuration and permissions

City Shield uses `config/sgjatlantis_cityshield-common.toml` in the relevant game or server directory. Stop the game or server before editing it. The Atlantis city file `sgjatlantis-config.toml` controls city installation and lighting, **not** these shield settings.

| Section | Setting | Purpose |
|:--|:--|:--|
| `matterBarrier` | `entityContactMode`, `majorContactDamage` | Select major damage or instant death and set the damage amount. |
| `matterBarrier` | `creativePlayersBypassShield` | Allow creative players to cross when true. |
| `power` | `zpmDrawPerTick`, `scaleDrawByVolume` | Configure ZPM base draw and volume scaling. |
| `power` | `allowGeneratorPower`, `generatorFePerTick`, `generatorFePerSide` | Configure ordinary FE input and draw. |
| `redstone` | `mode` | `DISABLED` for manual control, `HIGH_ENABLE` for high-signal activation, or `HIGH_DISABLE` for low-signal activation. Power and an emitter are still required. |

Manual activation is unavailable when redstone control is enabled. On Forge 1.0.4, players at the table can activate the shield and adjust its size; advanced geometry and the Surveyor require an operator on a dedicated server, or the local singleplayer host. On NeoForge 1.0.4, shield editing and activation require operator-level permissions. Ask the server administrator if controls are unavailable.
