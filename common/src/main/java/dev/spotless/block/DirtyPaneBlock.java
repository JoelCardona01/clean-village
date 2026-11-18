package dev.spotless.block;

import dev.spotless.clean.Cleaning;
import dev.spotless.item.ToolKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Fogged-up, grimy glass pane. One wipe makes it the original pane again. */
public class DirtyPaneBlock extends IronBarsBlock implements Cleanable, DirtyVariant {

    private final Block original;

    public DirtyPaneBlock(Block original, Properties props) {
        super(props);
        this.original = original;
    }

    @Override
    public Block original() {
        return original;
    }

    @Override
    public boolean canClean(BlockState state, ToolKind tool) {
        return true;
    }

    @Override
    public int units(BlockState state) {
        return 1;
    }

    @Override
    public BlockState cleanedOnce(BlockState state) {
        return copyProps(state, original.defaultBlockState());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState copyProps(BlockState from, BlockState to) {
        for (Property p : from.getProperties()) {
            if (to.hasProperty(p)) to = to.setValue(p, from.getValue(p));
        }
        return to;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Cleaning.onDirtLost(level, pos, units(state));
    }

    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return dev.spotless.clean.Cleaning.destroyProgress(state, player);
    }
}
