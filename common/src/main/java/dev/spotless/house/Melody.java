package dev.spotless.house;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Happy celebration jingle (~6.5 s) played with note block sounds (C3).
 * Each note: {tick, instrument, semitone (0..24, 12 = F#4 like note blocks)}.
 * Instruments: 0 harp, 1 bell, 2 flute, 3 chime, 4 bass.
 */
public final class Melody {
    private Melody() {}

    // Key of G major on the note-block scale (0 = F#3). G=1, A=3, B=5, C=6, D=8, E=10, F#=12, G'=13, A'=15, B'=17, D''=20
    private static final int[][] NOTES = {
            // bar 1: rising arpeggio
            {0, 0, 1}, {0, 4, 1}, {4, 0, 5}, {8, 0, 8}, {12, 2, 13}, {12, 4, 8},
            // bar 2: bouncy answer
            {18, 0, 12}, {22, 0, 13}, {26, 0, 15}, {30, 2, 13}, {30, 4, 6},
            // bar 3
            {36, 0, 10}, {40, 0, 12}, {44, 0, 13}, {48, 2, 17}, {48, 4, 1},
            // bar 4: little run down
            {54, 0, 15}, {57, 0, 13}, {60, 0, 12}, {63, 0, 10}, {66, 2, 8}, {66, 4, 3},
            // bar 5: up again with bells
            {72, 1, 8}, {76, 1, 13}, {80, 1, 17}, {84, 2, 20}, {84, 4, 8},
            // final chord + chimes
            {94, 1, 13}, {94, 1, 17}, {94, 0, 20}, {94, 4, 1}, {94, 3, 13},
            {104, 3, 20}, {112, 3, 24}
    };

    public static final int LENGTH = 120;

    private static Holder<SoundEvent> instrument(int i) {
        return switch (i) {
            case 1 -> SoundEvents.NOTE_BLOCK_BELL;
            case 2 -> SoundEvents.NOTE_BLOCK_FLUTE;
            case 3 -> SoundEvents.NOTE_BLOCK_CHIME;
            case 4 -> SoundEvents.NOTE_BLOCK_BASS;
            default -> SoundEvents.NOTE_BLOCK_HARP;
        };
    }

    /** Play every note scheduled for this tick of the melody (audible to nearby players, C3). */
    public static void tick(ServerLevel level, Vec3 at, int t) {
        for (int[] n : NOTES) {
            if (n[0] == t) {
                float pitch = (float) Math.pow(2.0, (n[2] - 12) / 12.0);
                float vol = n[1] == 4 ? 0.9f : n[1] == 3 ? 0.7f : 1.4f;
                level.playSound(null, at.x, at.y, at.z, instrument(n[1]), SoundSource.RECORDS, vol, pitch);
            }
        }
    }
}
