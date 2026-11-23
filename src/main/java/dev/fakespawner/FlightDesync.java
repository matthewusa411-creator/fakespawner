package dev.fakespawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * While you glide with the fake elytra (an iron chestplate), your movement packets are not sent,
 * so the server never sees you fly. You only move on your own screen.
 */
public final class FlightDesync {
    private FlightDesync() {}

    public static boolean flightAllowed() {
        return FakeConfig.elytraEnabled && FakeConfig.flightEnabled;
    }

    public static boolean wearingFakeElytra(LocalPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE);
    }

    /** True while movement packets should be swallowed. */
    public static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && flightAllowed() && player.isFallFlying() && wearingFakeElytra(player);
    }
}
