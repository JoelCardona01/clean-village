package dev.cleanvillage.fabric;

import dev.cleanvillage.CleanVillage;
import dev.cleanvillage.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Fabric entry point: registers everything straight into the vanilla registries. */
public final class CleanVillageFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CleanVillage.init(FabricLoader.getInstance().getConfigDir());
        ModContent.registerSounds((id, s) -> Registry.register(BuiltInRegistries.SOUND_EVENT, id, s));
        ModContent.registerBlocks((id, b) -> Registry.register(BuiltInRegistries.BLOCK, id, b));
        ModContent.registerItems((id, i) -> Registry.register(BuiltInRegistries.ITEM, id, i));
        CreativeModeTabEvents.modifyOutputEvent(ModContent.TOOLS_TAB).register(output -> {
            for (Item item : ModContent.TAB_ITEMS) output.accept(item);
        });
    }
}
