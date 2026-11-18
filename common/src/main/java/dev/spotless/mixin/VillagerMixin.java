package dev.spotless.mixin;

import dev.spotless.house.House;
import dev.spotless.house.HouseManager;
import dev.spotless.house.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    /** Per-player cooldown so holding right click does not spam the chat. */
    @org.spongepowered.asm.mixin.Unique
    private static final java.util.Map<java.util.UUID, Long> LAST_TALK = new java.util.HashMap<>();

    /** A2: the sad owner does not trade, it tells you how it feels. */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void spotless$talk(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Villager self = (Villager) (Object) this;
        if (!self.entityTags().contains(HouseManager.OWNER_TAG)) return;
        House h = HouseManager.activeQuestOf(self);
        if (h == null) return;
        long now = self.level().getGameTime();
        Long last = LAST_TALK.get(player.getUUID());
        if (hand == InteractionHand.MAIN_HAND && (last == null || now - last > 40)) {
            LAST_TALK.put(player.getUUID(), now);
            Component line = h.phase == House.Phase.SAD ? Texts.sadLine(self.getRandom(), h.ownerName)
                    : Texts.waitingLine(self.getRandom(), h.ownerName);
            player.sendSystemMessage(Component.translatable("chat.spotless.villager", h.ownerName, line));
            self.playSound(h.phase == House.Phase.SAD ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_AMBIENT, 1.0f,
                    h.phase == House.Phase.SAD ? 0.8f : 1.1f);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    /** A4: no witch conversion during the quest. */
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void spotless$noWitch(ServerLevel level, LightningBolt bolt, CallbackInfo ci) {
        if (HouseManager.activeQuestOf((Villager) (Object) this) != null) ci.cancel();
    }

    /** While scripted (walking to the door, celebrating) the brain is paused; the mod drives navigation. */
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void spotless$scripted(ServerLevel level, CallbackInfo ci) {
        if (((Villager) (Object) this).entityTags().contains(HouseManager.SCRIPTED_TAG)) ci.cancel();
    }
}
