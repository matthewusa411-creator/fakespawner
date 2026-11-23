package dev.fakespawner.mixin;

import dev.fakespawner.FlightDesync;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An iron chestplate counts as a glider for you, so the elytra flight can start. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true, require = 0)
    private void fakespawner$glide(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer player
                && FlightDesync.flightAllowed()
                && FlightDesync.wearingFakeElytra(player)) {
            cir.setReturnValue(true);
        }
    }
}
