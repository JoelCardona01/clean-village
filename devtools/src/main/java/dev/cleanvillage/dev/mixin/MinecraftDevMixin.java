package dev.cleanvillage.dev.mixin;

import dev.cleanvillage.dev.DevScript;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftDevMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void cleanvillage$devScript(CallbackInfo ci) {
        DevScript.tick((Minecraft) (Object) this);
    }
}
