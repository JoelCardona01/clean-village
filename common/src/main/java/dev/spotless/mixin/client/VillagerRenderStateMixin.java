package dev.spotless.mixin.client;

import dev.spotless.client.SittingState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerRenderState.class)
public abstract class VillagerRenderStateMixin implements SittingState {
    @Unique
    private boolean spotless$sitting;

    @Override
    public boolean spotless$isSitting() {
        return spotless$sitting;
    }

    @Override
    public void spotless$setSitting(boolean sitting) {
        spotless$sitting = sitting;
    }
}
