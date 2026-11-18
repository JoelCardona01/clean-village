package dev.spotless.neoforge;

import dev.spotless.Spotless;
import dev.spotless.registry.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point: registration through RegisterEvent on the mod bus. */
@Mod(Spotless.MOD_ID)
public final class SpotlessNeoForge {
    public SpotlessNeoForge(IEventBus modBus, ModContainer container) {
        Spotless.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(RegisterEvent.class, e -> {
            if (e.getRegistryKey().equals(Registries.SOUND_EVENT)) {
                e.register(Registries.SOUND_EVENT, h -> ModContent.registerSounds(h::register));
            } else if (e.getRegistryKey().equals(Registries.BLOCK)) {
                e.register(Registries.BLOCK, h -> ModContent.registerBlocks(h::register));
            } else if (e.getRegistryKey().equals(Registries.ITEM)) {
                e.register(Registries.ITEM, h -> ModContent.registerItems(h::register));
            }
        });
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, e -> {
            if (e.getTabKey().equals(ModContent.TOOLS_TAB)) {
                for (Item item : ModContent.TAB_ITEMS) e.accept(item);
            }
        });
    }
}
