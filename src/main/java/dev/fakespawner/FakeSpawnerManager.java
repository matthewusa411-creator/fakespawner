package dev.fakespawner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Tracks which moss blocks are "fake spawners" (client memory only, never saved). */
public final class FakeSpawnerManager {
    private static final Map<GlobalPos, SpawnerData> SPAWNERS = new HashMap<>();
    private static GlobalPos pending;
    private static int pendingAge;

    private FakeSpawnerManager() {}

    public static void clear() {
        SPAWNERS.clear();
        pending = null;
        GhostItems.clear();
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        ItemStack held = player.getItemInHand(hand);
        SpawnerData data = SPAWNERS.get(key);

        if (data != null) {
            // Crouch + place a moss block on a fake spawner -> add to the stack (client side only).
            if (player.isShiftKeyDown() && held.is(Items.MOSS_BLOCK)) {
                if (hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
                int n = held.getCount();
                data.stack += n;
                held.shrink(n); // client side only; the server still has them, so rejoining restores them
                player.displayClientMessage(Component.literal(
                        "You added " + n + " Spawners to the stack, There is now " + data.stack + " in the stack.")
                        .withStyle(ChatFormatting.RED), false);
                return InteractionResult.SUCCESS;
            }
            // Normal right click -> our chest screen.
            if (!player.isShiftKeyDown() || held.isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) {
                    Minecraft.getInstance().setScreen(SpawnerScreen.create(data));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Placing a normal moss block: remember where it should appear, confirm on the next ticks.
        if (held.is(Items.MOSS_BLOCK)) {
            pending = GlobalPos.of(level.dimension(), pos.relative(hit.getDirection()));
            pendingAge = 0;
        }
        return InteractionResult.PASS;
    }

    public static void onTick(Minecraft mc) {
        GhostItems.tick(mc);

        Level level = mc.level;
        if (level == null || mc.player == null) return;

        if (pending != null) {
            if (pending.dimension().equals(level.dimension())
                    && level.getBlockState(pending.pos()).is(Blocks.MOSS_BLOCK)) {
                SPAWNERS.putIfAbsent(pending, new SpawnerData());
                pending = null;
            } else if (++pendingAge > 20) {
                pending = null;
            }
        }

        RandomSource rand = level.getRandom();
        Iterator<Map.Entry<GlobalPos, SpawnerData>> it = SPAWNERS.entrySet().iterator();
        while (it.hasNext()) {
            GlobalPos key = it.next().getKey();
            if (!key.dimension().equals(level.dimension())) continue;
            BlockPos p = key.pos();
            if (!level.hasChunkAt(p)) continue;
            if (!level.getBlockState(p).is(Blocks.MOSS_BLOCK)) { // mined / replaced
                it.remove();
                continue;
            }
            if (p.distSqr(mc.player.blockPosition()) < 24 * 24) {
                // Spawner-style smoke and flames
                double x = p.getX() + rand.nextDouble();
                double y = p.getY() + rand.nextDouble();
                double z = p.getZ() + rand.nextDouble();
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            }
        }
    }

    /** Makes the moss block item's tooltip read like a skeleton spawner. */
    public static void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        if (!stack.is(Items.MOSS_BLOCK)) return;
        lines.clear();
        lines.add(plain("Spawner", 0xFF55FF));
        lines.add(plain("Skeleton", 0xFFFF55));
        lines.add(plain("~ $1K", 0x55FF55));
        lines.add(Component.empty());
        lines.add(plain("Interact with Spawn Egg:", 0xAAAAAA));
        lines.add(plain("Sets Mob Type", 0x5555FF));
        if (flag.isAdvanced()) {
            lines.add(plain("minecraft:spawner", 0x555555));
            lines.add(plain("13 component(s)", 0x555555));
        }
    }

    private static Component plain(String text, int rgb) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(rgb).withItalic(false));
    }
}
