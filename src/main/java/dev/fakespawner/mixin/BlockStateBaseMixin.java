package dev.fakespawner.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blast furnaces use the Mob Spawner's place / break / step / hit sounds. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getSoundType", at = @At("HEAD"), cancellable = true)
    private void fakespawner$spawnerSounds(CallbackInfoReturnable<SoundType> cir) {
        if (((BlockState) (Object) this).is(Blocks.BLAST_FURNACE)) {
            cir.setReturnValue(SoundType.SPAWNER);
        }
    }
}
