package com.dankscripts.sgjatlantis_cityshield;

import com.dankscripts.sgjatlantis_cityshield.config.ShieldConfig;
import com.dankscripts.sgjatlantis_cityshield.network.ModNetwork;
import com.dankscripts.sgjatlantis_cityshield.registry.ModBlockEntities;
import com.dankscripts.sgjatlantis_cityshield.registry.ModBlocks;
import com.dankscripts.sgjatlantis_cityshield.registry.ModItems;
import com.dankscripts.sgjatlantis_cityshield.registry.ModMenus;
import com.dankscripts.sgjatlantis_cityshield.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(AtlantisCityShield.MOD_ID)
public final class AtlantisCityShield {
    public static final String MOD_ID = "sgjatlantis_cityshield";
    public static final Logger LOGGER = LogUtils.getLogger();
        public static final net.neoforged.neoforge.common.world.chunk.TicketController CHUNK_TICKETS =
            new net.neoforged.neoforge.common.world.chunk.TicketController(ResourceLocation.fromNamespaceAndPath(MOD_ID, "shield"));

    public AtlantisCityShield(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ShieldConfig.SPEC);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModSounds.register(modBus);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::onConfigLoading);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) -> event.register(CHUNK_TICKETS));
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SHIELD_EMITTER.get(),
                (emitter, side) -> emitter.energyForSide(side));
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != ShieldConfig.SPEC) return;
        if (ShieldConfig.migratePowerDraw()) ShieldConfig.SPEC.save();
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "functional_blocks"))) {
            event.accept(ModItems.SHIELD_CONTROLLER_ITEM.get());
            event.accept(ModItems.SHIELD_EMITTER_ITEM.get());
        }
        if (tab.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "tools_and_utilities"))) {
            event.accept(ModItems.SHIELD_SURVEYOR.get());
        }
    }
}
