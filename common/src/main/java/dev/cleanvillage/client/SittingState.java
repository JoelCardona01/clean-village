package dev.cleanvillage.client;

/** Added to VillagerRenderState: is this villager sitting (riding the invisible seat)? */
public interface SittingState {
    boolean cleanvillage$isSitting();

    void cleanvillage$setSitting(boolean sitting);
}
