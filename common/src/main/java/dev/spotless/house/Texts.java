package dev.spotless.house;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/** Villager names, the story book and the thank-you letter (texts live in the lang files, T7). */
public final class Texts {
    private Texts() {}

    public static final String[] NAMES = {
            "Marta", "Tom", "Lucía", "Pablo", "Elena", "Hugo", "Nora", "Bruno", "Clara", "Iván",
            "Sofía", "Leo", "Alba", "Teo", "Vera", "Dani", "Rosa", "Mateo", "Inés", "Gael",
            "Julia", "Óscar", "Lola", "Simón", "Carmen", "Félix", "Irene", "Marco", "Paula", "Rufo"
    };
    public static final int STORIES = 6;
    public static final int STORY_PAGES = 3;
    public static final int LETTER_PAGES = 2;
    public static final int SAD_LINES = 10;
    public static final int WAITING_LINES = 4;

    public static String randomName(RandomSource rnd) {
        return NAMES[rnd.nextInt(NAMES.length)];
    }

    private static ItemStack book(String titleKey, String author, String prefix, int pages, String name) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> list = new ArrayList<>();
        for (int i = 1; i <= pages; i++) {
            list.add(Filterable.passThrough(Component.translatable(prefix + ".page" + i, name)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(author), author, 0, list, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(titleKey, name));
        return stack;
    }

    /** The book in the chest: what happened to the house. */
    public static ItemStack storyBook(int story, String name) {
        return book("book.spotless.story.title", name, "book.spotless.story." + story, STORY_PAGES, name);
    }

    /** The signed thank-you letter thrown to every helper. */
    public static ItemStack thankYouLetter(int story, String name, String playerName) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> list = new ArrayList<>();
        for (int i = 1; i <= LETTER_PAGES; i++) {
            list.add(Filterable.passThrough(Component.translatable("book.spotless.letter." + story + ".page" + i, name, playerName)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(name), name, 0, list, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("book.spotless.letter.title", name));
        return stack;
    }

    public static Component sadLine(RandomSource rnd, String name) {
        return Component.translatable("villager.spotless.sad." + (1 + rnd.nextInt(SAD_LINES)), name);
    }

    public static Component waitingLine(RandomSource rnd, String name) {
        return Component.translatable("villager.spotless.waiting." + (1 + rnd.nextInt(WAITING_LINES)), name);
    }
}
