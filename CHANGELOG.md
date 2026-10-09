# Changelog

## 1.0.2

- Detects Large Naquadah Cable blocks beneath the emitter through up to two
  intervening blocks, even when the concealed cable has no block entity.
- Resolves live endpoints through the native cable network and periodically
  refreshes routes so cable topology changes do not leave stale power sources.
- First NeoForge 1.21.1 release, targeting NeoForge 21.1.219 and Java 21.
- Requires SGJourney 0.6.50 and uses native ZPM Hub zero-point energy storage.
- Migrated custom networking to NeoForge payload registration and distribution.
- Migrated energy exposure to registered block-entity capabilities and chunk
  forcing to a registered ticket controller.
- Migrated item state to data components and inventory persistence to
  registry-aware serialization. Uses SGJourney's frequency component API.
- Updated protection events, menus, block interactions, renderer buffers,
  model loaders, mixin targets, access transformers, recipes, and loot tables.
- Retains accepted shield/table visuals, activation audio, survey mechanics,
  and the default 100,000,000 FE per tick power draw.
