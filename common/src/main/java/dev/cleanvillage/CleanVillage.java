package dev.cleanvillage;

import dev.cleanvillage.house.HouseManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** Common entry point shared by every loader. */
public final class CleanVillage {
    public static final String MOD_ID = "cleanvillage";
    public static final Logger LOG = LoggerFactory.getLogger("CleanVillage");

    private static boolean initialized;

    private CleanVillage() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Called once by the loader entry point, before registration. */
    public static void init(Path configDir) {
        if (initialized) return;
        initialized = true;
        Config.load(configDir);
        LOG.info("Clean Village loaded (village chance {}, houses {}-{})",
                Config.get().villageChance, Config.get().minHouses, Config.get().maxHouses);
    }

    /** Called at the end of every server tick (mixin). */
    public static void onServerTick(MinecraftServer server) {
        HouseManager.tick(server);
    }
}
