package com.mustangdoc.sgjpatch;

import com.mustangdoc.sgjpatch.client.render.AtlantisDHDButtonStateRenderer;
import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDCrystalScreen;
import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import com.mustangdoc.sgjpatch.config.SGJPatchClientConfig;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import com.mustangdoc.sgjpatch.init.SGJPatchBlocks;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchMenus;
import com.mustangdoc.sgjpatch.power.ConcealedFloorDhdPowerBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(SGJAdditionsCapacityPatch.MOD_ID)
public final class SGJAdditionsCapacityPatch {
    public static final String MOD_ID = "sgjatlantis_dhd";

    public SGJAdditionsCapacityPatch(IEventBus eventBus, ModContainer modContainer) {
        SGJPatchBlocks.register(eventBus);
        SGJPatchItems.register(eventBus);
        SGJPatchBlockEntities.register(eventBus);
        SGJPatchMenus.register(eventBus);
        eventBus.addListener(SGJAdditionsCapacityPatch::registerCapabilities);
        modContainer.registerConfig(ModConfig.Type.CLIENT, SGJPatchClientConfig.SPEC);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SGJPatchBlockEntities.ATLANTIS_DHD.get(),
                (blockEntity, direction) -> blockEntity.getEnergyHandler(direction)
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                SGJPatchBlockEntities.ATLANTIS_DHD.get(),
                (blockEntity, direction) -> blockEntity.getItemHandler(direction)
        );

        // Any ordinary floor material can conceal the installation. The
        // provider returns null everywhere except the exact cable-floor-DHD
        // arrangement, so unrelated blocks and machines remain untouched.
        Block[] blocks = BuiltInRegistries.BLOCK.stream().toArray(Block[]::new);
        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, blockEntity, direction) ->
                        ConcealedFloorDhdPowerBridge.getFloorEnergyCapability(
                                level, pos, blockEntity, direction),
                blocks
        );
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientModEvents {
        private ClientModEvents() {}

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(SGJPatchMenus.ATLANTIS_DHD.get(), AtlantisDHDScreenFixed::new);
            event.register(SGJPatchMenus.ATLANTIS_DHD_CRYSTAL.get(), AtlantisDHDCrystalScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(SGJPatchBlockEntities.ATLANTIS_DHD.get(),
                    AtlantisDHDButtonStateRenderer::new);
        }
    }
}
