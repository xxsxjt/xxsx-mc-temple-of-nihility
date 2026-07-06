package com.templenihility.item;

import com.templenihility.TempleNihilityMod;
import com.templenihility.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NihilityArmorEvents {
    private static final String VOID_POWER_KEY = TempleNihilityMod.MOD_ID + ".VoidPower";
    private static final int MAX_VOID_POWER = 100;
    private static final double DODGE_CHANCE = 0.25;

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0 || !hasFullSet(player)) {
            return;
        }

        int current = getVoidPower(player);
        if (current < MAX_VOID_POWER) {
            setVoidPower(player, current + 1);
        }
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
    }

    private static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NIHILITY_HELMET.get())
            && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NIHILITY_CHESTPLATE.get())
            && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.NIHILITY_LEGGINGS.get())
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.NIHILITY_BOOTS.get());
    }

    private static boolean isAttack(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null;
    }

    private static int getVoidPower(Player player) {
        return player.getPersistentData().getIntOr(VOID_POWER_KEY, 0);
    }

    private static void setVoidPower(Player player, int value) {
        CompoundTag data = player.getPersistentData();
        data.putInt(VOID_POWER_KEY, Math.max(0, Math.min(MAX_VOID_POWER, value)));
    }

    private NihilityArmorEvents() {
    }
}
