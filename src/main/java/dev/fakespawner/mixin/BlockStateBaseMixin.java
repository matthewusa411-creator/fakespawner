package dev.fakespawner.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Moss blocks use the Mob Spawner sounds and stop hiding neighbouring faces, like a real spawner. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getSoundType", at = @At("HEAD"), cancellable = true)
    private void fakespawner$spawnerSounds(CallbackInfoReturnable<SoundType> cir) {
        if (((BlockState) (Object) this).is(Blocks.MOSS_BLOCK)) {
            cir.setReturnValue(SoundType.SPAWNER);
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void fakespawner$noOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (((BlockState) (Object) this).is(Blocks.MOSS_BLOCK)) {
            cir.setReturnValue(false);
        }
    }
}
