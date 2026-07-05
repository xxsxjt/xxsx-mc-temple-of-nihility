package com.templenihility.entity;

import com.templenihility.config.ModConfig;
import com.templenihility.trade.TradeManager;
import com.templenihility.trade.TradeOffer;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TradeGoal extends Goal {
    private final NihilityCreature creature;
    private Player tradingPlayer;
    private long nextTradeGameTime;

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
        long gameTime = creature.level().getGameTime();
        if (gameTime < nextTradeGameTime) {
            long seconds = Math.max(1, (nextTradeGameTime - gameTime + 19) / 20);
            player.sendSystemMessage(Component.translatable("message.templenihility.trade_cooldown", seconds));
            return true;
        }

        int tier = creature.getTier();
        TradeOffer offer = TradeManager.getAffordableOffer(tier, payment);
        if (offer == null) {
            Optional<ItemStack> cheapest = TradeManager.getCheapestMatchingCost(tier, payment);
            if (cheapest.isPresent()) {
                ItemStack cost = cheapest.get();
                player.sendSystemMessage(Component.translatable(
                    "message.templenihility.trade_need_more",
                    cost.getCount(), cost.getHoverName()));
            } else {
                player.sendSystemMessage(Component.translatable("message.templenihility.trade_wrong_item"));
            }
            return true;
        }

        ItemStack cost = offer.getCost();
        ItemStack result = offer.getResult();
        payment.shrink(cost.getCount());
        if (!player.getInventory().add(result.copy())) {
            player.drop(result.copy(), false);
        }
        nextTradeGameTime = gameTime + ModConfig.TRADE_COOLDOWN.get();
        player.sendSystemMessage(Component.translatable(
            "message.templenihility.trade_success",
            cost.getCount(), cost.getHoverName(), result.getCount(), result.getHoverName()));
        return true;
    }
}
