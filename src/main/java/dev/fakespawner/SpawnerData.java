package dev.fakespawner;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Fake state of one spawner stack: how many are stacked and how much fake loot has piled up. */
public final class SpawnerData {
    public static final int PAGE_SLOTS = 45;           // top 5 rows of the 6-row chest
    public static final long CAP = PAGE_SLOTS * 64L;   // combined bones + arrows

    public int stack = 1;
    public long bones = 5;
    public long arrows = 5;

    private double carry;
    private long last = System.currentTimeMillis();

    /** One bone and one arrow per spawner every 2 seconds. */
    public void update() {
        long now = System.currentTimeMillis();
        double add = (now - last) / 2000.0 * stack + carry;
        last = now;
        long whole = (long) add;
        carry = add - whole;
        long room = Math.max(0, (CAP - bones - arrows) / 2);
        long each = Math.min(whole, room);
        bones += each;
        arrows += each;
    }

    public int percent() {
        return (int) Math.min(100, (bones + arrows) * 100 / CAP);
    }

    private static ItemStack make(Item item, int count, boolean ghost) {
        return ghost ? GhostItems.make(item, count) : new ItemStack(item, count);
    }

    private List<ItemStack> build(boolean ghost, boolean consume) {
        List<ItemStack> out = new ArrayList<>();
        long b = bones, a = arrows;
        while (b > 0 && out.size() < PAGE_SLOTS) {
            int n = (int) Math.min(64, b);
            out.add(make(Items.BONE, n, ghost));
            b -= n;
        }
        while (a > 0 && out.size() < PAGE_SLOTS) {
            int n = (int) Math.min(64, a);
            out.add(make(Items.ARROW, n, ghost));
            a -= n;
        }
        if (consume) {
            bones = b;
            arrows = a;
        }
        return out;
    }

    /** What the chest GUI shows (plain, non-ghost items). */
    public List<ItemStack> display() {
        return build(false, false);
    }

    /** Removes the current page of loot and returns it as ghost items. */
    public List<ItemStack> drainPage() {
        return build(true, true);
    }

    public void clearLoot() {
        bones = 0;
        arrows = 0;
    }
}
