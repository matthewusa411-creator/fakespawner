package dev.fakespawner.mixin;

import dev.fakespawner.GhostItems;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pressing Q on a ghost item throws a ghost item entity instead of telling the server. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void fakespawner$dropGhost(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (GhostItems.isGhost(self.getInventory().getSelectedItem())) {
            GhostItems.dropSelected(self, fullStack);
            cir.setReturnValue(true);
        }
    }
}
