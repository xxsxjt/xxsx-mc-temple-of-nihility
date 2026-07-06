package com.templenihility.item;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NihilityArmorEvents {
    private static final String VOID_POWER_KEY = TempleNihilityMod.MOD_ID + ".VoidPower";
    public static final String STACK_VOID_POWER_KEY = "TempleNihilityVoidPower";
    private static final int MAX_VOID_POWER = 100;
    private static final double DODGE_CHANCE = 0.25;

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }

        int current = getVoidPower(player);
        if (hasFullSet(player)) {
            if (current < MAX_VOID_POWER) {
                current++;
            }
            if (current > 0 && repairFirstDamagedNihilityItem(player)) {
                current--;
            }
            setVoidPower(player, current);
        }
        syncArmorTooltipPower(player, current);
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || event.getAmount() <= 0.0F
            || !hasFullSet(player)
            || !isAttack(event.getSource())) {
            return;
        }

        int power = getVoidPower(player);
        if (power <= 0) {
            return;
        }

        power--;
        boolean dodged = player.getRandom().nextDouble() < DODGE_CHANCE;
        setVoidPower(player, power);
        if (dodged) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(10);
            player.sendOverlayMessage(Component.translatable(
                "message.templenihility.void_power_dodge", power, MAX_VOID_POWER));
        }
        syncArmorTooltipPower(player, power);
    }

    public static int getVoidPower(Player player) {
        return player.getPersistentData().getIntOr(VOID_POWER_KEY, 0);
    }

    public static int getMaxVoidPower() {
        return MAX_VOID_POWER;
    }

    public static boolean tryConsumeVoidPower(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        int power = getVoidPower(player);
        if (power < amount) {
            return false;
        }
        setVoidPower(player, power - amount);
        syncArmorTooltipPower(player, power - amount);
        return true;
    }

    public static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NIHILITY_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NIHILITY_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.NIHILITY_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.NIHILITY_BOOTS.get());
    }

    private static boolean isAttack(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null;
    }

    private static void setVoidPower(Player player, int value) {
        CompoundTag data = player.getPersistentData();
        data.putInt(VOID_POWER_KEY, Math.max(0, Math.min(MAX_VOID_POWER, value)));
    }

    private static boolean repairFirstDamagedNihilityItem(Player player) {
        if (repairOne(player.getMainHandItem())) {
            player.getInventory().setChanged();
            return true;
        }

        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (repairOne(player.getItemBySlot(slot))) {
                player.getInventory().setChanged();
                return true;
            }
        }

        if (repairOne(player.getOffhandItem())) {
            player.getInventory().setChanged();
            return true;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (repairOne(player.getInventory().getItem(i))) {
                player.getInventory().setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean repairOne(ItemStack stack) {
        if (!isRepairableNihilityStack(stack)) {
            return false;
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
        return true;
    }

    private static boolean isRepairableNihilityStack(ItemStack stack) {
        return !stack.isEmpty()
            && stack.isDamageableItem()
            && stack.isDamaged()
            && TempleNihilityMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    private static void syncArmorTooltipPower(Player player, int power) {
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof NihilityArmor) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putInt(STACK_VOID_POWER_KEY, Math.max(0, Math.min(MAX_VOID_POWER, power)));
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    private NihilityArmorEvents() {
    }
}
