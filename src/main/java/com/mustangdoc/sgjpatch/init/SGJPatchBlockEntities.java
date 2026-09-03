package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SGJPatchBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SGJAdditionsCapacityPatch.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtlantisDHDEntity>> ATLANTIS_DHD =
            BLOCK_ENTITIES.register(
                    "atlantis_dhd",
                    () -> BlockEntityType.Builder.of(AtlantisDHDEntity::new, SGJPatchBlocks.ATLANTIS_DHD.get()).build(null)
            );

    private SGJPatchBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
