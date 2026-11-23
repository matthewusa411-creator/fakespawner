package dev.fakespawner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * The vanilla 6-row chest screen (so it uses your resource pack's chest texture), filled with fake loot.
 * Nothing is ever sent to the server: clicks are handled here and the slots can't be moved.
 */
public class SpawnerScreen extends ContainerScreen {
    private static final int SELL_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int DROP_SLOT = 50;

    private final SimpleContainer container;
    private final SpawnerData data;
    private final Random random = new Random();

    private SpawnerScreen(ChestMenu menu, Inventory inventory, Component title,
                          SimpleContainer container, SpawnerData data) {
        super(menu, inventory, title);
        this.container = container;
        this.data = data;
    }

    public static SpawnerScreen create(SpawnerData data) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        SimpleContainer container = new SimpleContainer(54);
        // 127 is above the ids a server hands out (1-100), so it can never match a real container.
        ChestMenu menu = new ChestMenu(MenuType.GENERIC_9x6, 127, inventory, container, 6);
        SpawnerScreen screen = new SpawnerScreen(menu, inventory,
                Component.literal(data.stack + " SKELETON SPAWNERS"), container, data);
        screen.refresh();
        return screen;
    }

    // ---- item building -------------------------------------------------------------------------

    private static MutableComponent line(String text, ChatFormatting... formats) {
        return Component.literal(text).withStyle(Style.EMPTY.withItalic(false)).withStyle(formats);
    }

    private static ItemStack button(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private void refresh() {
        data.update();
        List<ItemStack> loot = data.display();
        for (int i = 0; i < 54; i++) {
            container.setItem(i, i < loot.size() ? loot.get(i) : ItemStack.EMPTY);
        }
        for (int i = SpawnerData.PAGE_SLOTS; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        container.setItem(SELL_SLOT, button(Items.GOLD_INGOT,
                line("SELL ALL", ChatFormatting.RED),
                List.of(line("Click to sell all mob drops!", ChatFormatting.WHITE))));

        container.setItem(INFO_SLOT, button(Items.SKELETON_SKULL,
                line(data.stack + " SKELETON SPAWNERS", ChatFormatting.GREEN),
                List.of(
                        line(data.bones + " ", ChatFormatting.GREEN).append(line("Bone", ChatFormatting.WHITE)),
                        line(data.arrows + " ", ChatFormatting.GREEN).append(line("Arrow", ChatFormatting.WHITE)),
                        line("(" + data.percent() + "% filled)", ChatFormatting.GREEN))));

        container.setItem(DROP_SLOT, button(Items.DISPENSER,
                line("DROP LOOT", ChatFormatting.GREEN),
                List.of(line("Click to drop all loot on the page", ChatFormatting.WHITE))));
    }

    // ---- behaviour -----------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        // Never call super: that would move items and send packets. Only the three buttons do anything.
        if (slotId == SELL_SLOT) {
            click();
            data.clearLoot();
            refresh();
        } else if (slotId == DROP_SLOT) {
            click();
            dropLoot();
            refresh();
        }
    }

    private void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void dropLoot() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        for (ItemStack stack : data.drainPage()) {
            Vec3 motion = new Vec3(
                    (random.nextDouble() - 0.5) * 0.2,
                    0.2,
                    (random.nextDouble() - 0.5) * 0.2);
            GhostItems.spawn(stack, player.getX(), player.getY() + 0.5, player.getZ(), motion);
        }
    }
}
