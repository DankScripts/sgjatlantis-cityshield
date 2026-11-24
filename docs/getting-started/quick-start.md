---
layout: default
title: Quick Start
parent: Getting Started
nav_order: 3
---

# Quick Start

## What you need

- **Shield Controller**: the control table, settings, and power monitor.
- **Shield Emitter**: the separate block that generates the shield and receives power.
- **Shield Surveyor**: the tool for fitting the shield to your build.
- A powered Stargate Journey **ZPM Hub** and Large Naquadah Cable, or an enabled ordinary FE power supply.

Check JEI or another recipe viewer for your installed modpack's recipes. The shield can be used without installing the Atlantis city addon.

## Setup

1. Place the controller and emitter in the same dimension, within the controller's communication range. They link automatically when an eligible emitter is available.
2. Connect power to the **emitter**, not the controller. See [Power]({{ '/guides/power.html' | relative_url }}) for the two supported routes.
3. If necessary, survey the area with the Shield Surveyor and commit the shape; see [Surveying]({{ '/guides/surveying.html' | relative_url }}).
4. Open the controller and activate the shield. Allow the field to form before relying on its protection. If activation fails, check the emitter, power, and redstone settings.

Some bundled Atlantis city controllers start with a pre-surveyed shape when the compatible city addon is present. Existing saved surveys are preserved. Other controllers can be surveyed manually.

## Controller range and crystals

The base controller-to-emitter range is **16 blocks**. The table has eight communication crystal slots: each unprogrammed standard crystal adds 16 blocks and each unprogrammed advanced crystal adds 32. Programmed crystals do not add range.

Open **Crystal Controls** at the table, or use a crystal on the table to insert it. Sneak-use the table with an empty hand to remove the last installed crystal. Removing a crystal can shut down an active shield if its emitter is then out of range.
