package dev.fakespawner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Chest-style screen shown when you open a fake spawner. Purely cosmetic and client side. */
public class SpawnerScreen extends Screen {
    private static final int W = 176, H = 222;
    private static final int SLOT = 18;
    private static final int PILE_CAP_PER_SPAWNER = 64 * 27;

    private final GlobalPos key;
    private long bones = 5, arrows = 5;
    private int ticks;

    private int left, top;

    private final ItemStack boneStack = new ItemStack(Items.BONE);
    private final ItemStack arrowStack = new ItemStack(Items.ARROW);
    private final ItemStack sellStack = new ItemStack(Items.GOLD_INGOT);
    private final ItemStack skullStack = new ItemStack(Items.SKELETON_SKULL);
    private final ItemStack dropStack = new ItemStack(Items.DISPENSER);

    public SpawnerScreen(GlobalPos key) {
        super(Component.literal("Skeleton Spawners"));
        this.key = key;
    }

    private int stack() { return Math.max(1, FakeSpawnerManager.getStack(key)); }
    private long cap() { return (long) PILE_CAP_PER_SPAWNER * stack(); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void tick() {
        // The "loot" piles up on its own, one item per spawner per second.
        if (++ticks % 20 == 0) {
            bones = Math.min(cap(), bones + stack());
            arrows = Math.min(cap(), arrows + stack());
        }
    }

    private int slotX(int col) { return left + 8 + col * SLOT; }
    private int slotY(int row) { return top + 18 + row * SLOT; }

    private static String abbreviate(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return Long.toString(n);
    }

    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 16, y + 16, 0xFF111111);
    }

    private void drawItem(GuiGraphics g, ItemStack s, int x, int y, Long count) {
        g.renderItem(s, x, y);
        if (count != null) {
            String t = abbreviate(count);
            g.drawString(font, t, x + 17 - font.width(t), y + 9, 0xFFFFFFFF, true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(left, top, left + W, top + H, 0xFF2B2B2B);
        g.drawString(font, Component.literal(stack() + " SKELETON SPAWNERS"), left + 8, top + 6, 0xFF8A8A8A, false);

        for (int r = 0; r < 6; r++) for (int c = 0; c < 9; c++) drawSlot(g, slotX(c), slotY(r));

        drawItem(g, boneStack, slotX(0), slotY(0), bones);
        drawItem(g, arrowStack, slotX(1), slotY(0), arrows);
        drawItem(g, sellStack, slotX(3), slotY(5), null);
        drawItem(g, skullStack, slotX(4), slotY(5), null);
        drawItem(g, dropStack, slotX(5), slotY(5), null);

        // Player inventory (display only)
        g.drawString(font, Component.literal("Inventory"), left + 8, top + 128, 0xFFDDDDDD, false);
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int x = left + 8 + c * SLOT, y = top + 140 + r * SLOT;
                drawSlot(g, x, y);
                ItemStack s = inv.getItem(9 + r * 9 + c);
                if (!s.isEmpty()) { g.renderItem(s, x, y); g.renderItemDecorations(font, s, x, y); }
            }
        }
        for (int c = 0; c < 9; c++) {
            int x = left + 8 + c * SLOT, y = top + 198;
            drawSlot(g, x, y);
            ItemStack s = inv.getItem(c);
            if (!s.isEmpty()) { g.renderItem(s, x, y); g.renderItemDecorations(font, s, x, y); }
        }

        // Tooltips for the bottom-row buttons
        if (over(mouseX, mouseY, 3, 5)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("SELL ALL").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal("Click to sell all mob drops!")), mouseX, mouseY);
        } else if (over(mouseX, mouseY, 4, 5)) {
            int pct = (int) Math.min(100, (bones + arrows) * 100 / (2 * cap()));
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(stack() + " SKELETON SPAWNERS").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal(bones + " Bone").withStyle(ChatFormatting.GREEN),
                    Component.literal(arrows + " Arrow").withStyle(ChatFormatting.GREEN),
                    Component.literal("(" + pct + "% filled)").withStyle(ChatFormatting.GREEN)), mouseX, mouseY);
        } else if (over(mouseX, mouseY, 5, 5)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("DROP LOOT").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal("Click to drop all loot on the page")), mouseX, mouseY);
        }
    }

    private boolean over(int mx, int my, int col, int row) {
        int x = slotX(col), y = slotY(row);
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        if (over(mx, my, 3, 5) || over(mx, my, 5, 5)) {
            // Both buttons just empty the fake piles; nothing is sold or dropped for real.
            bones = 0;
            arrows = 0;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
