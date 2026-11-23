package dev.fakespawner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

public class FakeSpawnerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register(FakeSpawnerManager::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(FakeSpawnerManager::onTick);
        // Everything is client-side only, so it is forgotten when you leave a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FakeSpawnerManager.clear());
    }
}
