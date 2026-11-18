package dev.spotless.block;

import dev.spotless.clean.Cleaning;
import dev.spotless.item.ToolKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Dirty variant of a full vanilla block. STAGE 2 = filthy, 1 = stained. Turns back into the original. */
public class DirtyBlock extends Block implements Cleanable, DirtyVariant {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, 2);

    private final Block original;

    public DirtyBlock(Block original, Properties props) {
        super(props);
        this.original = original;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 2));
    }

    @Override
    public Block original() {
        return original;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public boolean canClean(BlockState state, ToolKind tool) {
        return true;
    }

    @Override
    public int units(BlockState state) {
        return state.getValue(STAGE);
    }

    @Override
    public BlockState cleanedOnce(BlockState state) {
        int s = state.getValue(STAGE);
        return s > 1 ? state.setValue(STAGE, s - 1) : original.defaultBlockState();
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
