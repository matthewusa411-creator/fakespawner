package dev.fakespawner.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Moss blocks behave like a real spawner for rendering: Mob Spawner sounds, and they no longer
 * hide the faces of neighbouring blocks (otherwise you see straight through the cage into the ground).
 */
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

    @Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void fakespawner$noFaceOcclusion(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (((BlockState) (Object) this).is(Blocks.MOSS_BLOCK)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void fakespawner$notSolid(CallbackInfoReturnable<Boolean> cir) {
        if (((BlockState) (Object) this).is(Blocks.MOSS_BLOCK)) {
            cir.setReturnValue(false);
        }
    }
}
