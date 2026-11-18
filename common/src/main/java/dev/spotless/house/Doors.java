package dev.spotless.house;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The owner opens/closes the house doors while walking in or out (its brain is paused). */
final class Doors {
    private Doors() {}

    static void open(ServerLevel level, BoundingBox box, boolean open) {
        BoundingBox b = box.inflatedBy(1);
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock door && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && DoorBlock.isWoodenDoor(s) && door.isOpen(s) != open) {
                door.setOpen(null, level, s, p.immutable(), open);
            }
        }
    }
}
