package dev.fakespawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Blocks;

public class FakeSpawnerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Spawner textures have see-through gaps, so moss blocks need the cutout layer too.
        BlockRenderLayerMap.putBlock(Blocks.MOSS_BLOCK, ChunkSectionLayer.CUTOUT);

        UseBlockCallback.EVENT.register(FakeSpawnerManager::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(FakeSpawnerManager::onTick);
        ItemTooltipCallback.EVENT.register(FakeSpawnerManager::onTooltip);
        // Everything is client side only, so it is forgotten when you leave a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FakeSpawnerManager.clear());
    }
}
