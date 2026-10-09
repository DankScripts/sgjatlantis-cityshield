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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AtlantisCityShield.MOD_ID)
public final class AtlantisCityShield {
    public static final String MOD_ID = "sgjatlantis_cityshield";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AtlantisCityShield() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShieldConfig.SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModSounds.register(modBus);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::onConfigLoading);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != ShieldConfig.SPEC) return;
        if (ShieldConfig.migratePowerDraw()) event.getConfig().save();
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(new ResourceLocation("minecraft", "functional_blocks"))) {
            event.accept(ModItems.SHIELD_CONTROLLER_ITEM.get());
            event.accept(ModItems.SHIELD_EMITTER_ITEM.get());
        }
        if (tab.equals(new ResourceLocation("minecraft", "tools_and_utilities"))) {
            event.accept(ModItems.SHIELD_SURVEYOR.get());
        }
    }
}
