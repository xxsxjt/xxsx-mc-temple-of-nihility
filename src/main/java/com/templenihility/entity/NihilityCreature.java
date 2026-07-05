package com.templenihility.entity;

import com.templenihility.menu.NihilityTradeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class NihilityCreature extends PathfinderMob {
    private final TradeGoal tradeGoal;

    protected NihilityCreature(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.tradeGoal = new TradeGoal(this);
    }

    public static AttributeSupplier.Builder createAttributes(int tier) {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, switch (tier) {
                case 1 -> 24.0;
                case 2 -> 36.0;
                case 3 -> 54.0;
                case 4 -> 82.0;
                default -> 150.0;
            })
            .add(Attributes.ATTACK_DAMAGE, switch (tier) {
                case 1 -> 3.0;
                case 2 -> 5.0;
                case 3 -> 7.5;
                case 4 -> 11.0;
                default -> 18.0;
            })
            .add(Attributes.ARMOR, switch (tier) {
                case 1 -> 0.0;
                case 2 -> 2.0;
                case 3 -> 5.0;
                case 4 -> 8.0;
                default -> 14.0;
            })
            .add(Attributes.ARMOR_TOUGHNESS, tier >= 4 ? tier * 1.5 : 0.0)
            .add(Attributes.ATTACK_KNOCKBACK, tier >= 3 ? tier * 0.12 : 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, tier >= 4 ? 0.15 + tier * 0.05 : 0.0)
            .add(Attributes.MOVEMENT_SPEED, switch (tier) {
                case 1 -> 0.25;
                case 2 -> 0.27;
                case 3 -> 0.29;
                case 4 -> 0.27;
                default -> 0.24;
            })
            .add(Attributes.FOLLOW_RANGE, 24.0 + tier * 8.0)
            .add(Attributes.SCALE, 0.9 + tier * 0.09)
            .add(Attributes.STEP_HEIGHT, tier >= 4 ? 1.0 : 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(4, tradeGoal);
    }

    public abstract int getTier();

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return InteractionResult.CONSUME;

        if (player instanceof ServerPlayer serverPlayer) {
            int tier = getTier();
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new NihilityTradeMenu(id, inventory, tier),
                Component.translatable("container.templenihility.nihility_trade", getDisplayName())),
                buffer -> buffer.writeVarInt(tier));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}
