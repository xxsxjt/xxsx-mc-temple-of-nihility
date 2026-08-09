package com.templenihility.effect;

import com.templenihility.TempleNihilityMod;
import com.templenihility.world.NihilityVisualEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NihilityRelicEffects {
    private static final String PREFIX = TempleNihilityMod.MOD_ID + ".Relic.";
    private static final String BARRIER_UNTIL = PREFIX + "BarrierUntil";
    private static final String BARRIER_CHARGES = PREFIX + "BarrierCharges";
    private static final String NULLIFICATION_UNTIL = PREFIX + "NullificationUntil";
    private static final String WAR_RESONANCE_UNTIL = PREFIX + "WarResonanceUntil";
    private static final String SOUL_SHIELD_UNTIL = PREFIX + "SoulShieldUntil";
    private static final String SOUL_SHIELD_TENTHS = PREFIX + "SoulShieldTenths";
    private static final String ECHO_MARK_UNTIL = PREFIX + "EchoMarkUntil";
    private static final String BEACON_MARK_UNTIL = PREFIX + "BeaconMarkUntil";
    private static final String SUPPRESSED_UNTIL = PREFIX + "SuppressedUntil";
    private static final String STASIS_UNTIL = PREFIX + "StasisUntil";

    private static final int MAX_FIELD_TARGETS = 32;
    private static final Map<UUID, RiftField> RIFT_FIELDS = new HashMap<>();
    private static final Map<UUID, DrumState> DRUM_STATES = new HashMap<>();

    public static void activateBarrier(Player player, int durationTicks, int charges) {
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        data.putLong(BARRIER_UNTIL, now + durationTicks);
        data.putInt(BARRIER_CHARGES, Math.max(1, charges));
    }

    public static void activateNullification(Player player, int durationTicks) {
        player.getPersistentData().putLong(NULLIFICATION_UNTIL,
            player.level().getGameTime() + durationTicks);
    }

    public static void activateWarResonance(Player player, int durationTicks) {
        player.getPersistentData().putLong(WAR_RESONANCE_UNTIL,
            player.level().getGameTime() + durationTicks);
    }

    public static void grantSoulShield(Player player, float points, int durationTicks) {
        var data = player.getPersistentData();
        float current = getSoulShield(player);
        data.putInt(SOUL_SHIELD_TENTHS, Math.round(Math.max(current, points) * 10.0F));
        data.putLong(SOUL_SHIELD_UNTIL, player.level().getGameTime() + durationTicks);
    }

    public static void markEcho(LivingEntity target, int durationTicks) {
        target.getPersistentData().putLong(ECHO_MARK_UNTIL,
            target.level().getGameTime() + durationTicks);
    }

    public static void markBeacon(LivingEntity target, int durationTicks) {
        target.getPersistentData().putLong(BEACON_MARK_UNTIL,
            target.level().getGameTime() + durationTicks);
    }

    public static void suppress(LivingEntity target, int durationTicks) {
        target.getPersistentData().putLong(SUPPRESSED_UNTIL,
            target.level().getGameTime() + durationTicks);
    }

    public static void applyStasis(LivingEntity target, int durationTicks) {
        target.getPersistentData().putLong(STASIS_UNTIL,
            target.level().getGameTime() + durationTicks);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    public static void createRift(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(7.0));
        RIFT_FIELDS.put(player.getUUID(), new RiftField(level, center, player.getUUID()));
    }

    public static void activateDrum(Player player, int durationTicks) {
        if (player.level() instanceof ServerLevel level) {
            DRUM_STATES.put(player.getUUID(),
                new DrumState(level, player.getUUID(), level.getGameTime() + durationTicks));
        }
    }

    public static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        long now = player.level().getGameTime();
        if (hasBarrier(player, now)) {
            if (player.tickCount % 10 == 0) {
                NihilityVisualEffects.barrierPulse(player, getBarrierCharges(player));
            }
        } else {
            clearBarrier(player);
        }

        if (isActive(player, NULLIFICATION_UNTIL, now)) {
            player.clearFire();
            if (player.tickCount % 16 == 0) {
                NihilityVisualEffects.nullificationPulse(player);
            }
        } else {
            player.getPersistentData().remove(NULLIFICATION_UNTIL);
        }

        if (!isActive(player, WAR_RESONANCE_UNTIL, now)) {
            player.getPersistentData().remove(WAR_RESONANCE_UNTIL);
        }
        if (!isActive(player, SOUL_SHIELD_UNTIL, now)) {
            clearSoulShield(player);
        }
    }

    public static void entityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living)
            || living instanceof Player
            || living.level().isClientSide()) {
            return;
        }

        long now = living.level().getGameTime();
        if (!isActive(living, STASIS_UNTIL, now)) {
            living.getPersistentData().remove(STASIS_UNTIL);
            return;
        }

        living.setDeltaMovement(Vec3.ZERO);
        living.hurtMarked = true;
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (now % 8L == 0L) {
            NihilityVisualEffects.stasisPulse(living);
        }
        event.setCanceled(true);
    }

    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        tickRifts(level);
        tickDrums(level);
    }

    public static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.isCanceled() || target.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }

        long now = target.level().getGameTime();
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof Player attacker) {
            float multiplier = 1.0F;
            if (isActive(attacker, WAR_RESONANCE_UNTIL, now)) {
                multiplier += 0.20F;
            }
            if (isActive(target, ECHO_MARK_UNTIL, now)) {
                multiplier += 0.20F;
            }
            if (isActive(target, BEACON_MARK_UNTIL, now)) {
                multiplier += 0.10F;
            }
            if (multiplier != 1.0F) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }

        if (sourceEntity instanceof LivingEntity livingSource
            && isActive(livingSource, SUPPRESSED_UNTIL, now)) {
            event.setAmount(event.getAmount() * 0.70F);
        }

        if (!(target instanceof Player player)) {
            return;
        }

        if (isActive(player, WAR_RESONANCE_UNTIL, now)) {
            event.setAmount(event.getAmount() * 0.85F);
        }

        if (hasBarrier(player, now)) {
            event.setAmount(event.getAmount() * 0.35F);
            consumeBarrierCharge(player);
            event.setInvulnerabilityTicks(6);
        }

        absorbWithSoulShield(player, event);
    }

    public static void effectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || event.getEffectInstance().getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        if (isActive(player, NULLIFICATION_UNTIL, player.level().getGameTime())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static void projectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
            || !(hit.getEntity() instanceof Player player)
            || player.level().isClientSide()
            || !hasBarrier(player, player.level().getGameTime())) {
            return;
        }
        consumeBarrierCharge(player);
        event.setCanceled(true);
        event.getProjectile().discard();
        NihilityVisualEffects.barrierImpact(player, hit.getLocation());
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearTransientMaps(event.getEntity().getUUID());
    }

    public static void playerCloned(PlayerEvent.Clone event) {
        clearTransientMaps(event.getOriginal().getUUID());
        clearTransientMaps(event.getEntity().getUUID());
        clearPlayerState(event.getEntity());
    }

    private static void tickRifts(ServerLevel level) {
        Iterator<Map.Entry<UUID, RiftField>> iterator = RIFT_FIELDS.entrySet().iterator();
        while (iterator.hasNext()) {
            RiftField field = iterator.next().getValue();
            if (field.level == level && !field.tick()) {
                iterator.remove();
            }
        }
    }

    private static void tickDrums(ServerLevel level) {
        Iterator<Map.Entry<UUID, DrumState>> iterator = DRUM_STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            DrumState state = iterator.next().getValue();
            if (state.level != level) {
                continue;
            }
            if (!state.tick()) {
                iterator.remove();
            }
        }
    }

    private static void absorbWithSoulShield(Player player, LivingIncomingDamageEvent event) {
        long now = player.level().getGameTime();
        if (!isActive(player, SOUL_SHIELD_UNTIL, now)) {
            clearSoulShield(player);
            return;
        }

        float shield = getSoulShield(player);
        if (shield <= 0.0F) {
            clearSoulShield(player);
            return;
        }

        float absorbed = Math.min(shield, event.getAmount());
        float remaining = event.getAmount() - absorbed;
        player.getPersistentData().putInt(SOUL_SHIELD_TENTHS,
            Math.max(0, Math.round((shield - absorbed) * 10.0F)));
        event.setAmount(remaining);
        if (remaining <= 0.0F) {
            event.setCanceled(true);
            event.setInvulnerabilityTicks(4);
        }
        NihilityVisualEffects.soulShieldImpact(player, absorbed);
    }

    private static boolean hasBarrier(Player player, long now) {
        return isActive(player, BARRIER_UNTIL, now) && getBarrierCharges(player) > 0;
    }

    private static int getBarrierCharges(Player player) {
        return Math.max(0, player.getPersistentData().getIntOr(BARRIER_CHARGES, 0));
    }

    private static void consumeBarrierCharge(Player player) {
        int remaining = getBarrierCharges(player) - 1;
        if (remaining <= 0) {
            clearBarrier(player);
            player.sendOverlayMessage(Component.translatable("message.templenihility.barrier_shattered"));
            NihilityVisualEffects.barrierBreak(player);
        } else {
            player.getPersistentData().putInt(BARRIER_CHARGES, remaining);
        }
    }

    private static void clearBarrier(Player player) {
        player.getPersistentData().remove(BARRIER_UNTIL);
        player.getPersistentData().remove(BARRIER_CHARGES);
    }

    private static float getSoulShield(Player player) {
        return Math.max(0, player.getPersistentData().getIntOr(SOUL_SHIELD_TENTHS, 0)) / 10.0F;
    }

    private static void clearSoulShield(Player player) {
        player.getPersistentData().remove(SOUL_SHIELD_UNTIL);
        player.getPersistentData().remove(SOUL_SHIELD_TENTHS);
    }

    private static boolean isActive(LivingEntity entity, String key, long now) {
        long expiresAt = entity.getPersistentData().getLongOr(key, 0L);
        return expiresAt > 0L && expiresAt >= now;
    }

    private static void clearTransientMaps(UUID playerId) {
        RIFT_FIELDS.remove(playerId);
        DRUM_STATES.remove(playerId);
    }

    private static void clearPlayerState(Player player) {
        clearBarrier(player);
        clearSoulShield(player);
        player.getPersistentData().remove(NULLIFICATION_UNTIL);
        player.getPersistentData().remove(WAR_RESONANCE_UNTIL);
    }

    private static boolean isHostile(LivingEntity living) {
        return !(living instanceof Player) && living.getType().getCategory() == MobCategory.MONSTER;
    }

    private static final class RiftField {
        private static final int DURATION = 20 * 6;
        private static final double RANGE = 7.0;
        private final ServerLevel level;
        private final Vec3 center;
        private final UUID ownerId;
        private final List<LivingEntity> targets = new ArrayList<>();
        private int age;

        private RiftField(ServerLevel level, Vec3 center, UUID ownerId) {
            this.level = level;
            this.center = center;
            this.ownerId = ownerId;
        }

        private boolean tick() {
            if (age++ >= DURATION) {
                return false;
            }
            if (age % 4 == 1) {
                scan();
            }
            pull();
            NihilityVisualEffects.riftFieldPulse(level, center, age, targets.size());
            return true;
        }

        private void scan() {
            targets.clear();
            AABB area = new AABB(center, center).inflate(RANGE);
            for (Entity entity : level.getEntities((Entity) null, area,
                    candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
                if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                    targets.add(living);
                    if (targets.size() >= MAX_FIELD_TARGETS) {
                        break;
                    }
                }
            }
        }

        private void pull() {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
            Iterator<LivingEntity> iterator = targets.iterator();
            while (iterator.hasNext()) {
                LivingEntity living = iterator.next();
                if (!living.isAlive() || living.level() != level
                    || living.position().distanceToSqr(center) > (RANGE + 2.0) * (RANGE + 2.0)) {
                    iterator.remove();
                    continue;
                }

                Vec3 pull = center.subtract(living.position());
                if (pull.lengthSqr() > 0.04) {
                    double distance = pull.length();
                    double strength = Math.min(0.42, 0.08 + distance * 0.035);
                    Vec3 velocity = living.getDeltaMovement().scale(0.72)
                        .add(pull.scale(1.0 / distance).scale(strength));
                    if (velocity.lengthSqr() > 0.58 * 0.58) {
                        velocity = velocity.normalize().scale(0.58);
                    }
                    living.setDeltaMovement(velocity);
                    living.hurtMarked = true;
                    living.resetFallDistance();
                }
                if (owner != null && age % 20 == 1) {
                    living.hurtServer(level, owner.damageSources().indirectMagic(owner, owner), 1.5F);
                    markEcho(living, 30);
                }
            }
        }
    }

    private static final class DrumState {
        private final ServerLevel level;
        private final UUID playerId;
        private final long expiresAt;
        private long nextPulse;

        private DrumState(ServerLevel level, UUID playerId, long expiresAt) {
            this.level = level;
            this.playerId = playerId;
            this.expiresAt = expiresAt;
            this.nextPulse = level.getGameTime();
        }

        private boolean tick() {
            long now = level.getGameTime();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null || player.level() != level || now > expiresAt) {
                return false;
            }
            if (now < nextPulse) {
                return true;
            }
            nextPulse = now + 20L;

            int hits = 0;
            for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(14.0),
                    candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
                if (!(entity instanceof LivingEntity living) || !isHostile(living)) {
                    continue;
                }
                if (living.hurtServer(level, player.damageSources().indirectMagic(player, player), 2.5F)) {
                    suppress(living, 50);
                    hits++;
                }
            }
            if (hits > 0) {
                player.heal(Math.min(3.0F, hits * 0.35F));
            }
            NihilityVisualEffects.drumPulse(player, hits);
            return true;
        }
    }

    private NihilityRelicEffects() {
    }
}
