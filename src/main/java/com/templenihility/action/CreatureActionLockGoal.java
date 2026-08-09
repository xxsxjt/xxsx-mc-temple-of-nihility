package com.templenihility.action;

import com.templenihility.entity.NihilityCreature;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public final class CreatureActionLockGoal extends Goal {
    private final NihilityCreature creature;

    public CreatureActionLockGoal(NihilityCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return creature.hasActiveCreatureAction();
    }

    @Override
    public boolean canContinueToUse() {
        return creature.hasActiveCreatureAction();
    }

    @Override
    public void start() {
        creature.getNavigation().stop();
    }

    @Override
    public void tick() {
        creature.getNavigation().stop();
        LivingEntity target = creature.getCreatureActionTarget();
        if (target != null) {
            creature.getLookControl().setLookAt(target, 50.0F, 50.0F);
        }
    }
}
