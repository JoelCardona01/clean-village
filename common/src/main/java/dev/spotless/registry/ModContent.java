package dev.spotless.registry;

import dev.spotless.Spotless;
import dev.spotless.block.*;
import dev.spotless.item.CleaningToolItem;
import dev.spotless.item.ToolKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * All blocks, items and sounds. Loader code calls the register* methods, in this order,
 * passing a sink that performs the actual registry write.
 */
public final class ModContent {
    private ModContent() {}

    /** Unbreakable by hand: dirt must be cleaned with tools (S6). */
    private static final float UNBREAKABLE = -1.0f;
    private static final float BLAST_PROOF = 3600000.0f;

    // ---- blocks
    public static GrimeBlock GRIME;
    public static MoldBlock MOLD;
    public static DustyCobwebBlock DUSTY_COBWEB;
    public static FamilyPortraitBlock FAMILY_PORTRAIT;
    /** original vanilla block -> dirty variant */
    public static final Map<Block, Block> DIRTY_OF = new LinkedHashMap<>();
    /** registry path of every dirty variant (for data generation and tests) */
    public static final Map<String, Block> DIRTY_BY_NAME = new LinkedHashMap<>();

    // ---- items
    public static CleaningToolItem SOAPY_SPONGE;
    public static BlockItem FAMILY_PORTRAIT_ITEM;
    public static final List<Item> TAB_ITEMS = new ArrayList<>();
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

    // ---- sounds
    public static SoundEvent SCRUB;
    public static SoundEvent BUBBLES;
    public static SoundEvent SPARKLE;
    public static SoundEvent DRIP;
    public static SoundEvent SPONGE_DRY;

    /** Full vanilla blocks used by village houses that get a dirty twin. */
    private static final String[] CUBES = {
            "cobblestone", "mossy_cobblestone", "stone_bricks", "oak_planks", "spruce_planks", "birch_planks",
            "acacia_planks", "jungle_planks", "dark_oak_planks", "smooth_sandstone", "cut_sandstone", "sandstone",
            "terracotta", "white_terracotta", "yellow_terracotta", "packed_ice", "snow_block", "bricks"
    };
    private static final String[] PILLARS = {
            "oak_log", "spruce_log", "birch_log", "acacia_log", "jungle_log", "dark_oak_log",
            "stripped_oak_log", "stripped_spruce_log", "stripped_acacia_log", "stripped_birch_log",
            "oak_wood", "spruce_wood", "acacia_wood", "stripped_oak_wood", "stripped_spruce_wood", "stripped_acacia_wood"
    };

    public static List<String> cubeNames() { return List.of(CUBES); }
    public static List<String> pillarNames() { return List.of(PILLARS); }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Spotless.id(path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Spotless.id(path));
    }

    private static <T extends Block> T block(BiConsumer<Identifier, Block> sink, String path,
                                             BlockBehaviour.Properties props, Function<BlockBehaviour.Properties, T> ctor) {
        T b = ctor.apply(props.setId(blockKey(path)));
        sink.accept(Spotless.id(path), b);
        return b;
    }

    private static Block vanilla(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path));
    }

    public static void registerBlocks(BiConsumer<Identifier, Block> sink) {
        GRIME = block(sink, "grime", BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).noCollision()
                .strength(UNBREAKABLE, BLAST_PROOF).sound(SoundType.MUD).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), GrimeBlock::new);
        MOLD = block(sink, "mold", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollision()
                .strength(UNBREAKABLE, BLAST_PROOF).sound(SoundType.MOSS).noOcclusion().pushReaction(PushReaction.IMMOVEABLE), MoldBlock::new);
        DUSTY_COBWEB = block(sink, "dusty_cobweb", BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)
                .strength(UNBREAKABLE, BLAST_PROOF).pushReaction(PushReaction.IMMOVEABLE), DustyCobwebBlock::new);
        FAMILY_PORTRAIT = block(sink, "family_portrait", BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(0.5f).sound(SoundType.WOOD).noCollision().noOcclusion(), FamilyPortraitBlock::new);

        for (String name : CUBES) {
            Block original = vanilla(name);
            Block dirty = block(sink, "dirty_" + name, BlockBehaviour.Properties.ofFullCopy(original)
                    .strength(UNBREAKABLE, BLAST_PROOF).pushReaction(PushReaction.IMMOVEABLE), p -> new DirtyBlock(original, p));
            DIRTY_OF.put(original, dirty);
            DIRTY_BY_NAME.put("dirty_" + name, dirty);
        }
        for (String name : PILLARS) {
            Block original = vanilla(name);
            Block dirty = block(sink, "dirty_" + name, BlockBehaviour.Properties.ofFullCopy(original)
                    .strength(UNBREAKABLE, BLAST_PROOF).pushReaction(PushReaction.IMMOVEABLE), p -> new DirtyPillarBlock(original, p));
            DIRTY_OF.put(original, dirty);
            DIRTY_BY_NAME.put("dirty_" + name, dirty);
        }
        Block pane = block(sink, "dirty_glass_pane", BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE)
                .strength(UNBREAKABLE, BLAST_PROOF).pushReaction(PushReaction.IMMOVEABLE), p -> new DirtyPaneBlock(Blocks.GLASS_PANE, p));
        DIRTY_OF.put(Blocks.GLASS_PANE, pane);
        DIRTY_BY_NAME.put("dirty_glass_pane", pane);
    }

    private static <T extends Item> T item(BiConsumer<Identifier, Item> sink, String path, Item.Properties props,
                                           Function<Item.Properties, T> ctor) {
        T it = ctor.apply(props.setId(itemKey(path)));
        sink.accept(Spotless.id(path), it);
        TAB_ITEMS.add(it);
        return it;
    }

    public static void registerItems(BiConsumer<Identifier, Item> sink) {
        SOAPY_SPONGE = item(sink, "soapy_sponge", new Item.Properties().durability(128)
                        .component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                                net.minecraft.network.chat.Component.translatable("item.spotless.soapy_sponge.hint1")
                                        .withStyle(s -> s.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY)),
                                net.minecraft.network.chat.Component.translatable("item.spotless.soapy_sponge.hint2")
                                        .withStyle(s -> s.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY))))),
                p -> new CleaningToolItem(ToolKind.SPONGE, p));
        FAMILY_PORTRAIT_ITEM = item(sink, "family_portrait", new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .useBlockDescriptionPrefix(), p -> new BlockItem(FAMILY_PORTRAIT, p));
    }

    private static SoundEvent sound(BiConsumer<Identifier, SoundEvent> sink, String path) {
        SoundEvent e = SoundEvent.createVariableRangeEvent(Spotless.id(path));
        sink.accept(Spotless.id(path), e);
        return e;
    }

    public static void registerSounds(BiConsumer<Identifier, SoundEvent> sink) {
        SCRUB = sound(sink, "scrub");
        BUBBLES = sound(sink, "bubbles");
        SPARKLE = sound(sink, "sparkle");
        DRIP = sound(sink, "drip");
        SPONGE_DRY = sound(sink, "sponge_dry");
    }
}
