package com.templenihility.item;

import com.templenihility.energy.VoidPower;
import com.templenihility.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NihilityArmorEvents {
    public static final String STACK_VOID_POWER_KEY = VoidPower.STACK_POWER_KEY;
    public static final String STACK_VOID_POWER_MAX_KEY = VoidPower.STACK_MAX_POWER_KEY;
    private static final double DODGE_CHANCE = 0.25;

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }

        if (hasFullSet(player)) {
            VoidPower.add(player, 1);
            if (VoidPower.get(player) > 0 && repairFirstDamagedNihilityItem(player)) {
                VoidPower.tryConsume(player, 1);
            }
        }
        VoidPower.syncArmorTooltipPower(player);
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || event.getAmount() <= 0.0F
            || !hasFullSet(player)
            || !isAttack(event.getSource())) {
            return;
        }

        if (!VoidPower.tryConsume(player, 1)) {
            return;
        }

        int power = VoidPower.get(player);
        boolean dodged = player.getRandom().nextDouble() < DODGE_CHANCE;
        if (dodged) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(10);
            player.sendOverlayMessage(Component.translatable(
                "message.templenihility.void_power_dodge", power, VoidPower.getMax(player)));
        }
        VoidPower.syncArmorTooltipPower(player);
    }

    public static int getVoidPower(Player player) {
        return VoidPower.get(player);
    }

    public static int getMaxVoidPower() {
        return VoidPower.BASE_MAX;
    }

    public static boolean tryConsumeVoidPower(Player player, int amount) {
        return VoidPower.tryConsume(player, amount);
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
        return VoidPower.isNihilityDamageable(stack);
    }

    private NihilityArmorEvents() {
    }
}
