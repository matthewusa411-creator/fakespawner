package dev.fakespawner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Tracks which moss blocks are "fake spawners" (client memory only, never saved). */
public final class FakeSpawnerManager {
    private static final Map<GlobalPos, SpawnerData> SPAWNERS = new HashMap<>();
    private static final List<Broken> BROKEN = new ArrayList<>();
    private static GlobalPos pending;
    private static int pendingAge;

    private static final class Broken {
        final GlobalPos pos;
        int age;
        Broken(GlobalPos pos) { this.pos = pos; }
    }

    private FakeSpawnerManager() {}

    public static void clear() {
        for (SpawnerData data : SPAWNERS.values()) data.removeSkeleton();
        SPAWNERS.clear();
        BROKEN.clear();
        pending = null;
        GhostItems.clear();
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() || !FakeConfig.spawnerEnabled) return InteractionResult.PASS;

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

        if (!FakeConfig.spawnerEnabled) {
            for (SpawnerData data : SPAWNERS.values()) data.removeSkeleton();
            return;
        }

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
            Map.Entry<GlobalPos, SpawnerData> entry = it.next();
            GlobalPos key = entry.getKey();
            SpawnerData data = entry.getValue();
            if (!key.dimension().equals(level.dimension())) continue;
            BlockPos p = key.pos();
            if (!level.hasChunkAt(p)) continue;

            if (!level.getBlockState(p).is(Blocks.MOSS_BLOCK)) {
                // Broken: the spawners go straight into your inventory as ghost items.
                it.remove();
                data.removeSkeleton();
                if (p.distSqr(mc.player.blockPosition()) < 12 * 12) {
                    giveGhostSpawners(mc, data.stack);
                    BROKEN.add(new Broken(key));
                }
                continue;
            }

            if (p.distSqr(mc.player.blockPosition()) < 48 * 48) {
                data.tickSkeleton(level, p);
            }
            if (p.distSqr(mc.player.blockPosition()) < 24 * 24) {
                double x = p.getX() + rand.nextDouble();
                double y = p.getY() + rand.nextDouble();
                double z = p.getZ() + rand.nextDouble();
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            }
        }

        hideBrokenDrops(level);
    }

    private static void giveGhostSpawners(Minecraft mc, int count) {
        int left = count;
        while (left > 0) {
            int n = Math.min(64, left);
            mc.player.getInventory().add(GhostItems.make(Items.MOSS_BLOCK, n));
            left -= n;
        }
    }

    /** The server drops a real moss block where it broke; hide that item so only the ghost one exists. */
    private static void hideBrokenDrops(Level level) {
        Iterator<Broken> it = BROKEN.iterator();
        while (it.hasNext()) {
            Broken b = it.next();
            if (++b.age > 60 || !b.pos.dimension().equals(level.dimension())) {
                it.remove();
                continue;
            }
            AABB box = new AABB(b.pos.pos()).inflate(2.5);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box,
                    e -> e.getItem().is(Items.MOSS_BLOCK) && !GhostItems.isGhost(e.getItem()))) {
                item.discard();
            }
        }
    }

    // ---- tooltips ------------------------------------------------------------------------------

    private static Component plain(String text, int rgb) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(rgb).withItalic(false));
    }

    private static Component worth(long amount) {
        MutableComponent line = Component.literal("~ ")
                .withStyle(Style.EMPTY.withColor(0xAAAAAA).withItalic(false));
        return line.append(plain("$" + FakeConfig.money(amount), 0x55FF55));
    }

    public static void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        if (FakeConfig.spawnerEnabled && stack.is(Items.MOSS_BLOCK)) {
            lines.clear();
            lines.add(plain("Spawner", 0xFF55FF));
            lines.add(plain("Skeleton", 0xFFFF55));
            lines.add(worth(stack.getCount() * FakeConfig.spawnerWorth));
            lines.add(Component.empty());
            lines.add(plain("Interact with Spawn Egg:", 0xAAAAAA));
            lines.add(plain("Sets Mob Type", 0x5555FF));
            if (flag.isAdvanced()) {
                lines.add(plain("minecraft:spawner", 0x555555));
                lines.add(plain("13 component(s)", 0x555555));
            }
        } else if (FakeConfig.elytraEnabled && stack.is(Items.IRON_CHESTPLATE)) {
            lines.clear();
            lines.add(Component.translatable("item.minecraft.elytra").withStyle(Style.EMPTY.withItalic(false)));
            lines.add(worth(stack.getCount() * FakeConfig.elytraWorth));
            if (flag.isAdvanced()) {
                lines.add(plain("minecraft:elytra", 0x555555));
            }
        }
    }
}
