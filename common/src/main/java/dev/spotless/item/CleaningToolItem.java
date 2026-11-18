package dev.spotless.item;

import dev.spotless.clean.Cleaning;
import dev.spotless.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The soapy sponge: the only cleaning tool. Hold right click (or left click) on any dirt to scrub it.
 * Every scrub uses a bit of soap; when the sponge is dry, right click on water to soak it again.
 */
public class CleaningToolItem extends Item {
    private final ToolKind kind;

    public CleaningToolItem(ToolKind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    public ToolKind kind() {
        return kind;
    }

    public static boolean isDry(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }

    /** Right click on water: soak the sponge again. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            if (stack.getDamageValue() == 0) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                BlockPos pos = hit.getBlockPos();
                stack.setDamageValue(0);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8f, 1.4f);
                level.playSound(null, pos, ModContent.BUBBLES, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.3, 0.1, 0.3, 0.02);
                    sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.1);
                }
                if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.translatable("message.spotless.sponge_recharged"));
            }
            return InteractionResult.SUCCESS;
        }
        if (isDry(stack) && !level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.spotless.sponge_dry"));
        }
        return InteractionResult.PASS;
    }

    /** Right click on dirt: start scrubbing (continues while the button is held). */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockState state = level.getBlockState(ctx.getClickedPos());
        if (!Cleaning.canClean(state, kind)) return InteractionResult.PASS;
        if (isDry(ctx.getItemInHand())) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.spotless.sponge_dry"));
                level.playSound(null, player.blockPosition(), ModContent.SPONGE_DRY, SoundSource.PLAYERS, 0.7f, 1.0f);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player) || isDry(stack)) {
            entity.releaseUsingItem();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, EntitySelector.CAN_BE_PICKED, player.blockInteractionRange());
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            entity.releaseUsingItem();
            return;
        }
        BlockState state = level.getBlockState(bhr.getBlockPos());
        if (!Cleaning.canClean(state, kind)) {
            entity.releaseUsingItem();
            return;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            int used = getUseDuration(stack, entity) - remaining;
            int need = Math.max(1, Cleaning.scrubTicks(state, kind, bhr.getDirection()));
            Cleaning.scrubEffects(server, bhr, kind, used);
            if (used % need == need - 1) {
                Cleaning.scrub(server, bhr, state, sp, stack, kind, entity.getUsedItemHand());
            }
        }
    }
}
