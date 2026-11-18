package dev.cleanvillage.item;

/** The cleaning tool. Only the soapy sponge exists: it cleans every kind of dirt. */
public enum ToolKind {
    SPONGE(5);

    public final int baseTicks;

    ToolKind(int baseTicks) {
        this.baseTicks = baseTicks;
    }
}
