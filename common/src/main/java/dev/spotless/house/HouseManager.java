package dev.spotless.house;

import dev.spotless.Spotless;
import dev.spotless.Config;
import dev.spotless.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Finds villages, creates dirty houses and runs every house's state machine. */
public final class HouseManager {
    private HouseManager() {}

    public static final String OWNER_TAG = "spotless.owner";
    public static final String SCRIPTED_TAG = "spotless.scripted";
    public static final String SEAT_TAG = "spotless.seat";
    /** Vertical offset of the invisible seat relative to the bed block (villager legs are 0.75 tall). */
    public static final double SEAT_Y = -0.12;
    public static final double SEAT_OUT = 0.32;

    private static final Map<String, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<String, Integer> MISSING = new HashMap<>();
    private static long tickCount;

    // ------------------------------------------------------------------ ticking

    public static void tick(MinecraftServer server) {
        tickCount++;
        for (ServerLevel level : server.getAllLevels()) {
            HouseData data = HouseData.get(level);
            if (tickCount % 40 == 0 && !level.players().isEmpty()) {
                try {
                    scanVillages(level, data);
                } catch (Exception e) {
                    Spotless.LOG.error("Village scan failed", e);
                }
            }
            for (House h : new ArrayList<>(data.houses())) {
                try {
                    tickHouse(level, data, h);
                } catch (Exception e) {
                    Spotless.LOG.error("Error ticking house {}", h.id, e);
                }
            }
        }
    }

    private static String barKey(ServerLevel level, House h) {
        return level.dimension().identifier() + "#" + h.id;
    }

    // ------------------------------------------------------------------ village discovery (G1-G4, T3)

