package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlock;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldEmitterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AtlantisCityShield.MOD_ID);

    public static final RegistryObject<Block> SHIELD_CONTROLLER = BLOCKS.register("shield_controller",
            () -> new ShieldControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> switch (state.getValue(ShieldControllerBlock.VISUAL_STATE)) {
                        case FORMING, ACTIVE -> 12;
                        case WARNING -> 8;
                        case STANDBY -> 2;
                    })));

    public static final RegistryObject<Block> SHIELD_EMITTER = BLOCKS.register("shield_emitter",
            () -> new ShieldEmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0F, 12.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
