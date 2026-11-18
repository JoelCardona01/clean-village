package dev.cleanvillage.block;

import dev.cleanvillage.clean.Cleaning;
import dev.cleanvillage.item.ToolKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Green mold patches in corners. Only the scrub brush removes it. */
public class MoldBlock extends MultifaceBlock implements Cleanable {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 2);

    public MoldBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL);
    }

    @Override
    public boolean canClean(BlockState state, ToolKind tool) {
        return true;
    }

    @Override
    public int units(BlockState state) {
        return state.getValue(LEVEL);
    }

    @Override
    public BlockState cleanedOnce(BlockState state) {
        int lvl = state.getValue(LEVEL);
        if (lvl > 1) return state.setValue(LEVEL, lvl - 1);
        return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }


    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Cleaning.onDirtLost(level, pos, units(state));
    }

    @Override
    protected float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player,
                                       net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return dev.cleanvillage.clean.Cleaning.destroyProgress(state, player);
    }
}
