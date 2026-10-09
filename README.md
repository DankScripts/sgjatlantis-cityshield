# Atlantis City Shield

## Source Branches

- [Forge 1.20.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.20.1-forge)
- [NeoForge 1.21.1](https://github.com/DankScripts/sgjatlantis-cityshield/tree/1.21.1-neoforge)

The `main` branch contains the Forge 1.20.1 release sources.

## v1.0.2 release

Forge 1.20.1 release for SGJourney 0.6.50 or newer, requiring Java 17.
Uses the native ZPM Hub zero-point energy API even when optional item energy
capabilities are disabled. The user-confirmed table visuals, survey system,
shield defenses, weather barrier, and default 100,000,000 FE/t draw are retained.
The separate NeoForge 1.21.1 release also carries version 1.0.2.

## v1.0.1-beta.2 baseline

This source matches the accepted Forge 1.20.1 Beta 2 release baseline locked on
2026-09-03. The exact runtime JAR and checksums are stored in
`baseline/accepted-balanced-bars/`. Its powered rear lightbar uses full client
lighting on only its four visible bar faces, while standby appearance and shield
mechanics remain unchanged. The shield control table passively monitors the
complete energy and live draw of all unique ZPM Hubs on the shield emitter's
Large Naquadah Cable network.
The display uses TFE for trillion-FE values and automatically steps down to
GFE, MFE, kFE, and FE as the remaining charge decreases. Monitoring does not
change the configured shield drain, whose default remains 100,000,000 FE/t.

Standalone Forge 1.20.1 mod for fitting, powering, and operating a city-scale Atlantis shield.

## Build

Run `./gradlew release`. The reobfuscated mod JAR is written to `build/libs/`
and the complete source archive is written to `dist/`.

## 0.1.0-alpha.31

Alpha 31 separates the shield controls from the hardware that generates the field:

- Restores the original compact prismarine Shield Emitter as its own craftable block, using the original emitter recipe from the early builds.
- Moves the Large Naquadah Cable and ZPM power connection to the Shield Emitter. The existing control table now operates the emitter instead of generating the shield itself.
- Preserves every existing surveyed world coordinate and shield setting. Neither the emitter's position nor its orientation changes the saved shield geometry.
- Gives the control table eight SGJourney communication-crystal slots. The table reaches 16 blocks by default; unprogrammed standard crystals add 16 blocks each and unprogrammed advanced crystals add 32 blocks each, matching SGJourney's DHD range rules.
- Lets operators install a communication crystal by using it on the table and remove the last installed crystal by sneak-using the table with an empty hand.
- Leaves the approved Alpha 30 table model, GUI, table recipe, wireframe hologram, crystal deck, and squared front corners unchanged.

## 0.1.0-alpha.30

Alpha 30 makes one focused correction to the approved controller table:

- Squares the two front fascia corner pieces with the table instead of leaving them turned outward.
- Preserves the Alpha 29 wireframe hologram, table geometry, crystal layout, GUI, survey system, shield behavior, and every other accepted feature unchanged.

## 0.1.0-alpha.29

Alpha 29 corrects the shield hologram projected by the controller table:

- Shrinks the table hologram so it remains over the central projector instead of overlapping the nearby flat crystals.
- Preserves the clear wireframe hologram appearance.
- Stops changing the hologram's radius and height during activation, removing the balloon-like inflation.
- Reveals the fixed-size wireframe from the projector upward with the same six-second coverage timing as the real shield.
- Adds a soft moving formation edge while keeping the existing animated cyan surface and grid lines.
- Keeps all Alpha 28 Surveyor instructions and every other Alpha 27 shield system unchanged.

## 0.1.0-alpha.28

Alpha 28 makes the Shield Surveyor understandable without outside instructions:

- Adds a complete, numbered **Easy Survey Guide** directly to the Surveyor tooltip.
- Explains every mouse action in plain language and shows the correct marking order.
- Clearly explains that **CENTER means the middle of the city at water level**, not the player's flying position.
- Shows a simple instruction for the currently selected mark and repeats it whenever the player changes survey modes.
- Gives clearer connection, marking, preview, and Commit messages while preserving all Alpha 27 shield behavior.

### Shield Surveyor: Easy Guide

1. Turn the shield **OFF**.
2. Hold the Shield Surveyor and sneak-right-click the shield controller. This connects the tool to that controller.
3. Sneak-right-click the air to choose what you want to mark.
4. Right-click a block to save the chosen mark.
5. Mark the points in this order: **CENTER, NORTH, SOUTH, EAST, WEST, TOP, BOTTOM**. **BOUNDARY** is optional.
6. CENTER must be the middle of the city at water level. Do not mark CENTER while flying high above the city.
7. NORTH and SOUTH share one radius; EAST and WEST share another. Mark the farther edge in each pair last.
8. Right-click the air without sneaking to open the shield settings from anywhere nearby.
9. Check the shield Preview. If it looks correct, press **Commit**.

What each mark means:

- **CENTER:** The middle of the city at water level. This is the shield's widest horizontal level.
- **NORTH / SOUTH:** The north and south edges of the city.
- **EAST / WEST:** The east and west edges of the city.
- **TOP:** The highest point the shield should reach.
- **BOTTOM:** The lowest point the shield should reach below the city.
- **BOUNDARY:** Optional. Use this on a horizontal city edge still outside the preview to make the shield wider.

## 0.1.0-alpha.27

Alpha 27 brings the shield console into the same physical scale and visual language as the Atlantis DHD table:

- Enlarges the complete shield table to the DHD table's full two-block-wide, 20-pixel-tall footprint while preserving the approved proportions and Z-frame construction.
- Updates the table-shaped selection and collision outline to follow the enlarged deck, legs, feet, and crossbeam.
- Realigns the projected shield hologram with the resized central projector.
- Replaces the twelve plain teal pads with the supplied patterned DHD crystal texture, mapped across each pad's top face.
- Preserves the Alpha 26 shield systems, GUI, power behavior, survey, saved geometry, light bar, and activation effects.

## 0.1.0-alpha.26

Alpha 26 matches the shield table's twelve DHD-style crystal pads to the approved Atlantis DHD crystal color:

- Replaces the pale illuminated pad texture with the supplied uniform **#04A2A1** teal texture byte-for-byte.
- Embeds the same source texture in the final Blockbench model and packages it for the in-game controller model.
- Preserves the Alpha 25 embedded Z-frame joints, rear light-bar correction, front corners, GUI, hologram, and all shield behavior.

## 0.1.0-alpha.25

Alpha 25 improves the physical construction of the controller table's Z-frame legs:

- Extends both diagonal sled supports into their floor rails and upper mounting rails instead of letting their ends stop just short of the adjoining structure.
- Uses the same embedded-joint principle as the Atlantis DHD table, making each Z-frame read as one continuous load-bearing assembly.
- Preserves the approved Z angle, leg spacing, cross-brace, tabletop, front corners, pale crystal pads, corrected rear light bar, GUI, and all shield behavior.

## 0.1.0-alpha.24

Alpha 24 corrects the two visual regressions found during Alpha 23 testing:

- Restores the twelve flat console pads to the pale illuminated crystal color shown in the approved Blockbench model.
- Preserves the complete six-face rear light-bar cuboid so the translucent table renders in its original stable quad order.
- Keeps the light bar inset between its end housings and gives its two hidden end faces the opaque brown fascia texture, preventing cyan bleed without removing geometry.
- Retains the Alpha 22 tabletop, Z-profile sled legs, collision, GUI, hologram, survey, shield, weather, defense, power, and portability behavior.

## 0.1.0-alpha.23

Alpha 23 makes one focused visual correction to the approved Alpha 22 controller table:

- Keeps the Alpha 22 table geometry, Z-profile sled legs, crystal colors, GUI, hologram, and all shield behavior unchanged.
- Insets the luminous rear status bar precisely between its opaque end housings.
- Removes the status bar's hidden end faces so cyan light can no longer bleed through either brown end cap.

## 0.1.0-alpha.22

Alpha 22 replaces the placeholder controller block with the approved Lantean control-room prop:

- Integrates the wide copper control table, angled sled supports, sloped crystal deck, recessed projector, and rear status light bar from the approved Blockbench model.
- Adds horizontal placement facing and a table-shaped selection/collision outline while retaining portable survey and shield geometry data when broken.
- Gives the controller distinct standby, shield-formation, active, and low-power warning appearances.
- Projects a small animated cyan shield hologram above the table while the city shield is forming or active.
- Redesigns the main controller GUI around only the shield toggle and Shield Settings button; survey geometry, preview, step size, commit, and cancel controls now live on the settings page.
- Preserves the existing shield field, six-second activation timing, supplied activation sound, defenses, weather barrier, ZPM cable power behavior, recipes, and configuration.

## 0.1.0-alpha.21

Alpha 21 replaces the experimental synthesized activation cues with the approved `cityshield.ogg` supplied for the project:

- Packages the supplied audio byte-for-byte as the shield-raising sound, without conversion, trimming, remixing, or loudness changes.
- Uses its native OGG Vorbis, 44.1 kHz stereo encoding and 5.927-second duration.
- Keeps the existing six-second shield-formation animation and activation timing unchanged.
- Does not change shield visuals, geometry, defenses, weather blocking, power behavior, shutdown behavior, or configuration.

## 0.1.0-alpha.20

Alpha 20 refines the shield-raising sound in response to in-game testing:

- Removes the continuous rising/rotating whir from the Alpha 19 activation cue.
- Keeps the accepted traveling formation pulses.
- Rebuilds the background around low zero-point pressure, broadband energy movement, and sparse non-tonal electrical glints.
- Preserves the final field lock and the existing 6.4-second sound/formation synchronization.
- Does not change shield visuals, geometry, defenses, weather blocking, power behavior, shutdown behavior, or configuration.

## 0.1.0-alpha.19

Alpha 19 replaces the original shield-raising sound while preserving the accepted six-second animation:

- Replaces the simple rising whistle/noise cue with a completely original, layered Atlantis-style activation effect.
- Builds from a deep zero-point generator surge into irregular Ancient-energy resonances, traveling wavefront pulses, and crystalline upper-field currents.
- Lands a restrained energy lock at six seconds, synchronized with the formation front reaching the dome's top center.
- Uses no audio sampled from *Stargate Atlantis* or any other copyrighted production.
- Does not change shield geometry, formation speed, shutdown behavior, defenses, weather blocking, power consumption, or configuration.

## 0.1.0-alpha.18

Alpha 18 establishes a more practical configurable city-shield power draw:

- Reduces the default continuous draw to **100,000,000 FE/t (100 MFE/t)**.
- Keeps `power.zpmDrawPerTick` in `config/sgjatlantis_cityshield-common.toml`, allowing server owners to choose any positive consumption value.
- Automatically migrates installations still using the Alpha 16 **1 GFE/t** or Alpha 17 **500 MFE/t** defaults to 100 MFE/t.
- Leaves other customized `zpmDrawPerTick` values unchanged.
- Preserves ZPM-only power, Large Naquadah Cable coupling, one-tick buffering, and maximum-entropy shutdown behavior.

## 0.1.0-alpha.17

Alpha 17 cuts the city shield's continuous ZPM consumption in half:

- Reduces the default active draw from **1 GFE/t** to **500 MFE/t**.
- Automatically migrates an existing Alpha 16 config that still uses the old 1 GFE/t default, so upgrading immediately applies the reduction without requiring a manual config edit.
- Leaves deliberately customized `zpmDrawPerTick` values unchanged.
- Preserves the one-tick zero-point buffer and maximum-entropy behavior: the final partial energy transfer still powers one last tick before the depleted ZPM drops the shield.
- Does not change activation, defenses, weather blocking, geometry, animation, cable coupling, or the requirement for a ZPM Hub and Large Naquadah Cable.

## 0.1.0-alpha.16

Alpha 16 makes a surveyed shield controller safely portable:

- Breaking the controller drops an item that retains the current survey, committed geometry, preview state, center, radii, upper height, lower depth, and margin.
- The retained dome remains at its surveyed world coordinates when the controller is placed elsewhere; moving the hardware does not drag the shield away from Atlantis.
- A replaced controller always comes back offline, even if it was broken while active. Its Large Naquadah Cable and ZPM Hub connection must still be valid before reactivation.
- Breaking a controller in Creative mode intentionally produces the retained controller item instead of silently deleting its survey.
- Surveyors bound to the old location are automatically rebound when they are in the placing player's inventory. Any other Surveyor can be rebound by sneak-using it on the relocated controller.
- Portable data is shown in the controller item's tooltip. The zero-point buffer and legacy ZPM inventory are never stored in the dropped item.

## 0.1.0-alpha.15

Alpha 15 adds concealed city-grid coupling for the Large Naquadah Cable:

- The cable may still touch any face of the shield controller directly.
- Alternatively, one or two ordinary blocks may sit between the controller and cable in a straight horizontal or vertical line. This allows the city power grid to remain beneath floors or behind walls.
- The controller locates the connected Large Naquadah Cable network and draws from a ZPM installed in an SGJourney ZPM Hub on that same network.
- The short bridge does not turn the intervening blocks into cables and does not accept generic FE, batteries, generators, smaller Naquadah cables, or an unconnected ZPM Hub.
- Cable routes are cached and validated by SGJourney network identity so the mod does not scan a city-sized cable grid every tick. Breaking the route or removing its ZPM still causes the shield to lose power and dissipate.

## 0.1.0-alpha.14

Alpha 14 moves city-shield power onto SGJourney's city-scale zero-point cable network:

- Removes the controller's internal ZPM bay. The ZPM now belongs in an SGJourney ZPM Hub, where it can power the city's existing infrastructure.
- Requires an SGJourney Large Naquadah Cable to touch any face of the shield controller.
- Implements the controller as a native SGJourney zero-point-energy receiver, allowing the ZPM Hub and cable network to transfer their full long-valued energy units without truncation.
- Rejects ordinary FE and non-zero-point cable traffic. A generator, battery, or generic FE network cannot substitute for a ZPM Hub.
- Keeps a one-tick buffer equal to `zpmDrawPerTick`, so power is consumed continuously without storing a large reserve after the cable is disconnected.
- Preserves maximum-entropy behavior: the final partial transfer from a nearly exhausted ZPM still powers one last tick, then the shield dissipates when no zero-point energy remains. Repeated partial supply cannot sustain the field indefinitely.
- Replaces the controller inventory with clear **Large Naquadah Cable** and **Zero-point grid** status readouts.
- Safely ejects any ZPM left inside an Alpha 13 controller into the world on first load instead of deleting it during the upgrade.

## 0.1.0-alpha.13

Alpha 13 introduces real SGJourney ZPM power, a synchronized shield-raising sound, and configurable lethal contact:

- Adds a dedicated ZPM bay to the controller interface. Only `sgjourney:zero_point_module` is accepted; batteries, generators, and other FE items cannot power the city shield.
- Requires a charged ZPM to activate and continuously draws `zpmDrawPerTick` while active. The default is 1 GFE/t, above SGJourney's non-ZPM technology tiers and below a ZPM's default 100 GFE/t output capability.
- Bases activation eligibility on ZPM output and the presence of any remaining energy, not a minimum stored-energy reserve. A module at 99.9% entropy can therefore power the field until its final energy is consumed at maximum entropy.
- Automatically dissipates the shield when its ZPM is removed, depleted, or unable to sustain the configured output.
- Keeps exactly the controller's own chunk ticking while the shield is active so entropy drains continuously; it does not force-load the thousand-block city or shield surface.
- Adds an original six-second Atlantis-style zero-point surge, electrical rise, and resonant lock-in synchronized with dome formation for players in and around the city.

- Adds `config/sgjatlantis_cityshield-common.toml` with `entityContactMode = "MAJOR_DAMAGE"` or `"INSTANT_DEATH"`.
- `MAJOR_DAMAGE` applies the configurable `majorContactDamage` value and repels anything that survives; the default is 40 damage (20 hearts) per collision.
- `INSTANT_DEATH` immediately kills living entities and removes other entities that touch a formed shield segment.
- Adds `creativePlayersBypassShield`; its strict default is `false`, while `true` lets creative players cross freely and unharmed.
- Keeps projectiles absorbed, the moving formation-front collision timing, and the future tuned personal-shield authorization hook.

## 0.1.0-alpha.12

Alpha 12 replaces the rain-sound wrapper with direct per-tick control of every active vanilla rain channel:

- Existing rain and rain-above sounds now fade through the exact same local multiplier as precipitation streaks and particles.
- Rain audio reaches silence on the tick local precipitation disappears, even while the shield continues forming toward the dome's top center elsewhere.
- No sound is forcibly stopped: each rain channel becomes inaudible smoothly and is allowed to finish naturally.
- Rain volume restores smoothly if the shield shuts down or the player moves back outside the field.
- Thunder and all non-rain sounds remain untouched.

## 0.1.0-alpha.11

Alpha 11 synchronizes the functional barrier and local weather with the moving formation front:

- Removes the six-second protection delay. Every visible surface segment becomes solid on the exact tick the rising formation edge reaches it, while portions not yet formed remain passable.
- Applies that moving boundary to entities, projectiles, attacks, explosions, pistons, and lightning; shutdown still removes protection immediately across the entire field.
- Tapers rain and snow streaks, precipitation particles, and existing rain ambience through a soft band that travels with the local leading edge instead of cutting them off abruptly.
- Uses one shared 120-tick formation calculation for rendering, collision, lightning interception, and weather suppression so the effects cannot drift out of sync.
- Keeps the real storm sky, clouds, thunder audio, and world rain/thunder state unchanged.

## 0.1.0-alpha.10

Alpha 10 synchronizes weather-barrier audio with the precipitation cutoff:

- Immediately stops any already-playing vanilla rain and rain-above sounds on the exact tick the completed shield begins suppressing precipitation.
- Continues preventing new rain ambience from starting while the camera remains inside the field.
- Does not stop lightning/thunder audio, alter the storm-dark sky, or change the world's rain/thunder state.
- Preserves Alpha 9's precipitation-only visual suppression and all Alpha 8 matter-barrier behavior.

## 0.1.0-alpha.9

Alpha 9 corrects the visual behavior of the automatic weather barrier:

- Keeps the world's real rain/thunder state, storm-dark sky, clouds, lighting, and thunder visible from beneath the shield.
- Suppresses only precipitation streaks, snow/rain particles, and local rain ambience while the camera is inside a completed shield.
- No longer sets the client rain or thunder level to zero, so raising the shield during a storm does not create artificial clear skies over Atlantis.
- Preserves Alpha 8's universal matter barrier, automatic lightning interception, impact effects, geometry, recipes, and future personal-shield access hook.

## 0.1.0-alpha.8

Alpha 8 makes protection an intrinsic, universal property of the active city shield:

- Removes the separate **Explosions**, **Projectiles**, and **Hostiles** controls and their obsolete saved settings.
- Blocks matter from crossing the completed field in either direction, including players, hostile and passive mobs, items, vehicles, projectiles, and falling blocks.
- Repels living and movable entities back to the side they came from without damaging them; projectiles are absorbed at the surface.
- Contains explosions on their originating side and cancels direct attacks across the field.
- Prevents pistons from pushing or pulling blocks across the mathematical shield boundary.
- Retains the automatic weather barrier and lightning interception from Alpha 7.
- Adds a central access hook for the planned frequency-tuned personal shield device. Until that device is implemented and active, no player is authorized to cross.
- Loads existing Alpha 1–7 shield geometry and activation state while ignoring all obsolete individual protection preferences.

## 0.1.0-alpha.7

Alpha 7 corrects the weather barrier controls introduced in Alpha 6:

- Removes the separate **Weather** toggle from the controller.
- Weather protection is now an intrinsic part of the field: it comes online automatically when shield materialization completes and shuts off with the shield itself.
- Alpha 6 saves that had Weather manually disabled are corrected automatically because that obsolete setting is ignored.
- Preserves precipitation suppression, lightning interception and impact flares, all Alpha 5 defenses, SGJourney recipes, and existing shield geometry.

## 0.1.0-alpha.6

Alpha 6 extends the active shield into a local weather barrier:

- Adds an independent **Weather** protection toggle to the controller, enabled by default on new and existing shields.
- Suppresses rain and snow visuals, precipitation particles, and storm sounds for a player located inside an operational shield.
- Intercepts lightning over the shield's horizontal footprint and displays a synchronized impact flare where the bolt reaches the upper dome.
- Leaves the world's global weather running normally, so players outside the shield continue to experience the same storm.
- Performs no city scan, block placement, or chunk force-loading. Existing snow and ice blocks are not removed.
- Loads Alpha 1–5 controller geometry and state while preserving all Alpha 5 protection and SGJourney recipes.

## 0.1.0-alpha.5

Alpha 5 turns the animated field into the first functional defensive shield:

- Intercepts incoming projectiles as they cross the mathematical shield boundary.
- Repels hostile mobs attempting to enter while allowing creatures already inside to remain unaffected.
- Prevents explosions outside the dome from damaging blocks or entities inside it.
- Blocks external projectile, explosion, and hostile-mob damage against protected entities.
- Produces synchronized localized cyan-white impact flashes and expanding energy ripples for all players in the dimension.
- Adds independent **Explosions**, **Projectiles**, and **Hostiles** protection toggles to the controller.
- Brings protection online after the accepted six-second materialization completes and takes it offline immediately when shutdown begins.
- Loads Alpha 1–4 controller geometry and state with all three new protection categories enabled by default.
- Replaces the placeholder recipes with SGJourney technology recipes: the controller requires exactly three Control Crystals plus Energy and Materialization Crystals, while the Surveyor uses Memory and Communication Crystals on a Naquadah-Copper Rod.

This remains a power-bypassed functional alpha. Continuous FE/ZPM drain, impact energy costs, warnings, and automatic power-loss collapse are the next power-system milestone.

## 0.1.0-alpha.4

Alpha 4 is a focused rendering correction for Alpha 3:

- Removes the apparent smaller bubble caused by the far hemisphere blending through the near shield surface.
- Uses camera-aware exterior face selection for the filled field and formation front.
- Removes the thin wandering trace-line overlay while retaining broad animated energy variation in the membrane.
- Keeps the complete inward-facing field visible when the player is standing inside the shield.
- Preserves Alpha 3 opacity, fluctuations, six-second materialization, simultaneous dissipation, controller data, and committed geometry.

## 0.1.0-alpha.3

Alpha 3 keeps Alpha 2's accepted six-second upward materialization timing while revising the field's appearance:

- The stable field is substantially more visible without hiding the city behind an opaque wall.
- Layered procedural blue/cyan energy variations continuously travel across the dome.
- Distorted luminous current lines and a brighter viewing-angle rim keep the active surface visibly alive.
- Deactivation no longer reverses the formation sweep. The entire dome flickers and dissipates simultaneously in roughly one second, similar to an energy iris shutting down.
- Alpha 1 and Alpha 2 controller data, committed geometry, Surveyor binding, and active state remain compatible.

## 0.1.0-alpha.2

Alpha 2 preserves Alpha 1 controllers and committed geometry while adding the first active energy field:

- Activate or deactivate the shield from the controller or a bound Surveyor's remote setup screen.
- Activation automatically commits the current geometry and turns Preview off.
- A bright formation front builds the translucent Atlantis-blue field from the lower perimeter to the dome's crown over six seconds.
- Deactivation collapses the visible field over three seconds.
- Active state and transition timing are server-authoritative, persistent, and synchronized for all players.
- Geometry controls are locked while the shield is active; deactivate before resizing it.
- Existing Boundary point fitting, 16,384-block radius limit, and 1/10/50/100/250 adjustment steps remain available.

This remains a power-free visual alpha. FE/ZPM drain, protection, impacts, damage interception, warnings, and power-loss collapse are later milestones.

## 0.1.0-alpha.1

Alpha 1 deliberately focuses on geometry setup:

- Place an **Atlantis Shield Controller**.
- Right-click it as an operator to open setup.
- Turn Preview on and adjust center X/Y/Z, Radius X/Z, Upper Height, Lower Depth, and measuring margin.
- Choose a step size of 1, 10, or 50 blocks for manual adjustments.
- Commit saves the working geometry; Cancel returns to the last committed geometry.
- Craft/use a **Shield Surveyor** for center, north, south, east, west, top, and bottom measurements.
- Sneak-right-click the Surveyor on a controller to bind it.
- Sneak-right-click in air to change mark type.
- Right-click a block to record that mark and update the preview.
- Right-click in air with a bound Surveyor to reopen setup remotely while viewing Atlantis from outside the dome.

The preview is client-rendered and does not place shield blocks, force-load the city, consume power, or provide protection.

Both server and clients need the mod. The controller and Surveyor are operator-only setup tools in this alpha.
