package dev.cleanvillage.clean;

import dev.cleanvillage.block.Cleanable;
import dev.cleanvillage.house.HouseManager;
import dev.cleanvillage.item.CleaningToolItem;
import dev.cleanvillage.item.ToolKind;
import dev.cleanvillage.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared cleaning logic: which tool cleans what, feedback and progress accounting. */
public final class Cleaning {
    private Cleaning() {}

    /** True while the mod itself changes dirt blocks (so removal hooks don't count it as "lost"). */
    private static int suppress;

    public static void suppressLoss(Runnable r) {
        suppress++;
        try {
            r.run();
        } finally {
            suppress--;
        }
    }

    public static boolean canClean(BlockState state, ToolKind tool) {
        if (state.getBlock() instanceof Cleanable c) return c.canClean(state, tool);
        // The sponge also wipes away ordinary cobwebs (without dropping string).
        return state.is(Blocks.COBWEB);
    }

    public static int scrubTicks(BlockState state, ToolKind tool, Direction face) {
        if (state.getBlock() instanceof dev.cleanvillage.block.DirtyVariant) return tool.baseTicks + 1;
        return tool.baseTicks;
    }

    /** Left click: mining progress per tick on dirt (only with a wet sponge; dirt is unbreakable otherwise). */
    public static float destroyProgress(BlockState state, net.minecraft.world.entity.player.Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof CleaningToolItem) || CleaningToolItem.isDry(held)) return 0f;
        if (player.getAbilities().instabuild) return 1f;
        return 1f / (scrubTicks(state, ToolKind.SPONGE, Direction.UP) + 1);
    }

    /** Left-click scrub: same as a right-click scrub, aimed at the block centre. */
    public static void scrubAt(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        scrub(level, hit, state, player, player.getMainHandItem(), ToolKind.SPONGE, InteractionHand.MAIN_HAND);
    }

    /** Continuous feedback while scrubbing (bubbles + scrub sound). */
    public static void scrubEffects(ServerLevel level, BlockHitResult hit, ToolKind tool, int usedTicks) {
        Vec3 p = hit.getLocation();
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.08);
        if (usedTicks % 2 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, p.x + n.x, p.y + n.y, p.z + n.z, 2, 0.18, 0.18, 0.18, 0.02);
        }
        if (usedTicks % 5 == 0) {
            float pitch = 0.9f + level.getRandom().nextFloat() * 0.3f;
            level.playSound(null, BlockPos.containing(p), ModContent.SCRUB,
                    SoundSource.PLAYERS, 0.6f, pitch);
        }
    }

    /** Remove one unit of dirt at the hit position. */
    public static void scrub(ServerLevel level, BlockHitResult hit, BlockState state, ServerPlayer player, ItemStack stack,
                             ToolKind tool, InteractionHand hand) {
        BlockPos pos = hit.getBlockPos();
        BlockState next;
        int unitsAfter;
        boolean counts;
        if (state.getBlock() instanceof Cleanable c) {
            next = c.cleanedOnce(state);
            unitsAfter = next.getBlock() instanceof Cleanable c2 ? c2.units(next) : 0;
            counts = true;
        } else {
            next = Blocks.AIR.defaultBlockState();
            unitsAfter = 0;
            counts = false;
        }
        final BlockState target = next;
        suppressLoss(() -> level.setBlock(pos, target, Block.UPDATE_ALL));

        Vec3 p = hit.getLocation();
        level.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, 6, 0.25, 0.25, 0.25, 0.04);
        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 4, 0.2, 0.2, 0.2, 0.0);
        }
        level.playSound(null, pos, ModContent.BUBBLES, SoundSource.PLAYERS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        if (unitsAfter == 0) {
            // Fully clean: brief shine on the block.
            level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.5);
            level.playSound(null, pos, ModContent.SPARKLE, SoundSource.PLAYERS, 0.5f, 1.4f + level.getRandom().nextFloat() * 0.4f);
        }

        // Tool wear. The sponge never breaks: it dries out and must be recharged.
        if (!player.getAbilities().instabuild) {
            stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 1));
            if (CleaningToolItem.isDry(stack)) {
                level.playSound(null, player.blockPosition(), ModContent.SPONGE_DRY, SoundSource.PLAYERS, 0.7f, 1.0f);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.cleanvillage.sponge_dry"));
            }
        }
        if (counts) HouseManager.onCleaned(level, pos, player, 1);
    }

    /** A dirt block disappeared without being cleaned (support removed, explosion, command...). */
    public static void onDirtLost(ServerLevel level, BlockPos pos, int units) {
        if (suppress > 0) return;
        HouseManager.onLost(level, pos, units);
    }
}
