package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.SGJAdditionsCapacityPatch;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SGJPatchItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SGJAdditionsCapacityPatch.MOD_ID);

    public static final DeferredItem<BlockItem> ATLANTIS_DHD = ITEMS.register(
            "atlantis_dhd",
            () -> new BlockItem(SGJPatchBlocks.ATLANTIS_DHD.get(), new Item.Properties())
    );

    private SGJPatchItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
