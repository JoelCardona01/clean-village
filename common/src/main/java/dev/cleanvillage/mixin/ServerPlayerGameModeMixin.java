package dev.cleanvillage.mixin;

import dev.cleanvillage.block.Cleanable;
import dev.cleanvillage.clean.Cleaning;
import dev.cleanvillage.item.CleaningToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Left click with the sponge: "breaking" a dirt block only removes one layer of dirt. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void cleanvillage$scrubInsteadOfBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Cleanable)) return;
        if (player.getMainHandItem().getItem() instanceof CleaningToolItem && !CleaningToolItem.isDry(player.getMainHandItem())) {
            Cleaning.scrubAt(level, pos, state, player);
        } else {
            // never break dirt by hand: just resend the real block to the client
            level.sendBlockUpdated(pos, state, state, 3);
        }
        cir.setReturnValue(false);
    }
}
