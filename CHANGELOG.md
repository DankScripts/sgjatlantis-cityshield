# Changelog

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
