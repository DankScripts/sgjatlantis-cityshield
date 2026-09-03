# Atlantis DHD 2.0.3-beta.3 — Minecraft 1.21.1 NeoForge

Targets:

- Minecraft 1.21.1
- NeoForge 21.1.219
- Java 21
- Stargate Journey 0.6.48-hotfix1

This release keeps the established 1.21.1 NeoForge implementation and ports
the relevant, accepted behavior from the Forge 1.20.1 2.0.3-beta.3 baseline.

Included changes:

- Current two-block-wide Atlantis control-table model with the accepted shield
  table frame and live physical button rendering.
- Normal right-click anywhere on the table opens the main DHD dialer.
- Sneak-right-click anywhere opens Crystal Controls when permitted.
- Dialing requires one Large Control Crystal, two Advanced Energy Crystals,
  and one Advanced Transfer Crystal.
- Advanced Communication Crystals are not part of the minimum operating set.
- Optional Pegasus control, communication, energy, memory, and transfer
  crystals retain SGJourney 1.21.1's native behavior when installed.
- A nearby Large Naquadah Cable can transfer power into the DHD through the
  restored floor block used by the Atlantis prop installation. A conditional
  capability proxy lets SGJourney use its native cable output path.
- Existing NeoForge GUI, shield control, waterlogging, capabilities, menus,
  symbol rendering, and migration aliases remain intact.

Runtime smoke-test checklist:

1. Confirm the block registers as `sgjatlantis_dhd:atlantis_dhd`.
2. Confirm the current table/frame model renders in-world and in inventory.
3. Confirm normal right-click opens the dialer and sneak-right-click opens
   Crystal Controls from the top and sides.
4. Confirm the DHD cannot enter symbols until the four-crystal minimum set is
   installed.
5. Confirm the DHD works without Advanced Communication Crystals, while
   optional Pegasus crystals retain their SGJourney functions.
6. Confirm a Large Naquadah Cable two blocks below the table supplies power
   through the intervening ordinary floor block.
