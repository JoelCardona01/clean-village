package dev.spotless;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config stored in config/spotless.json. */
public final class Config {
    /** Chance (0..1) that a newly generated village gets dirty houses. */
    public double villageChance = 0.6;
    /** Minimum and maximum number of dirty houses per selected village. */
    public int minHouses = 1;
    public int maxHouses = 2;
    /** Only touch villages that players have not visited yet (low inhabited time). */
    public boolean onlyNewVillages = true;
    /** The owner villager cannot die while the quest is active. */
    public boolean invulnerableOwner = true;
    /** Hero of the Village reward: level (1 = I) and duration in minutes. */
    public int heroLevel = 1;
    public int heroMinutes = 40;
    /** Extra loot rolls: one roll per this many dirt units (house size scaling). */
    public int unitsPerExtraRoll = 40;
    public int maxLootRolls = 4;
    /** Show the dirty-house ambience sounds (dripping, far rain). */
    public boolean ambientSounds = true;

    private static Config instance = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config get() {
        return instance;
    }

    static void load(Path dir) {
        Path file = dir.resolve(Spotless.MOD_ID + ".json");
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    Config c = GSON.fromJson(r, Config.class);
                    if (c != null) instance = c;
                }
            }
            instance.sanitize();
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            Spotless.LOG.warn("Could not read/write {}: {}", file, e.toString());
            instance.sanitize();
        }
    }

    private void sanitize() {
        villageChance = Math.max(0, Math.min(1, villageChance));
        minHouses = Math.max(0, minHouses);
        maxHouses = Math.max(minHouses, maxHouses);
        heroLevel = Math.max(0, Math.min(5, heroLevel));
        heroMinutes = Math.max(0, heroMinutes);
        unitsPerExtraRoll = Math.max(1, unitsPerExtraRoll);
        maxLootRolls = Math.max(1, maxLootRolls);
    }
}
