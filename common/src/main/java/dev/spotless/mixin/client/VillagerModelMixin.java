package dev.spotless.mixin.client;

import dev.spotless.client.SittingState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sitting pose for the sad owner: legs bent forward, head hanging low, a slow sob (A2). */
@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
    @Shadow @Final private ModelPart head;
    @Shadow @Final private ModelPart rightLeg;
    @Shadow @Final private ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V", at = @At("TAIL"))
    private void spotless$sit(VillagerRenderState state, CallbackInfo ci) {
        if (!((SittingState) state).spotless$isSitting()) return;
        rightLeg.xRot = -1.4137167f;
        rightLeg.yRot = 0.31415927f;
        rightLeg.zRot = 0.07853982f;
        leftLeg.xRot = -1.4137167f;
        leftLeg.yRot = -0.31415927f;
        leftLeg.zRot = -0.07853982f;
        head.xRot = 0.62f + 0.05f * Mth.sin(state.ageInTicks * 0.08f);
        head.zRot = 0.06f * Mth.sin(state.ageInTicks * 0.05f);
    }
}
