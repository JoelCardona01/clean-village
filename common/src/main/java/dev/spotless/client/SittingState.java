package dev.spotless.client;

/** Added to VillagerRenderState: is this villager sitting (riding the invisible seat)? */
public interface SittingState {
    boolean spotless$isSitting();

    void spotless$setSitting(boolean sitting);
}
