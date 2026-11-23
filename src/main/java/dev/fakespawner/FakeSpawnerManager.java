package dev.fakespawner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Tracks which blast furnaces are "fake spawners" (client memory only, never saved). */
public final class FakeSpawnerManager {
    private static final Map<GlobalPos, Integer> STACKS = new HashMap<>();
    private static GlobalPos pending;
    private static int pendingAge;

    private FakeSpawnerManager() {}

    public static void clear() {
        STACKS.clear();
        pending = null;
    }

    public static int getStack(GlobalPos key) {
        return STACKS.getOrDefault(key, 0);
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        ItemStack held = player.getItemInHand(hand);

        if (STACKS.containsKey(key)) {
            // Crouch + place a blast furnace on a fake spawner -> add to the stack (client side).
            if (player.isShiftKeyDown() && held.is(Items.BLAST_FURNACE)) {
                if (hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
                int n = held.getCount();
                int total = STACKS.merge(key, n, Integer::sum);
                held.shrink(n); // client-side only; the server still has them, so rejoining restores them
                player.displayClientMessage(Component.literal(
                        "You added " + n + " Spawners to the stack, There is now " + total + " in the stack.")
                        .withStyle(ChatFormatting.GREEN), false);
                return InteractionResult.SUCCESS;
            }
            // Normal right click -> open our screen instead of the blast furnace one.
            if (!player.isShiftKeyDown() || held.isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) {
                    Minecraft.getInstance().setScreen(new SpawnerScreen(key));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Placing a normal blast furnace: remember where it should appear, confirm on the next ticks.
        if (held.is(Items.BLAST_FURNACE)) {
            pending = GlobalPos.of(level.dimension(), pos.relative(hit.getDirection()));
            pendingAge = 0;
        }
        return InteractionResult.PASS;
    }

    public static void onTick(Minecraft mc) {
        Level level = mc.level;
        if (level == null || mc.player == null) return;

        if (pending != null) {
            if (pending.dimension().equals(level.dimension())
                    && level.getBlockState(pending.pos()).is(Blocks.BLAST_FURNACE)) {
                STACKS.putIfAbsent(pending, 1);
                pending = null;
            } else if (++pendingAge > 20) {
                pending = null;
            }
        }

        RandomSource rand = level.getRandom();
        Iterator<Map.Entry<GlobalPos, Integer>> it = STACKS.entrySet().iterator();
        while (it.hasNext()) {
            GlobalPos key = it.next().getKey();
            if (!key.dimension().equals(level.dimension())) continue;
            BlockPos p = key.pos();
            if (!level.hasChunkAt(p)) continue;
            if (!level.getBlockState(p).is(Blocks.BLAST_FURNACE)) { // mined / replaced
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
}
