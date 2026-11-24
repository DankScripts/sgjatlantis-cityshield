---
layout: default
title: Power
parent: Guides
nav_order: 2
---

# Power

## ZPM network

Connect a charged SGJourney ZPM Hub to the emitter with Large Naquadah Cable. The emitter can detect cable directly against it or beneath it with up to two intervening blocks. The controller monitors connected hub charge, estimated runtime, and live draw.

For the reference 500 by 500 radius, 340-block-high shield, the 1.0.4 source default is **20,000,000 FE/t** from a ZPM network. Impacts can briefly increase ZPM draw. Your actual setting or an older release may differ; check the common config and the in-game monitor.

## Ordinary FE

Ordinary FE power is enabled by default. Attach generators or FE cables directly to emitter faces. The reference-size default is **1,000 FE/t**, with a **250 FE/t per face** input limit, so four sufficiently supplied faces are needed at those defaults. Both values are configurable.

Power draw scales with the surveyed shield volume by default. In the 1.0.4 source it has a minimum of **50%** of the configured base draw; disabling volume scaling uses the base draw instead. Existing configuration values can differ from defaults. The shield shuts down if it loses its emitter connection or sufficient power.

See [Configuration]({{ '/configuration.html' | relative_url }}) to change the power settings.
