package com.templenihility.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;

public class TradeGoal extends Goal {
    private final NihilityCreature creature;
    private Player tradingPlayer;

    public TradeGoal(NihilityCreature creature) {
        this.creature = creature;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return creature.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return creature.getTarget() == null && tradingPlayer != null && tradingPlayer.isAlive();
    }

    @Override
    public void start() {
        // 查找附近的玩家
        if (creature.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            tradingPlayer = serverLevel.getNearestPlayer(creature, 5.0);
        }
    }

    @Override
    public void stop() {
        tradingPlayer = null;
    }

    @Override
    public void tick() {
        if (tradingPlayer != null) {
            creature.getLookControl().setLookAt(tradingPlayer, 30.0f, 30.0f);
        }
    }

}
