package dev.spotless.block;

import dev.spotless.clean.Cleaning;
import dev.spotless.item.ToolKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Dirty log / wood: keeps its axis when cleaned. */
public class DirtyPillarBlock extends RotatedPillarBlock implements Cleanable, DirtyVariant {

    private final Block original;

    public DirtyPillarBlock(Block original, Properties props) {
        super(props);
        this.original = original;
        registerDefaultState(defaultBlockState().setValue(DirtyBlock.STAGE, 2));
    }

    @Override
    public Block original() {
        return original;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DirtyBlock.STAGE);
    }

    @Override
    public boolean canClean(BlockState state, ToolKind tool) {
        return true;
    }

    @Override
    public int units(BlockState state) {
        return state.getValue(DirtyBlock.STAGE);
    }

    @Override
    public BlockState cleanedOnce(BlockState state) {
        int s = state.getValue(DirtyBlock.STAGE);
        if (s > 1) return state.setValue(DirtyBlock.STAGE, s - 1);
        BlockState o = original.defaultBlockState();
        return o.hasProperty(AXIS) ? o.setValue(AXIS, state.getValue(AXIS)) : o;
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
