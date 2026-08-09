package com.templenihility.entity;

import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NihilityBeliever extends NihilityCreature {
    public NihilityBeliever(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new AvoidEntityGoal<>(
            this,
            Player.class,
            target -> getDisposition() == Disposition.CALM && target.isSprinting(),
            5.0F,
            0.85,
            1.05,
            EntitySelector.NO_CREATIVE_OR_SPECTATOR));
    }

    @Override
    public int getTier() {
        return 1;
    }
}
