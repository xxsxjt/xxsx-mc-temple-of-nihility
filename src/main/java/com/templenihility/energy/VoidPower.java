package com.templenihility.energy;

import com.templenihility.TempleNihilityMod;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.effect.CombatFeedbackPolicy;
import com.templenihility.effect.RelicBuildPolicy;
import com.templenihility.item.NihilityArmor;
import com.templenihility.item.NihilityArmorEvents;
import com.templenihility.network.NihilityNetwork;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class VoidPower {
    public static final String PLAYER_KEY = TempleNihilityMod.MOD_ID + ".VoidPower";
    public static final String STACK_POWER_KEY = "TempleNihilityVoidPower";
    public static final String STACK_MAX_POWER_KEY = "TempleNihilityVoidPowerMax";
    public static final int BASE_MAX = 100;
    public static final int BATTERY_ACCESSORY_BONUS = 200;
    public static final int CONDUIT_ACCESSORY_BONUS = 500;

    public static int get(Player player) {
        int max = getMax(player);
        int stored = Math.max(0, player.getPersistentData().getIntOr(PLAYER_KEY, 0));
        if (stored > max) {
            set(player, max);
            return max;
        }
        return stored;
    }

    public static int getMax(Player player) {
        int max = BASE_MAX
            + RelicBuildPolicy.armorSetVoidPowerBonus(NihilityArmorEvents.countEquippedPieces(player));
        if (CuriosCompat.hasVoidPowerBattery(player)) {
            max += BATTERY_ACCESSORY_BONUS;
        }
        if (CuriosCompat.hasVoidConduitCharm(player)) {
            max += CONDUIT_ACCESSORY_BONUS;
        }
        return max;
    }

    public static int effectiveCost(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int effectiveAmount = amount;
        if (effectiveAmount > 1 && CuriosCompat.isChaosOverdriveActive(player)) {
            effectiveAmount = Math.max(1, (effectiveAmount + 1) / 2);
        }
        return effectiveAmount;
    }

    public static int add(Player player, int amount) {
        if (amount <= 0) {
            return get(player);
        }
        return set(player, get(player) + amount);
    }

    public static int set(Player player, int value) {
        int max = getMax(player);
        int previous = player.getPersistentData().getIntOr(PLAYER_KEY, 0);
        int clamped = Math.max(0, Math.min(max, value));
        player.getPersistentData().putInt(PLAYER_KEY, clamped);
        syncArmorTooltipPower(player);
        if (clamped != previous && !player.level().isClientSide()) {
            NihilityNetwork.syncHud(player);
        }
        return clamped;
    }

    public static boolean tryConsume(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        int effectiveAmount = effectiveCost(player, amount);
        int power = get(player);
        if (power < effectiveAmount) {
            return false;
        }
        set(player, power - effectiveAmount);
        return true;
    }

    public static void copyOnClone(Player original, Player clone, boolean wasDeath) {
        int prev = Math.max(0, original.getPersistentData().getIntOr(PLAYER_KEY, 0));
        int retained = CombatFeedbackPolicy.retainedVoidPowerOnClone(prev, wasDeath);
        clone.getPersistentData().putInt(PLAYER_KEY, Math.min(getMax(clone), retained));
        syncArmorTooltipPower(clone);
    }

    public static boolean canStore(Player player) {
        return NihilityArmorEvents.countEquippedPieces(player) >= 2
            || CuriosCompat.hasVoidPowerBattery(player)
            || CuriosCompat.hasVoidConduitCharm(player);
    }

    public static void syncArmorTooltipPower(Player player) {
        int power = Math.max(0, Math.min(getMax(player), player.getPersistentData().getIntOr(PLAYER_KEY, 0)));
        int max = getMax(player);
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof NihilityArmor) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putInt(STACK_POWER_KEY, power);
                tag.putInt(STACK_MAX_POWER_KEY, max);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    public static boolean isNihilityDamageable(ItemStack stack) {
        return !stack.isEmpty()
            && stack.isDamageableItem()
            && stack.isDamaged()
            && TempleNihilityMod.MOD_ID.equals(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    private VoidPower() {
    }
}
