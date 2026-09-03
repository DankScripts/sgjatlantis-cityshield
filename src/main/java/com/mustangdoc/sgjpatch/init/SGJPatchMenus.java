package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SGJPatchMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SGJAdditionsCapacityPatch.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AtlantisDHDMenu>> ATLANTIS_DHD =
            registerMenuType(AtlantisDHDMenu::new, "atlantis_dhd");

    public static final DeferredHolder<MenuType<?>, MenuType<AtlantisDHDCrystalMenu>> ATLANTIS_DHD_CRYSTAL =
            registerMenuType(AtlantisDHDCrystalMenu::new, "atlantis_dhd_crystal");

    private SGJPatchMenus() {}

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            IContainerFactory<T> factory, String name) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
