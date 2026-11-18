package dev.spotless.house;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

/** Persistent state of one dirty house. */
public final class House {
    public enum Phase { SAD, CLEANING, CELEBRATING, DONE }

    public int id;
    public BoundingBox box;
    public UUID owner;
    public String ownerName = "Marta";
    public int story;
    public Phase phase = Phase.SAD;
    public int total;
    public int remaining;
    public final Set<UUID> helpers = new LinkedHashSet<>();
    public BlockPos bed = BlockPos.ZERO;
    public BlockPos seat = BlockPos.ZERO;
    public float seatYaw;
    public BlockPos waitPos = BlockPos.ZERO;
    public BlockPos insidePos = BlockPos.ZERO;
    public BlockPos chest = BlockPos.ZERO;
    public UUID seatEntity;
    /** Light sources removed to make the house gloomy; restored at the end. pos -> state string. */
    public final Map<Long, String> removedLights = new LinkedHashMap<>();
    public boolean peeked;
    public String villageType = "plains";
    public long createdTick;

    // runtime only (not saved)
    transient int timer;
    transient int step;
    transient int walkTicks;
    transient int jumps;

    public float progress() {
        if (total <= 0) return 1f;
        return Math.max(0f, Math.min(1f, (total - remaining) / (float) total));
    }

    public int percent() {
        return (int) Math.floor(progress() * 100f + 1e-4f);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putIntArray("box", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
        if (owner != null) t.putString("owner", owner.toString());
        t.putString("ownerName", ownerName);
        t.putInt("story", story);
        t.putString("phase", phase.name());
        t.putInt("total", total);
        t.putInt("remaining", remaining);
        ListTag hl = new ListTag();
        for (UUID u : helpers) hl.add(StringTag.valueOf(u.toString()));
        t.put("helpers", hl);
        t.putLong("bed", bed.asLong());
        t.putLong("seat", seat.asLong());
        t.putFloat("seatYaw", seatYaw);
        t.putLong("waitPos", waitPos.asLong());
        t.putLong("insidePos", insidePos.asLong());
        t.putLong("chest", chest.asLong());
        if (seatEntity != null) t.putString("seatEntity", seatEntity.toString());
        ListTag lights = new ListTag();
        for (Map.Entry<Long, String> e : removedLights.entrySet()) {
            CompoundTag l = new CompoundTag();
            l.putLong("pos", e.getKey());
            l.putString("state", e.getValue());
            lights.add(l);
        }
        t.put("lights", lights);
        t.putBoolean("peeked", peeked);
        t.putString("villageType", villageType);
        t.putLong("created", createdTick);
        return t;
    }

    public static House load(CompoundTag t) {
        House h = new House();
        h.id = t.getIntOr("id", 0);
        int[] b = t.getIntArray("box").orElse(new int[]{0, 0, 0, 0, 0, 0});
        h.box = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        String o = t.getStringOr("owner", "");
        h.owner = o.isEmpty() ? null : UUID.fromString(o);
        h.ownerName = t.getStringOr("ownerName", "Marta");
        h.story = t.getIntOr("story", 0);
        try {
            h.phase = Phase.valueOf(t.getStringOr("phase", "SAD"));
        } catch (IllegalArgumentException e) {
            h.phase = Phase.SAD;
        }
        h.total = t.getIntOr("total", 0);
        h.remaining = t.getIntOr("remaining", 0);
        ListTag hl = t.getListOrEmpty("helpers");
        for (int i = 0; i < hl.size(); i++) {
            Tag e = hl.get(i);
            if (e instanceof StringTag st) {
                try {
                    h.helpers.add(UUID.fromString(st.value()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        h.bed = BlockPos.of(t.getLongOr("bed", 0));
        h.seat = BlockPos.of(t.getLongOr("seat", 0));
        h.seatYaw = t.getFloatOr("seatYaw", 0);
        h.waitPos = BlockPos.of(t.getLongOr("waitPos", 0));
        h.insidePos = BlockPos.of(t.getLongOr("insidePos", 0));
        h.chest = BlockPos.of(t.getLongOr("chest", 0));
        String se = t.getStringOr("seatEntity", "");
        h.seatEntity = se.isEmpty() ? null : UUID.fromString(se);
        ListTag lights = t.getListOrEmpty("lights");
        for (int i = 0; i < lights.size(); i++) {
            CompoundTag l = lights.getCompoundOrEmpty(i);
            h.removedLights.put(l.getLongOr("pos", 0), l.getStringOr("state", ""));
        }
        h.peeked = t.getBooleanOr("peeked", false);
        h.villageType = t.getStringOr("villageType", "plains");
        h.createdTick = t.getLongOr("created", 0);
        return h;
    }

    /** Unused helper kept for symmetry with LongTag import (avoids warnings on some compilers). */
    static Tag longTag(long v) {
        return LongTag.valueOf(v);
    }
}
