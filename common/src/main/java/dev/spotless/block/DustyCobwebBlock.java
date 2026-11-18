package dev.spotless.block;

import dev.spotless.clean.Cleaning;
import dev.spotless.item.ToolKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Old dusty cobweb. The feather duster sweeps it away. */
public class DustyCobwebBlock extends WebBlock implements Cleanable {

    public DustyCobwebBlock(Properties props) {
        super(props);
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
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Cleaning.onDirtLost(level, pos, 1);
    }

    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return dev.spotless.clean.Cleaning.destroyProgress(state, player);
    }
}
