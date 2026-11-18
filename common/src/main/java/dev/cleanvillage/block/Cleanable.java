package dev.cleanvillage.block;

import dev.cleanvillage.item.ToolKind;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A block that represents dirt and can be cleaned one "unit" at a time. */
public interface Cleanable {
    /** Can this tool clean this state at all? */
    boolean canClean(BlockState state, ToolKind tool);

    /** Dirt units left in this state (each scrub removes one). */
    int units(BlockState state);

    /** State after removing one unit of dirt. */
    BlockState cleanedOnce(BlockState state);

    /** Ticks needed per scrub for the given tool and hit face. */
    default int scrubTicks(BlockState state, ToolKind tool, Direction face) {
        return tool.baseTicks;
    }
}
