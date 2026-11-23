package dev.fakespawner;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

public class FakeSpawnerClient implements ClientModInitializer {
    private static boolean shiftWasDown;

    @Override
    public void onInitializeClient() {
        FakeConfig.load();

        // Spawner textures have see-through gaps, so moss blocks need the cutout layer too.
        BlockRenderLayerMap.putBlock(Blocks.MOSS_BLOCK, ChunkSectionLayer.CUTOUT);

        UseBlockCallback.EVENT.register(FakeSpawnerManager::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(FakeSpawnerManager::onTick);
        ClientTickEvents.END_CLIENT_TICK.register(FakeSpawnerClient::pollMenuKey);
        ItemTooltipCallback.EVENT.register(FakeSpawnerManager::onTooltip);
        // Everything is client side only, so it is forgotten when you leave a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FakeSpawnerManager.clear());
    }

    /** Right Shift opens (or closes) the settings menu. */
    private static void pollMenuKey(Minecraft mc) {
        boolean down = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (down && !shiftWasDown) {
            if (mc.screen == null) {
                mc.setScreen(new MenuScreen());
            } else if (mc.screen instanceof MenuScreen) {
                mc.setScreen(null);
            }
        }
        shiftWasDown = down;
    }
}
