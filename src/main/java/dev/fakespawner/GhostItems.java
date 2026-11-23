package dev.fakespawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-only "ghost" items: item entities the server doesn't know about, which you can pick up
 * into your (client-side) inventory and throw again. A server inventory resync or rejoining removes them.
 */
public final class GhostItems {
    private static final String TAG = "fakespawner_ghost";
    private static final List<ItemEntity> ENTITIES = new ArrayList<>();
    // High ids so they never collide with real entity ids from the server.
    private static int nextId = 2_000_000_000;

    private GhostItems() {}

    public static void clear() {
        ENTITIES.clear();
    }

    public static ItemStack make(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean isGhost(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(TAG);
    }

    public static void spawn(ItemStack stack, double x, double y, double z, Vec3 motion) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement(motion);
        entity.setPickUpDelay(40);
        entity.setId(nextId++);
        level.addEntity(entity);
        ENTITIES.add(entity);
    }

    public static void dropSelected(LocalPlayer player, boolean fullStack) {
        ItemStack dropped = player.getInventory().removeFromSelected(fullStack);
        if (dropped.isEmpty()) return;
        Vec3 look = player.getLookAngle();
        spawn(dropped, player.getX(), player.getEyeY() - 0.3, player.getZ(),
                new Vec3(look.x * 0.3, look.y * 0.3 + 0.1, look.z * 0.3));
        player.swing(InteractionHand.MAIN_HAND);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            ENTITIES.clear();
            return;
        }
        Iterator<ItemEntity> it = ENTITIES.iterator();
        while (it.hasNext()) {
            ItemEntity e = it.next();
            if (e.isRemoved() || e.level() != mc.level) {
                it.remove();
                continue;
            }
            if (e.tickCount > 6000) { // 5 minutes, like real items
                e.discard();
                it.remove();
                continue;
            }
            if (e.hasPickUpDelay()) continue;
            if (mc.player.getBoundingBox().inflate(1.0, 0.5, 1.0).intersects(e.getBoundingBox())) {
                ItemStack stack = e.getItem();
                int before = stack.getCount();
                mc.player.getInventory().add(stack);
                if (stack.getCount() != before) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 1.0F));
                }
                if (stack.isEmpty()) {
                    e.discard();
                    it.remove();
                } else {
                    e.setItem(stack);
                }
            }
        }
    }
}
