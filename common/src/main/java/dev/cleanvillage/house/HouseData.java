package dev.cleanvillage.house;

import com.mojang.serialization.Codec;
import dev.cleanvillage.CleanVillage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Per-dimension persistent data: every dirty house plus which villages were already processed (T2). */
public final class HouseData extends SavedData {
    public static final Codec<HouseData> CODEC = CompoundTag.CODEC.xmap(HouseData::load, HouseData::save);
    public static final SavedDataType<HouseData> TYPE = new SavedDataType<>(CleanVillage.id("houses"), HouseData::new, CODEC, null);

    /** A village found but whose house chunks are not all loaded yet. */
    public static final class PendingVillage {
        public String key;
        public String type;
        public int wanted;
        public final List<BoundingBox> candidates = new ArrayList<>();
    }

    final Map<Integer, House> houses = new LinkedHashMap<>();
    final Set<String> processedVillages = new HashSet<>();
    final List<PendingVillage> pending = new ArrayList<>();
    int nextId = 1;

    public static HouseData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<House> houses() {
        return houses.values();
    }

    public House byId(int id) {
        return houses.get(id);
    }

    public House add(House h) {
        h.id = nextId++;
        houses.put(h.id, h);
        setDirty();
        return h;
    }

    public void remove(int id) {
        houses.remove(id);
        setDirty();
    }

    private static HouseData load(CompoundTag t) {
        HouseData d = new HouseData();
        d.nextId = t.getIntOr("nextId", 1);
        ListTag hs = t.getListOrEmpty("houses");
        for (int i = 0; i < hs.size(); i++) {
            try {
                House h = House.load(hs.getCompoundOrEmpty(i));
                d.houses.put(h.id, h);
                d.nextId = Math.max(d.nextId, h.id + 1);
            } catch (Exception e) {
                CleanVillage.LOG.warn("Skipping corrupt house entry: {}", e.toString());
            }
        }
        ListTag pv = t.getListOrEmpty("processed");
        for (int i = 0; i < pv.size(); i++) {
            if (pv.get(i) instanceof StringTag st) d.processedVillages.add(st.value());
        }
        ListTag pl = t.getListOrEmpty("pending");
        for (int i = 0; i < pl.size(); i++) {
            CompoundTag c = pl.getCompoundOrEmpty(i);
            PendingVillage p = new PendingVillage();
            p.key = c.getStringOr("key", "");
            p.type = c.getStringOr("type", "plains");
            p.wanted = c.getIntOr("wanted", 1);
            ListTag cs = c.getListOrEmpty("candidates");
            for (int j = 0; j < cs.size(); j++) {
                Tag e = cs.get(j);
                if (e instanceof IntArrayTag ia && ia.getAsIntArray().length == 6) {
                    int[] b = ia.getAsIntArray();
                    p.candidates.add(new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]));
                }
            }
            d.pending.add(p);
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("nextId", nextId);
        ListTag hs = new ListTag();
        for (House h : houses.values()) hs.add(h.save());
        t.put("houses", hs);
        ListTag pv = new ListTag();
        for (String s : processedVillages) pv.add(StringTag.valueOf(s));
        t.put("processed", pv);
        ListTag pl = new ListTag();
        for (PendingVillage p : pending) {
            CompoundTag c = new CompoundTag();
            c.putString("key", p.key);
            c.putString("type", p.type);
            c.putInt("wanted", p.wanted);
            ListTag cs = new ListTag();
            for (BoundingBox b : p.candidates) {
                cs.add(new IntArrayTag(new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}));
            }
            c.put("candidates", cs);
            pl.add(c);
        }
        t.put("pending", pl);
        return t;
    }
}
