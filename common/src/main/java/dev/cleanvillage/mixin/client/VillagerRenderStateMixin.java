package dev.cleanvillage.mixin.client;

import dev.cleanvillage.client.SittingState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerRenderState.class)
public abstract class VillagerRenderStateMixin implements SittingState {
    @Unique
    private boolean cleanvillage$sitting;

    @Override
    public boolean cleanvillage$isSitting() {
        return cleanvillage$sitting;
    }

    @Override
    public void cleanvillage$setSitting(boolean sitting) {
        cleanvillage$sitting = sitting;
    }
}
