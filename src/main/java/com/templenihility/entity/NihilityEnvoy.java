package com.templenihility.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class NihilityEnvoy extends NihilityCreature {
    public NihilityEnvoy(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int getTier() {
        return 3;
    }
}
