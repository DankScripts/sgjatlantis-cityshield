package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch;
import com.mustangdoc.sgjpatch.block.AtlantisDHDBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SGJPatchBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SGJAdditionsCapacityPatch.MOD_ID);

    public static final DeferredBlock<AtlantisDHDBlock> ATLANTIS_DHD = BLOCKS.register(
            "atlantis_dhd",
            () -> new AtlantisDHDBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    private SGJPatchBlocks() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
