package com.templenihility.compat;

import com.mojang.datafixers.util.Either;
import com.templenihility.TempleNihilityMod;
import com.templenihility.energy.VoidPower;
import com.templenihility.init.ModItems;
import com.templenihility.world.NihilityVisualEffects;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class WaystonesCompat {
    public static final int WAYSTONE_BASE_COST = 12;
    public static final int WAYSTONE_DIMENSIONAL_EXTRA_COST = 12;
    private static final Map<UUID, Integer> PENDING_COSTS = new ConcurrentHashMap<>();
    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        WaystoneTeleportEvent.Before.EVENT.register(WaystonesCompat::beforeTeleport);
        WaystoneTeleportEvent.After.EVENT.register(WaystonesCompat::afterTeleport);
        TempleNihilityMod.LOGGER.info("Temple of Nihility Waystones integration loaded");
    }

    private static void beforeTeleport(WaystoneTeleportEvent.Before event) {
        Object context = event.getContext();
        ServerPlayer player = getPlayer(context);
        boolean nihilityWaystone = isNihilityWaystone(context);
        if (player == null || !nihilityWaystone) {
            return;
        }

        PENDING_COSTS.remove(player.getUUID());

        int cost = getCost(context);
        if (VoidPower.get(player) < cost) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.not_enough_void_power", cost, VoidPower.get(player), VoidPower.getMax(player)));
            return;
        }

        setRequirementsEmpty(context);
        PENDING_COSTS.put(player.getUUID(), cost);
    }

    private static void afterTeleport(WaystoneTeleportEvent.After event) {
        Object context = event.getContext();
        ServerPlayer player = getPlayer(context);
        if (player == null || !isNihilityWaystone(context)) {
            return;
        }

        int cost = PENDING_COSTS.getOrDefault(player.getUUID(), getCost(context));
        PENDING_COSTS.remove(player.getUUID());
        if (VoidPower.tryConsume(player, cost)) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.waystone_void_power_spent", cost, VoidPower.get(player), VoidPower.getMax(player)));
            NihilityVisualEffects.itemUse(player, NihilityVisualEffects.Burst.PHASE);
        }
    }

    private static boolean isNihilityWaystone(Object context) {
        try {
            Method method = context.getClass().getMethod("getWarpItem");
            Object stack = method.invoke(context);
            return stack instanceof ItemStack itemStack && itemStack.is(ModItems.NIHILITY_WAYSTONE.get());
        } catch (ReflectiveOperationException e) {
            TempleNihilityMod.LOGGER.warn("Failed to inspect Waystones warp item", e);
            return false;
        }
    }

    private static ServerPlayer getPlayer(Object context) {
        try {
            Method getEntity = context.getClass().getMethod("getEntity");
            Object entity = getEntity.invoke(context);
            return entity instanceof ServerPlayer player ? player : null;
        } catch (ReflectiveOperationException e) {
            TempleNihilityMod.LOGGER.warn("Failed to inspect Waystones entity", e);
            return null;
        }
    }

    private static boolean isDimensionalTeleport(Object context) {
        try {
            Method method = context.getClass().getMethod("isDimensionalTeleport");
            Object result = method.invoke(context);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException e) {
            TempleNihilityMod.LOGGER.warn("Failed to inspect Waystones dimensional flag", e);
            return false;
        }
    }

    private static void setRequirementsEmpty(Object context) {
        try {
            Method method = context.getClass().getMethod("setRequirements", Either.class);
            method.invoke(context, Either.<List<Object>, List<Object>>left(List.of()));
        } catch (ReflectiveOperationException e) {
            TempleNihilityMod.LOGGER.warn("Failed to override Waystones requirements", e);
        }
    }

    private static int getCost(Object context) {
        return WAYSTONE_BASE_COST + (isDimensionalTeleport(context) ? WAYSTONE_DIMENSIONAL_EXTRA_COST : 0);
    }

    private WaystonesCompat() {
    }
}
