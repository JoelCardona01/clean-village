package dev.cleanvillage.forge;

import dev.cleanvillage.CleanVillage;
import dev.cleanvillage.registry.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge entry point (Forge 66 / EventBus 7): registration through RegisterEvent on the mod bus group. */
@Mod(CleanVillage.MOD_ID)
public final class CleanVillageForge {
    public CleanVillageForge(FMLJavaModLoadingContext context) {
        CleanVillage.init(FMLPaths.CONFIGDIR.get());
        BusGroup modBus = context.getModBusGroup();
        RegisterEvent.getBus(modBus).addListener(e -> {
            if (e.getRegistryKey().equals(Registries.SOUND_EVENT)) {
                e.register(Registries.SOUND_EVENT, h -> ModContent.registerSounds(h::register));
            } else if (e.getRegistryKey().equals(Registries.BLOCK)) {
                e.register(Registries.BLOCK, h -> ModContent.registerBlocks(h::register));
            } else if (e.getRegistryKey().equals(Registries.ITEM)) {
                e.register(Registries.ITEM, h -> ModContent.registerItems(h::register));
            }
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(ModContent.TOOLS_TAB)) {
                for (Item item : ModContent.TAB_ITEMS) {
                    e.accept(() -> item);
                }
            }
        });
    }
}
