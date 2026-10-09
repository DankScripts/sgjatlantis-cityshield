package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.item.ShieldSurveyorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AtlantisCityShield.MOD_ID);

    public static final RegistryObject<Item> SHIELD_CONTROLLER_ITEM = ITEMS.register("shield_controller",
            () -> new BlockItem(ModBlocks.SHIELD_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIELD_EMITTER_ITEM = ITEMS.register("shield_emitter",
            () -> new BlockItem(ModBlocks.SHIELD_EMITTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIELD_SURVEYOR = ITEMS.register("shield_surveyor",
            () -> new ShieldSurveyorItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
