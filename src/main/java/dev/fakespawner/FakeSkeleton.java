package dev.fakespawner;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** A client-only skeleton that sits inside a fake spawner. The server never hears about it. */
public class FakeSkeleton extends Skeleton {
    private static int nextId = 2_100_000_000; // far above ids a server hands out

    public FakeSkeleton(Level level) {
        super(EntityType.SKELETON, level);
        setNoAi(true);
        setSilent(true);
        setNoGravity(true);
        setInvulnerable(true);
        this.noPhysics = true;
        AttributeInstance scale = getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(0.45); // fits inside the cage
        }
        refreshDimensions();
        setId(nextId++);
    }

    /** Not targetable, so clicks go through to the spawner block underneath. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
