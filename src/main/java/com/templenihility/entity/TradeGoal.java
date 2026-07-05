package com.templenihility.entity;

import com.templenihility.trade.TradeManager;
import com.templenihility.trade.TradeOffer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TradeGoal extends Goal {
    private final NihilityCreature creature;
    private Player tradingPlayer;

    public TradeGoal(NihilityCreature creature) {
        this.creature = creature;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return tradingPlayer != null && tradingPlayer.isAlive();
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

    /**
     * 处理交易请求
     */
    public boolean handleTrade(Player player, ItemStack payment) {
        int tier = creature.getTier();
        TradeOffer offer = TradeManager.getRandomOffer(tier);

        if (offer != null && offer.canAfford(payment)) {
            // 扣除支付物品
            ItemStack cost = offer.getCost();
            ItemStack result = offer.getResult();

            payment.shrink(cost.getCount());
            player.getInventory().add(result.copy());

            // 发送消息
            if (!creature.level().isClientSide()) {
                player.sendSystemMessage(Component.literal("交易成功! 获得: " + result.getHoverName().getString()));
            }
            return true;
        }
        return false;
    }
}
