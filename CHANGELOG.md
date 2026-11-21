# Changelog

## Unreleased

- Recognize the planned `stargate_atlantis` mod and floor conduits while
  retaining compatibility with the existing Atlantis namespaces.

## 1.0.4-dev-78-shared-shield-controls

- Let all players use activation, Crystal Controls, and the scale slider at
  the shield table. Keep advanced settings and the Surveyor operator-only on
  dedicated servers, and available to the local single-player host without cheats.

## 1.0.4-dev-77-singleplayer-controls

- Restore shield activation, settings, and Surveyor access for the local
  world host without requiring cheats. Dedicated-server permissions are unchanged.

## 1.0.4-dev-76-main-menu-first

- Always open the main shield page first. Crystal Controls is a submenu with
  a Back button for all viewers. Shield activation and settings still
  require operator permissions. Multiplayer clients and servers must match.

## 1.0.4-dev-75-controller-menu

- Use the server's permission decision when opening the shield controller so
  operators start on the main control page. Non-operators still start on
  Crystal Controls. Multiplayer clients and servers must use matching builds.

## 1.0.3-dev-74

- Initialize the bundled Lantea shield controller with the city's surveyed shield geometry when it has no saved definition. Existing surveys, manual controllers, and normal emitter linking are unchanged.

## 1.0.2

- Detects Large Naquadah Cable blocks beneath the emitter through up to two
  intervening blocks, even when the concealed cable has no block entity.
- Resolves live endpoints through the native cable network and periodically
  refreshes routes so cable topology changes do not leave stale power sources.
- Updated the required SGJourney version to 0.6.50.
- Detects, drains, and monitors native ZPM Hub storage without requiring the
  optional ZPM item energy capability.
- Preserves the accepted controller model, powered rear lightbar, activation
  sound, shield settings, survey data, and default 100,000,000 FE/t draw.
- Released separately for Forge 1.20.1 and, for the first time, NeoForge 1.21.1.

## 1.0.1-beta.2

### Added

- Added a live Zero-Point Power monitor to the Shield Control Table.
- Added aggregate remaining-energy and capacity readings for every unique ZPM
  Hub on the connected Large Naquadah Cable network.
- Added live shield draw, estimated runtime, and a visual charge bar.
- Added automatic PFE, TFE, GFE, MFE, kFE, and FE display scaling.

### Changed

- Locked the accepted powered rear lightbar at block light `15`, sky light `15`,
  and ambient occlusion disabled on only its four lightbar faces.
- ZPM monitoring now includes all remaining SGJourney entropy segments instead
  of showing only the current segment.
- Power monitoring is passive and does not increase shield energy consumption.
- The default shield draw remains 100,000,000 FE per tick.

### Fixed

- Fixed large synchronized energy values being truncated to 16 bits.
- Fixed the runtime estimate reaching zero and resetting at each entropy level.
- Fixed a client crash when opening Crystal Controls caused by an incorrect
  Forge slot-list bytecode descriptor.
