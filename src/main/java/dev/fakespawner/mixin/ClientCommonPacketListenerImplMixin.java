package dev.fakespawner.mixin;

import dev.fakespawner.FlightDesync;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While gliding on the fake elytra the server gets no movement packets, so it never sees you fly. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void fakespawner$swallowMovement(Packet<?> packet, CallbackInfo ci) {
        if (!FlightDesync.active()) return;
        if (packet instanceof ServerboundMovePlayerPacket) {
            ci.cancel();
        } else if (packet instanceof ServerboundPlayerCommandPacket command
                && command.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING) {
            ci.cancel();
        }
    }
}
