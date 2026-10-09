package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldEmitterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AtlantisCityShield.MOD_ID);

    public static final RegistryObject<BlockEntityType<ShieldControllerBlockEntity>> SHIELD_CONTROLLER =
            BLOCK_ENTITIES.register("shield_controller", () -> BlockEntityType.Builder
                    .of(ShieldControllerBlockEntity::new, ModBlocks.SHIELD_CONTROLLER.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<ShieldEmitterBlockEntity>> SHIELD_EMITTER =
            BLOCK_ENTITIES.register("shield_emitter", () -> BlockEntityType.Builder
                    .of(ShieldEmitterBlockEntity::new, ModBlocks.SHIELD_EMITTER.get())
                    .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
