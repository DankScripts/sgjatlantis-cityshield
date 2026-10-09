package com.dankscripts.sgjatlantis_cityshield.registry;

import com.dankscripts.sgjatlantis_cityshield.AtlantisCityShield;
import com.dankscripts.sgjatlantis_cityshield.menu.ShieldControllerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Supplier;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, AtlantisCityShield.MOD_ID);

    public static final Supplier<MenuType<ShieldControllerMenu>> SHIELD_CONTROLLER =
            MENUS.register("shield_controller", () -> IMenuTypeExtension.create(ShieldControllerMenu::new));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