    private static void scanVillages(ServerLevel level, HouseData data) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Set<Long> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            int pcx = player.chunkPosition().x(), pcz = player.chunkPosition().z();
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
                    if (!seen.add(key)) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (Map.Entry<Structure, StructureStart> e : chunk.getAllStarts().entrySet()) {
                        StructureStart start = e.getValue();
                        if (start == null || !start.isValid()) continue;
                        Holder<Structure> holder = registry.wrapAsHolder(e.getKey());
                        if (!holder.is(StructureTags.VILLAGE)) continue;
                        String id = registry.getKey(e.getKey()) == null ? "village" : registry.getKey(e.getKey()).toString();
                        String vkey = id + "@" + cx + "," + cz;
                        if (data.processedVillages.contains(vkey)) continue;
                        data.processedVillages.add(vkey);
                        data.setDirty();
                        considerVillage(level, data, chunk, start, id, vkey);
                    }
                }
            }
        }
        // Apply pending villages whose house chunks are all present.
        for (Iterator<HouseData.PendingVillage> it = data.pending.iterator(); it.hasNext(); ) {
            HouseData.PendingVillage p = it.next();
            boolean ready = true;
            for (BoundingBox b : p.candidates) {
                if (!boxLoaded(level, b)) {
                    ready = false;
                    break;
                }
            }
            if (!ready) continue;
            it.remove();
            data.setDirty();
            RandomSource rnd = RandomSource.create(level.getSeed() ^ p.key.hashCode());
            int made = 0;
            for (BoundingBox b : p.candidates) {
                if (made >= p.wanted) break;
                if (overlapsHouse(data, b)) continue;
                if (createHouse(level, data, b, rnd, p.type) != null) made++;
            }
            Spotless.LOG.info("Village {}: {} dirty house(s) created", p.key, made);
        }
    }

    private static void considerVillage(ServerLevel level, HouseData data, LevelChunk chunk, StructureStart start, String id, String vkey) {
        Config cfg = Config.get();
        if (cfg.onlyNewVillages && chunk.getInhabitedTime() > 20 * 60 * 3) return;
        RandomSource rnd = RandomSource.create(level.getSeed() ^ vkey.hashCode() * 31L);
        if (rnd.nextDouble() >= cfg.villageChance) return;
        int wanted = cfg.minHouses + (cfg.maxHouses > cfg.minHouses ? rnd.nextInt(cfg.maxHouses - cfg.minHouses + 1) : 0);
        if (wanted <= 0) return;
        List<BoundingBox> houses = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pe && pe.getElement().toString().contains("/houses/")) {
                houses.add(piece.getBoundingBox());
            }
        }
        if (houses.isEmpty()) return;
        Collections.shuffle(houses, new Random(rnd.nextLong()));
        HouseData.PendingVillage p = new HouseData.PendingVillage();
        p.key = vkey;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        p.type = path.startsWith("village_") ? path.substring(8) : "plains";
        p.wanted = wanted;
        // keep a few extra candidates: houses without a bed (farms, pens) are skipped when applying
        p.candidates.addAll(houses.subList(0, Math.min(houses.size(), 12)));
        data.pending.add(p);
        data.setDirty();
    }

    /** Debug: make up to two houses of the village whose start is near this position dirty, immediately. */
    public static int forceVillage(ServerLevel level, BlockPos near) {
        HouseData data = HouseData.get(level);
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        int made = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow((near.getX() >> 4) + dx, (near.getZ() >> 4) + dz);
                if (chunk == null) continue;
                for (Map.Entry<Structure, StructureStart> e : chunk.getAllStarts().entrySet()) {
                    if (!registry.wrapAsHolder(e.getKey()).is(StructureTags.VILLAGE) || !e.getValue().isValid()) continue;
                    String id = String.valueOf(registry.getKey(e.getKey()));
                    String vkey = id + "@" + chunk.getPos().x() + "," + chunk.getPos().z();
                    data.processedVillages.add(vkey);
                    RandomSource rnd = RandomSource.create(vkey.hashCode());
                    List<BoundingBox> houses = new ArrayList<>();
                    for (StructurePiece piece : e.getValue().getPieces()) {
                        if (piece instanceof PoolElementStructurePiece pe && pe.getElement().toString().contains("/houses/")) {
                            houses.add(piece.getBoundingBox());
                        }
                    }
                    String type = id.contains("village_") ? id.substring(id.indexOf("village_") + 8) : "plains";
                    for (BoundingBox b : houses) {
                        if (made >= 2) break;
                        if (overlapsHouse(data, b)) continue;
                        if (createHouse(level, data, b, rnd, type) != null) made++;
                    }
                }
            }
        }
        return made;
    }

    private static boolean boxLoaded(ServerLevel level, BoundingBox b) {
        for (int cx = (b.minX() - 1) >> 4; cx <= (b.maxX() + 1) >> 4; cx++) {
            for (int cz = (b.minZ() - 1) >> 4; cz <= (b.maxZ() + 1) >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
            }
        }
        return true;
    }

    private static boolean overlapsHouse(HouseData data, BoundingBox b) {
        for (House h : data.houses()) if (h.box.intersects(b)) return true;
        return false;
    }

    // ------------------------------------------------------------------ house creation

    /** Make the house inside this box dirty and spawn its sad owner. Returns null if the house is not suitable. */
    public static House createHouse(ServerLevel level, HouseData data, BoundingBox box, RandomSource rnd, String villageType) {
        if (!DirtGenerator.suitable(level, box)) return null;
        DirtGenerator.Result r = DirtGenerator.apply(level, box, rnd);
        if (r == null || r.units <= 0) return null;
        House h = new House();
        h.box = box;
        h.total = r.units;
        h.remaining = r.units;
        h.bed = r.bedFoot;
        h.seat = r.seat;
        h.seatYaw = r.seatYaw;
        h.waitPos = r.waitPos;
        h.insidePos = r.insidePos;
        h.chest = r.chest == null ? BlockPos.ZERO : r.chest;
        h.removedLights.putAll(r.lights);
        h.story = rnd.nextInt(Texts.STORIES);
        h.ownerName = Texts.randomName(rnd);
        h.villageType = villageType;
        h.createdTick = level.getGameTime();
        data.add(h);

        if (r.chest != null && level.getBlockEntity(r.chest) instanceof ChestBlockEntity chest) {
            chest.setItem(12, new ItemStack(ModContent.SOAPY_SPONGE));
            chest.setItem(13, Texts.storyBook(h.story, h.ownerName));
            chest.setItem(14, new ItemStack(ModContent.SOAPY_SPONGE));
            chest.setChanged();
        }
        spawnOwner(level, h);
        data.setDirty();
        Spotless.LOG.info("Dirty house #{} for {} at {} ({} dirt units)", h.id, h.ownerName, box.getCenter(), h.total);
        return h;
    }

    private static Villager spawnOwner(ServerLevel level, House h) {
        Villager v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
        if (v == null) return null;
        v.snapTo(h.seat.getX() + 0.5, h.seat.getY() + 0.6, h.seat.getZ() + 0.5, h.seatYaw, 40f);
        ResourceKey<VillagerType> type = VillagerType.byBiome(level.getBiome(h.bed));
        v.setVillagerData(v.getVillagerData().withType(level.registryAccess(), type));
        v.setCustomName(Component.literal(h.ownerName));
        v.setCustomNameVisible(true);
        v.setPersistenceRequired();
        v.addTag(OWNER_TAG);
        v.setPermanentlyInvulnerable(Config.get().invulnerableOwner);
        v.setNoAi(true);
        level.addFreshEntity(v);
        h.owner = v.getUUID();
        seat(level, h, v);
        return v;
    }

    /** Sit the owner on the edge of the bed riding an invisible display entity (A2). */
    private static void seat(ServerLevel level, House h, Villager v) {
        Entity seat = h.seatEntity == null ? null : level.getEntity(h.seatEntity);
        if (seat == null) {
            Display.BlockDisplay d = net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (d == null) return;
            Vec3 out = Vec3.atLowerCornerOf(net.minecraft.core.Direction.fromYRot(h.seatYaw).getUnitVec3i()).scale(SEAT_OUT);
            d.snapTo(h.seat.getX() + 0.5 + out.x, h.seat.getY() + SEAT_Y, h.seat.getZ() + 0.5 + out.z, h.seatYaw, 0);
            d.addTag(SEAT_TAG);
            level.addFreshEntity(d);
            seat = d;
            h.seatEntity = d.getUUID();
        }
        v.startRiding(seat, true, true);
        faceSeat(v, h);
    }

    private static void faceSeat(Villager v, House h) {
        v.setYRot(h.seatYaw);
        v.yBodyRot = h.seatYaw;
        v.setYHeadRot(h.seatYaw);
        v.setXRot(42f);
    }

    // ------------------------------------------------------------------ progress (S5, S6, T5)

    public static House houseAt(ServerLevel level, BlockPos pos) {
        for (House h : HouseData.get(level).houses()) {
            if (h.phase != House.Phase.DONE && h.box.inflatedBy(1).isInside(pos)) return h;
        }
        return null;
    }

    public static House houseOf(ServerLevel level, UUID villager) {
        for (House h : HouseData.get(level).houses()) {
            if (villager.equals(h.owner)) return h;
        }
        return null;
    }

    public static void onCleaned(ServerLevel level, BlockPos pos, ServerPlayer player, int units) {
        House h = houseAt(level, pos);
        if (h == null || h.phase == House.Phase.CELEBRATING) return;
        HouseData data = HouseData.get(level);
        if (player != null) h.helpers.add(player.getUUID());
        h.remaining = Math.max(0, h.remaining - units);
        if (h.phase == House.Phase.SAD) startCleaning(level, h);
        updateBar(level, h);
        data.setDirty();
        if (h.remaining == 0) startCelebration(level, h);
    }

    public static void onLost(ServerLevel level, BlockPos pos, int units) {
        House h = houseAt(level, pos);
        if (h == null || h.phase == House.Phase.CELEBRATING) return;
        h.remaining = Math.max(0, h.remaining - units);
        HouseData.get(level).setDirty();
        updateBar(level, h);
        if (h.remaining == 0 && h.phase == House.Phase.CLEANING) startCelebration(level, h);
    }

    private static void updateBar(ServerLevel level, House h) {
        ServerBossEvent bar = BARS.get(barKey(level, h));
        if (bar == null) return;
        bar.setProgress(h.progress());
        bar.setName(Component.translatable("bossbar.spotless.house", h.ownerName, h.percent()));
        bar.setColor(h.percent() >= 100 ? BossEvent.BossBarColor.GREEN
                : h.percent() >= 50 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
    }

    private static void tickBar(ServerLevel level, House h) {
        String key = barKey(level, h);
        boolean show = h.phase == House.Phase.CLEANING || (h.phase == House.Phase.CELEBRATING && h.step < 4);
        ServerBossEvent bar = BARS.get(key);
        if (!show) {
            if (bar != null) {
                bar.removeAllPlayers();
                BARS.remove(key);
            }
            return;
        }
        if (bar == null) {
            bar = new ServerBossEvent(UUID.nameUUIDFromBytes(key.getBytes()), Component.empty(),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            BARS.put(key, bar);
            updateBar(level, h);
        }
        BoundingBox area = h.box.inflatedBy(1);
        for (ServerPlayer p : level.players()) {
            boolean inside = area.isInside(p.blockPosition()) || area.isInside(p.blockPosition().above());
            if (inside && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!inside && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
    }

    // ------------------------------------------------------------------ state machine (A, C, R)

    private static void tickHouse(ServerLevel level, HouseData data, House h) {
        if (h.phase == House.Phase.DONE) return;
        if (!level.isLoaded(h.bed) || !level.isPositionEntityTicking(h.bed)) return;
        tickBar(level, h);
        Villager v = h.owner == null ? null : level.getEntity(h.owner) instanceof Villager vv ? vv : null;
        String key = barKey(level, h);
        if (v == null || !v.isAlive()) {
            // Owner missing (killed by /kill, or lost): after a grace period, a relative moves in.
            int miss = MISSING.merge(key, 1, Integer::sum);
            if (miss > 200 && (v == null || !v.isAlive())) {
                MISSING.remove(key);
                h.seatEntity = null;
                Villager nv = spawnOwner(level, h);
                if (nv != null && h.phase != House.Phase.SAD) {
                    nv.stopRiding();
                    nv.setNoAi(false);
                    nv.addTag(SCRIPTED_TAG);
                    nv.snapTo(Vec3.atBottomCenterOf(h.phase == House.Phase.CLEANING ? h.waitPos : h.insidePos));
                    discardSeat(level, h);
                }
                data.setDirty();
            }
            return;
        }
        MISSING.remove(key);
        switch (h.phase) {
            case SAD -> tickSad(level, h, v);
            case CLEANING -> tickWaiting(level, h, v);
            case CELEBRATING -> Celebration.tick(level, data, h, v);
            default -> {
            }
        }
    }

    private static void tickSad(ServerLevel level, House h, Villager v) {
        if (!v.isPassenger()) seat(level, h, v);
        if (!v.isNoAi()) v.setNoAi(true);
        faceSeat(v, h);
        RandomSource rnd = level.getRandom();
        h.timer++;
        if (h.timer % 35 == 0 && rnd.nextFloat() < 0.75f) {
            // tears
            Vec3 eye = v.getEyePosition();
            Vec3 fwd = Vec3.directionFromRotation(30f, h.seatYaw).scale(0.25);
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x).normalize().scale(0.13);
            Vec3 a = eye.add(fwd).add(right).add(0, -0.12, 0);
            Vec3 b = eye.add(fwd).subtract(right).add(0, -0.12, 0);
            level.sendParticles(ParticleTypes.FALLING_WATER, a.x, a.y, a.z, 1, 0.01, 0.01, 0.01, 0);
            level.sendParticles(ParticleTypes.FALLING_WATER, b.x, b.y, b.z, 1, 0.01, 0.01, 0.01, 0);
        }
        if (h.timer % 220 == 0 && level.getNearestPlayer(v, 8) != null) {
            level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.5f, 0.8f);
        }
        ambience(level, h, rnd);
    }

    /** Gloomy ambience while the house is dirty: dripping water and far rain (only heard inside). */
    private static void ambience(ServerLevel level, House h, RandomSource rnd) {
        if (!Config.get().ambientSounds || h.timer % 20 != 0) return;
        BoundingBox area = h.box.inflatedBy(1);
        boolean someoneInside = false;
        for (ServerPlayer p : level.players()) if (area.isInside(p.blockPosition())) someoneInside = true;
        if (!someoneInside) return;
        if (rnd.nextFloat() < 0.45f) {
            BlockPos p = new BlockPos(h.box.minX() + rnd.nextInt(h.box.getXSpan()), h.box.maxY() - 1,
                    h.box.minZ() + rnd.nextInt(h.box.getZSpan()));
            level.playSound(null, p, ModContent.DRIP, SoundSource.AMBIENT, 0.6f, 0.8f + rnd.nextFloat() * 0.4f);
        }
        if (h.timer % 160 == 0) {
            level.playSound(null, BlockPos.containing(Vec3.atCenterOf(h.box.getCenter())), SoundEvents.WEATHER_RAIN, SoundSource.AMBIENT, 0.12f, 0.6f);
        }
    }

    /** First dirt removed: the owner gets up and waits outside the door (A3). */
    private static void startCleaning(ServerLevel level, House h) {
        h.phase = House.Phase.CLEANING;
        h.step = 0;
        h.walkTicks = 0;
        Villager v = h.owner == null ? null : level.getEntity(h.owner) instanceof Villager vv ? vv : null;
        if (v != null) {
            v.stopRiding();
            discardSeat(level, h);
            standUp(level, h, v);
            v.setNoAi(false);
            v.addTag(SCRIPTED_TAG);
            v.setXRot(0);
            level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 1f, 1.1f);
            Doors.open(level, h.box, true);
        }
        for (ServerPlayer p : level.players()) {
            if (h.helpers.contains(p.getUUID())) {
                p.sendSystemMessage(Component.translatable("message.spotless.started", h.ownerName));
            }
        }
    }

    /** Put the owner on its feet in front of the bed (the seat is slightly sunk into the bed). */
    private static void standUp(ServerLevel level, House h, Villager v) {
        BlockPos front = h.seat.relative(net.minecraft.core.Direction.fromYRot(h.seatYaw));
        BlockPos to = DirtGenerator.standable(level, front) ? front : h.insidePos;
        v.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
    }

    private static void discardSeat(ServerLevel level, House h) {
        if (h.seatEntity != null) {
            Entity seat = level.getEntity(h.seatEntity);
            if (seat != null) seat.discard();
            h.seatEntity = null;
        }
    }

    private static void tickWaiting(ServerLevel level, House h, Villager v) {
        if (v.isPassenger()) v.stopRiding();
        if (v.isNoAi()) v.setNoAi(false);
        if (!v.entityTags().contains(SCRIPTED_TAG)) v.addTag(SCRIPTED_TAG);
        h.timer++;
        Vec3 target = Vec3.atBottomCenterOf(h.waitPos);
        double dist = v.position().distanceTo(target);
        if (h.step == 0) {
            h.walkTicks++;
            if (dist < 1.2) {
                v.getNavigation().stop();
                h.step = 1;
                Doors.open(level, h.box, false);
            } else if (h.walkTicks > 240) {
                Celebration.poofTeleport(level, v, target);
                h.step = 1;
                Doors.open(level, h.box, false);
            } else if (h.walkTicks % 20 == 1 || v.getNavigation().isDone()) {
                v.getNavigation().moveTo(target.x, target.y, target.z, 0.55);
            }
        } else {
            if (dist > 3.0) {
                h.step = 0;
                h.walkTicks = 0;
            }
            ServerPlayer near = (ServerPlayer) level.getNearestPlayer(v, 14);
            if (near != null) v.getLookControl().setLookAt(near, 30f, 30f);
            else v.getLookControl().setLookAt(Vec3.atCenterOf(h.insidePos));
        }
        if (!h.peeked && h.percent() >= 50) {
            h.peeked = true;
            // Peeks in, a little happier
            v.getLookControl().setLookAt(Vec3.atCenterOf(h.insidePos));
            level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1.2f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.8, v.getZ(), 8, 0.4, 0.3, 0.4, 0.02);
            BoundingBox area = h.box.inflatedBy(4);
            for (ServerPlayer p : level.players()) {
                if (area.isInside(p.blockPosition())) {
                    p.sendSystemMessage(Component.translatable("message.spotless.halfway", h.ownerName));
                }
            }
            HouseData.get(level).setDirty();
        }
        if (h.timer % 400 == 0 && level.getRandom().nextBoolean()) {
            level.playSound(null, v.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
    }

    public static void startCelebration(ServerLevel level, House h) {
        h.phase = House.Phase.CELEBRATING;
        h.step = 0;
        h.timer = 0;
        h.walkTicks = 0;
        h.jumps = 0;
        updateBar(level, h);
        HouseData.get(level).setDirty();
    }

    /** Debug helper: jump straight to a phase. */
    public static void forcePhase(ServerLevel level, House h, House.Phase phase) {
        switch (phase) {
            case SAD -> {
                h.phase = House.Phase.SAD;
                Villager v = h.owner == null ? null : level.getEntity(h.owner) instanceof Villager vv ? vv : null;
                if (v != null) {
                    v.removeTag(SCRIPTED_TAG);
                    v.snapTo(Vec3.atBottomCenterOf(h.seat).add(0, 0.6, 0));
                    seat(level, h, v);
                }
            }
            case CLEANING -> startCleaning(level, h);
            case CELEBRATING -> {
                if (h.phase == House.Phase.SAD) startCleaning(level, h);
                h.remaining = 0;
                startCelebration(level, h);
            }
            case DONE -> {
                h.phase = House.Phase.DONE;
                Villager v = h.owner == null ? null : level.getEntity(h.owner) instanceof Villager vv ? vv : null;
                if (v != null) Celebration.release(v);
                discardSeat(level, h);
                tickBar(level, h);
            }
        }
        HouseData.get(level).setDirty();
    }

    /** True if this villager is the owner of a house whose quest is still running. */
    public static House activeQuestOf(Villager v) {
        if (!(v.level() instanceof ServerLevel sl) || !v.entityTags().contains(OWNER_TAG)) return null;
        House h = houseOf(sl, v.getUUID());
        return h != null && h.phase != House.Phase.DONE ? h : null;
    }

    static void removeBar(ServerLevel level, House h) {
        ServerBossEvent bar = BARS.remove(barKey(level, h));
        if (bar != null) bar.removeAllPlayers();
    }
}
