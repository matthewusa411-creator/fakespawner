package dev.fakespawner.mixin;

import dev.fakespawner.FakeConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Moss blocks are called "Spawner" (pink) and iron chestplates are called "Elytra". */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void fakespawner$name(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.has(DataComponents.CUSTOM_NAME)) return;
        if (FakeConfig.spawnerEnabled && self.is(Items.MOSS_BLOCK)) {
            cir.setReturnValue(Component.literal("Spawner")
                    .withStyle(Style.EMPTY.withColor(0xFF55FF).withItalic(false)));
        } else if (FakeConfig.elytraEnabled && self.is(Items.IRON_CHESTPLATE)) {
            cir.setReturnValue(Component.translatable("item.minecraft.elytra"));
        }
    }
}
