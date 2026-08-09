package com.templenihility.world;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.entity.NihilityCreature;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.init.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class NihilityVisualEffects {
    public enum WeaponAction {
        MISSING_STEP,
        ZERO_SLASH,
        BOUNDARY_BREAK
    }

    public enum Burst {
        SHADOW,
        PURIFY,
        ECHO,
        BARRIER,
        PHASE,
        GRAVITY,
        WAR,
        HEAL,
        NULLIFY,
        STASIS,
        RIFT,
        VOID_BEACON,
        CHAOS
    }

    public static void weaponWindup(Player player, WeaponAction action) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        switch (action) {
            case MISSING_STEP -> {
                cloud(level, center, shard(), 12, 0.24, 0.36, 0.24, 0.015);
                ring(level, center, 0.65, ripple(), 12, 0.008);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, 0.35F, 1.65F);
            }
            case ZERO_SLASH -> {
                cloud(level, center.add(player.getLookAngle().scale(0.7)),
                    mote(), 10, 0.12, 0.18, 0.12, 0.015);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.25F, 1.55F);
            }
            case BOUNDARY_BREAK -> {
                ring(level, center, 0.85, ripple(), 22, 0.008);
                cloud(level, center, shard(), 16, 0.3, 0.16, 0.3, 0.016);
                sound(level, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.45F, 0.65F);
            }
        }
    }

    public static void missingStep(ServerLevel level, Vec3 from, Vec3 to) {
        line(level, from.add(0.0, 0.9, 0.0), to.add(0.0, 0.9, 0.0), shard(), 0.28);
        line(level, from.add(0.0, 1.15, 0.0), to.add(0.0, 1.15, 0.0), mote(), 0.48);
        cloud(level, to.add(0.0, 0.9, 0.0), ripple(), 18, 0.22, 0.42, 0.22, 0.018);
        sound(level, BlockPos.containing(to), SoundEvents.ENDERMAN_TELEPORT, 0.55F, 1.65F);
    }

    public static void zeroSlash(ServerLevel level, Vec3 start, Vec3 direction, double reach, int chargeTier) {
        Vec3 end = start.add(direction.scale(reach));
        line(level, start, end, mote(), 0.28);
        line(level, start.add(0.0, -0.08, 0.0), end.add(0.0, -0.08, 0.0), shard(), 0.22);
        cloud(level, end, ripple(), 9 + chargeTier * 4, 0.14, 0.14, 0.14, 0.015);
        sound(level, BlockPos.containing(start), SoundEvents.PLAYER_ATTACK_SWEEP, 0.75F, 0.72F + chargeTier * 0.08F);
        sound(level, BlockPos.containing(end), SoundEvents.AMETHYST_BLOCK_BREAK, 0.45F, 1.35F);
    }

    public static void boundaryBreakWave(ServerLevel level, Vec3 origin, Vec3 direction, int wave) {
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        double distance = 2.25 + wave * 2.45;
        double halfWidth = 2.0 + wave * 0.8;
        int points = 24 + wave * 6;
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points * 2.0 - 1.0;
            double forwardOffset = distance - Math.abs(t) * (0.65 + wave * 0.2);
            Vec3 point = origin.add(direction.scale(forwardOffset))
                .add(side.scale(t * halfWidth))
                .add(0.0, 0.12 + (1.0 - Math.abs(t)) * 0.14, 0.0);
            cloud(level, point, i % 3 == 0 ? ripple() : shard(),
                1, 0.025, 0.025, 0.025, 0.005);
        }
        sound(level, BlockPos.containing(origin.add(direction.scale(distance))),
            SoundEvents.GENERIC_EXPLODE.value(), 0.42F + wave * 0.08F, 0.82F - wave * 0.08F);
    }

    public static void itemUse(Player player, Burst burst) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        switch (burst) {
            case SHADOW -> {
                cloud(level, center, shard(), 28, 0.52, 0.42, 0.52, 0.025);
                cloud(level, center.add(player.getLookAngle().scale(1.1)), ripple(), 14, 0.2, 0.18, 0.2, 0.025);
                ring(level, center, 1.15, mote(), 20, 0.012);
                sound(level, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 0.55F, 1.35F);
            }
            case PURIFY -> {
                ring(level, center, 1.6, ripple(), 32, 0.025);
                ring(level, center, 2.45, shard(), 36, 0.018);
                cloud(level, center, mote(), 18, 0.4, 0.18, 0.4, 0.018);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.25F);
                sound(level, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.45F, 1.2F);
            }
            case ECHO -> {
                ring(level, center, 2.2, ripple(), 36, 0.016);
                cloud(level, center.add(0.0, 0.45, 0.0), mote(), 20, 0.35, 0.2, 0.35, 0.018);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.65F, 1.45F);
            }
            case BARRIER -> {
                ring(level, center, 1.25, ripple(), 32, 0.018);
                ring(level, center, 1.85, shard(), 36, 0.018);
                cloud(level, center, mote(), 12, 0.32, 0.22, 0.32, 0.015);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.75F, 1.05F);
            }
            case PHASE -> {
                Vec3 look = player.getLookAngle().normalize();
                for (int i = 0; i < 8; i++) {
                    Vec3 point = center.subtract(look.scale(i * 0.35));
                    cloud(level, point, shard(), 3, 0.08, 0.06, 0.08, 0.01);
                }
                cloud(level, center.add(look.scale(1.4)), ripple(), 14, 0.16, 0.12, 0.16, 0.025);
                sound(level, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5F, 1.35F);
            }
            case GRAVITY -> {
                Vec3 field = center.add(player.getLookAngle().normalize().scale(8.0)).add(0.0, 0.4, 0.0);
                ring(level, field, 1.25, ripple(), 36, 0.035);
                cloud(level, field, shard(), 30, 0.65, 0.32, 0.65, 0.045);
                sound(level, BlockPos.containing(field), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7F, 0.75F);
            }
            case WAR -> {
                ring(level, center, 2.6, abyss(), 42, 0.028);
                ring(level, center, 3.35, abyss(), 48, 0.016);
                cloud(level, center, abyss(), 20, 0.55, 0.22, 0.55, 0.018);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.72F);
            }
            case HEAL -> {
                cloud(level, center, abyss(), 24, 0.42, 0.25, 0.42, 0.025);
                ring(level, center, 1.05, abyss(), 20, 0.012);
                sound(level, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.55F, 1.55F);
            }
            case NULLIFY -> {
                ring(level, center, 1.85, shard(), 36, 0.018);
                cloud(level, center, ripple(), 24, 0.45, 0.32, 0.45, 0.025);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 0.65F);
            }
            case STASIS -> {
                ring(level, center, 2.1, ripple(), 40, 0.018);
                ring(level, center.add(0.0, 0.35, 0.0), 2.1, shard(), 40, 0.01);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.55F, 0.65F);
            }
            case RIFT -> {
                ring(level, center, 1.35, ripple(), 38, 0.045);
                cloud(level, center.add(0.0, 0.2, 0.0), shard(), 28, 0.5, 0.2, 0.5, 0.045);
                sound(level, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.65F, 0.85F);
            }
            case VOID_BEACON -> {
                for (int y = 0; y < 7; y++) {
                    cloud(level, center.add(0.0, y * 0.45, 0.0), mote(), 3, 0.09, 0.03, 0.09, 0.01);
                }
                ring(level, center, 2.8, ripple(), 44, 0.016);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.45F);
            }
            case CHAOS -> {
                ring(level, center, 1.2, chaos(), 30, 0.028);
                cloud(level, center, chaos(), 28, 0.5, 0.4, 0.5, 0.04);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.7F);
            }
        }
    }

    public static void lanternPulse(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        cloud(level, center, mote(), 10, 0.22, 0.35, 0.22, 0.02);
        ring(level, center, 0.75, ripple(), 14, 0.01);
        sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 1.55F);
    }

    public static void voidPhaseTrail(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.95, 0.0);
        cloud(level, center, shard(), 9, 0.32, 0.45, 0.32, 0.012);
        cloud(level, center.add(0.0, 0.25, 0.0), mote(), 6, 0.22, 0.25, 0.22, 0.02);
        if (player.tickCount % 32 == 0) {
            ring(level, center, 0.72, ripple(), 14, 0.01);
        }
    }

    public static void magnetPulse(Player player, int targetCount) {
        if (!(player.level() instanceof ServerLevel level) || targetCount <= 0) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.75, 0.0);
        int count = Math.min(14, 4 + targetCount / 4);
        cloud(level, center, shard(), count, 0.45, 0.2, 0.45, 0.025);
        ring(level, center, 0.85, ripple(), 14, 0.012);
    }

    /** Low-rate flowing refraction around Nihility creatures; deliberately bounded per entity. */
    public static void creatureFlow(NihilityCreature creature) {
        if (!(creature.level() instanceof ServerLevel level)) {
            return;
        }

        int tier = Math.max(1, Math.min(5, creature.getTier()));
        Disposition disposition = creature.getDisposition();
        int points = 2 + (tier >= 3 ? 1 : 0) + (tier >= 5 ? 1 : 0)
            + (disposition.blocksTrading() ? 1 : 0);
        double height = Math.max(1.6, creature.getBbHeight() * 0.82);
        double radius = 0.18 + tier * 0.035 + (disposition == Disposition.HOSTILE ? 0.08 : 0.0);
        int phase = creature.tickCount / 6;
        for (int i = 0; i < points; i++) {
            double progress = (i + 0.5) / points;
            double angle = phase * 0.16 + progress * Math.PI * 2.0 + tier * 0.4;
            double x = creature.getX() + Math.cos(angle) * radius;
            double y = creature.getY() + 0.18 + progress * height;
            double z = creature.getZ() + Math.sin(angle) * radius;
            ParticleOptions flow = (phase + i) % 3 == 0 ? ripple() : mote();
            level.sendParticles(flow, x, y, z, 1,
                -Math.sin(angle) * 0.012, 0.004, Math.cos(angle) * 0.012, 0.0);
        }
    }

    public static void creatureActionWindup(NihilityCreature creature, ActionType action, LivingEntity target) {
        if (!(creature.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = creature.position().add(0.0, creature.getBbHeight() * 0.48, 0.0);
        if (action == ActionType.FOLLOWER_INTERCEPT) {
            ring(level, center, 0.55, shard(), 12, 0.012);
            sound(level, creature.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, 0.35F, 0.72F);
        } else if (action == ActionType.ENVOY_BACKSTEP) {
            cloud(level, center, ripple(), 9, 0.24, 0.35, 0.24, 0.018);
            sound(level, creature.blockPosition(), SoundEvents.ENDERMAN_STARE, 0.28F, 1.35F);
        } else if (action == ActionType.ENVOY_RIFT_DIRECTION) {
            Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.18, 0.0);
            line(level, center, targetCenter, ripple(), 0.42);
            ring(level, targetCenter, 0.62, shard(), 18, 0.0);
            sound(level, creature.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.42F, 1.28F);
        } else if (action == ActionType.GRAND_ENVOY_DOMAIN) {
            ring(level, center, 1.1, ripple(), 28, -0.012);
            cloud(level, center, shard(), 14, 0.28, 0.62, 0.28, 0.018);
            sound(level, creature.blockPosition(), SoundEvents.BEACON_ACTIVATE, 0.55F, 0.58F);
        } else if (action == ActionType.GRAND_ENVOY_RECALL) {
            ring(level, center, 0.82, mote(), 24, 0.018);
            beam(level, center.add(0.0, -0.3, 0.0), 8, ripple());
            sound(level, creature.blockPosition(), SoundEvents.ENDERMAN_STARE, 0.42F, 0.92F);
        } else if (action == ActionType.GRAND_ENVOY_SHIELD) {
            ring(level, center, 0.95, shard(), 30, 0.0);
            ring(level, center, 1.35, ripple(), 36, 0.015);
            sound(level, creature.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.55F, 0.82F);
        } else if (action == ActionType.SAINT_ERASURE_LINE) {
            Vec3 targetCenter = target.position().add(0.0, 0.12, 0.0);
            line(level, center, targetCenter, mote(), 0.38);
            ring(level, targetCenter, 0.85, shard(), 22, 0.0);
            sound(level, creature.blockPosition(), SoundEvents.BEACON_DEACTIVATE, 0.62F, 0.58F);
        } else if (action == ActionType.SAINT_MISSING_ZONE) {
            Vec3 targetCenter = target.position().add(0.0, 0.08, 0.0);
            ring(level, targetCenter, com.templenihility.action.SaintActionPolicy.MISSING_ZONE_RADIUS,
                ripple(), 28, -0.01);
            cloud(level, targetCenter, shard(), 12, 0.4, 0.05, 0.4, 0.012);
            sound(level, target.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.5F, 0.68F);
        }
    }

    public static void creatureIntercept(NihilityCreature creature, LivingEntity target, boolean hit) {
        if (!(creature.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = creature.position().add(0.0, creature.getBbHeight() * 0.5, 0.0);
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        line(level, from, to, hit ? mote() : shard(), hit ? 0.24 : 0.4);
        if (hit) {
            cloud(level, to, ripple(), 12, 0.22, 0.28, 0.22, 0.022);
            sound(level, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.65F, 0.82F);
        }
    }

    public static void creatureBackstep(NihilityCreature creature, LivingEntity target) {
        if (!(creature.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = creature.position().add(0.0, creature.getBbHeight() * 0.5, 0.0);
        Vec3 away = from.subtract(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
        Vec3 to = away.lengthSqr() > 1.0E-6 ? from.add(away.normalize().scale(2.0)) : from;
        line(level, from, to, ripple(), 0.28);
        cloud(level, from, shard(), 14, 0.28, 0.36, 0.28, 0.025);
        sound(level, creature.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 0.45F, 1.55F);
    }

    public static void saintChallengeStart(NihilityCreature saint, BlockPos origin) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = saint.position().add(0.0, saint.getBbHeight() * 0.52, 0.0);
        ring(level, center, 1.25, ripple(), 36, 0.018);
        ring(level, center, 1.75, shard(), 44, -0.012);
        cloud(level, center, mote(), 28, 0.38, 0.72, 0.38, 0.025);
        beam(level, center.add(0.0, -0.35, 0.0), 14, mote());
        ring(level, Vec3.atCenterOf(origin).add(0.0, 0.08, 0.0),
            com.templenihility.challenge.SaintChallengePolicy.ARENA_RADIUS, ripple(), 72, 0.0);
        sound(level, saint.blockPosition(), SoundEvents.BEACON_ACTIVATE, 0.75F, 0.62F);
    }

    public static void saintChallengeDeclarationPulse(NihilityCreature saint) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = saint.position().add(0.0, saint.getBbHeight() * 0.5, 0.0);
        ring(level, center, 0.72 + (saint.tickCount % 30) * 0.02, ripple(), 20, 0.012);
        cloud(level, center, shard(), 8, 0.22, 0.52, 0.22, 0.015);
    }

    public static void saintChallengeBoundary(NihilityCreature saint, BlockPos origin) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 boundaryCenter = Vec3.atCenterOf(origin).add(0.0, 0.08, 0.0);
        ring(level, boundaryCenter, com.templenihility.challenge.SaintChallengePolicy.ARENA_RADIUS,
            ripple(), 72, 0.0);
        Vec3 saintCenter = saint.position().add(0.0, saint.getBbHeight() * 0.52, 0.0);
        ring(level, saintCenter, 0.92, mote(), 24, 0.01);
    }

    public static void saintChallengeEnd(NihilityCreature saint, boolean victory) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = saint.position().add(0.0, saint.getBbHeight() * 0.48, 0.0);
        cloud(level, center, victory ? mote() : ripple(), victory ? 48 : 24,
            0.62, 0.8, 0.62, victory ? 0.055 : 0.028);
        ring(level, center, victory ? 2.2 : 1.35, shard(), victory ? 56 : 32,
            victory ? 0.035 : -0.018);
        sound(level, saint.blockPosition(),
            victory ? SoundEvents.END_PORTAL_SPAWN : SoundEvents.BEACON_DEACTIVATE,
            victory ? 0.85F : 0.55F, victory ? 1.45F : 0.72F);
    }

    public static void saintErasureLineTelegraph(NihilityCreature saint, Vec3 origin, Vec3 direction) {
        if (!(saint.level() instanceof ServerLevel level) || direction.lengthSqr() <= 1.0E-6) {
            return;
        }
        Vec3 forward = direction.multiply(1.0, 0.0, 1.0).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x)
            .scale(com.templenihility.action.SaintActionPolicy.ERASURE_LINE_HALF_WIDTH);
        Vec3 start = origin.add(0.0, 0.1, 0.0);
        Vec3 end = start.add(forward.scale(com.templenihility.action.SaintActionPolicy.ERASURE_LINE_LENGTH));
        line(level, start, end, mote(), 0.48);
        line(level, start.add(side), end.add(side), ripple(), 0.72);
        line(level, start.subtract(side), end.subtract(side), ripple(), 0.72);
    }

    public static void saintErasureLineResolve(NihilityCreature saint, Vec3 origin,
                                               Vec3 direction, boolean hit) {
        if (!(saint.level() instanceof ServerLevel level) || direction.lengthSqr() <= 1.0E-6) {
            return;
        }
        Vec3 forward = direction.multiply(1.0, 0.0, 1.0).normalize();
        Vec3 start = origin.add(0.0, 0.16, 0.0);
        Vec3 end = start.add(forward.scale(com.templenihility.action.SaintActionPolicy.ERASURE_LINE_LENGTH));
        line(level, start, end, hit ? shard() : ripple(), 0.22);
        for (int i = 2; i <= 18; i += 2) {
            cloud(level, start.add(forward.scale(i)), hit ? mote() : shard(),
                4, 0.16, 0.12, 0.16, 0.025);
        }
        sound(level, saint.blockPosition(),
            hit ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.AMETHYST_BLOCK_BREAK,
            hit ? 0.65F : 0.45F, hit ? 1.45F : 0.82F);
    }

    public static void saintMissingZonesTelegraph(NihilityCreature saint, Vec3[] centers) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        for (Vec3 center : centers) {
            ring(level, center.add(0.0, 0.08, 0.0),
                com.templenihility.action.SaintActionPolicy.MISSING_ZONE_RADIUS,
                ripple(), 18, -0.012);
        }
    }

    public static void saintMissingZonesActivate(NihilityCreature saint, Vec3[] centers) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        for (Vec3 center : centers) {
            ring(level, center.add(0.0, 0.08, 0.0),
                com.templenihility.action.SaintActionPolicy.MISSING_ZONE_RADIUS,
                shard(), 32, 0.025);
            cloud(level, center.add(0.0, 0.16, 0.0), ripple(),
                18, 0.55, 0.08, 0.55, 0.035);
        }
        sound(level, saint.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, 0.65F, 0.62F);
    }

    public static void saintMissingZonesPulse(NihilityCreature saint, Vec3[] centers) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        for (Vec3 center : centers) {
            ring(level, center.add(0.0, 0.06, 0.0),
                com.templenihility.action.SaintActionPolicy.MISSING_ZONE_RADIUS,
                ripple(), 12, -0.018);
            cloud(level, center.add(0.0, 0.12, 0.0), mote(), 3, 0.28, 0.03, 0.28, 0.01);
        }
    }

    public static void saintVoidTerminusTelegraph(NihilityCreature saint, Vec3 center) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 c = center.add(0.0, 0.08, 0.0);
        ring(level, c, com.templenihility.action.SaintActionPolicy.TERMINUS_START_RADIUS,
            ripple(), 44, -0.012);
        ring(level, c, com.templenihility.action.SaintActionPolicy.TERMINUS_END_RADIUS,
            mote(), 16, 0.008);
        sound(level, saint.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.6F, 0.5F);
    }

    public static void saintVoidTerminusActivate(NihilityCreature saint, Vec3 center) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 c = center.add(0.0, 0.1, 0.0);
        ring(level, c, com.templenihility.action.SaintActionPolicy.TERMINUS_START_RADIUS,
            shard(), 64, 0.03);
        cloud(level, saint.position().add(0.0, saint.getBbHeight() * 0.5, 0.0),
            abyss(), 40, 0.6, 0.9, 0.6, 0.05);
        sound(level, saint.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 0.8F, 0.5F);
    }

    public static void saintVoidTerminusPulse(NihilityCreature saint, Vec3 center, double radius) {
        if (!(saint.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 c = center.add(0.0, 0.08, 0.0);
        ring(level, c, radius, ripple(), 40, -0.01);
        ring(level, c, com.templenihility.action.SaintActionPolicy.TERMINUS_END_RADIUS,
            mote(), 20, 0.006);
    }

    public static void grandEnvoyDomainStart(NihilityCreature envoy, BlockPos origin) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(origin).add(0.0, 0.08, 0.0);
        ring(level, center, com.templenihility.action.GrandEnvoyActionPolicy.DOMAIN_RADIUS,
            ripple(), 56, -0.012);
        cloud(level, envoy.position().add(0.0, envoy.getBbHeight() * 0.5, 0.0),
            shard(), 32, 0.48, 0.72, 0.48, 0.04);
        sound(level, origin, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.72F, 0.62F);
    }

    public static void grandEnvoyDomainPulse(ServerLevel level, Vec3 center, double radius) {
        ring(level, center.add(0.0, 0.08, 0.0), radius, ripple(), 48, -0.008);
        ring(level, center.add(0.0, 0.22, 0.0), radius * 0.55, mote(), 28, 0.006);
    }

    public static void grandEnvoyDomainDrag(Player player, Vec3 center) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 playerCenter = player.position().add(0.0, 0.8, 0.0);
        Vec3 inward = center.subtract(playerCenter).multiply(1.0, 0.0, 1.0);
        if (inward.lengthSqr() > 1.0E-6) {
            inward = inward.normalize().scale(0.018);
        }
        level.sendParticles(ripple(), playerCenter.x, playerCenter.y, playerCenter.z,
            3, inward.x, 0.01, inward.z, 0.0);
    }

    public static void grandEnvoyShieldRaised(NihilityCreature envoy, int charges) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = envoy.position().add(0.0, envoy.getBbHeight() * 0.54, 0.0);
        ring(level, center, 1.35, ripple(), 42, 0.025);
        grandEnvoyShieldNodes(level, center, charges, 5);
        sound(level, envoy.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), 0.62F, 0.72F);
    }

    public static void grandEnvoyShieldPulse(NihilityCreature envoy, int charges) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = envoy.position().add(0.0, envoy.getBbHeight() * 0.54, 0.0);
        grandEnvoyShieldNodes(level, center, charges, 2);
    }

    public static void grandEnvoyShieldImpact(NihilityCreature envoy, int chargesLeft) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = envoy.position().add(0.0, envoy.getBbHeight() * 0.52, 0.0);
        cloud(level, center, shard(), 18, 0.42, 0.55, 0.42, 0.045);
        ring(level, center, 1.05, ripple(), 28, 0.028);
        if (chargesLeft > 0) {
            grandEnvoyShieldNodes(level, center, chargesLeft, 4);
        }
        sound(level, envoy.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), 0.7F, 1.18F);
    }

    public static void grandEnvoyRecall(NihilityCreature envoy, NihilityCreature ally,
                                        Vec3 from, Vec3 destination) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        line(level, from.add(0.0, ally.getBbHeight() * 0.5, 0.0),
            destination.add(0.0, ally.getBbHeight() * 0.5, 0.0), ripple(), 0.35);
        cloud(level, destination.add(0.0, ally.getBbHeight() * 0.5, 0.0),
            mote(), 14, 0.25, 0.42, 0.25, 0.025);
        sound(level, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, 0.42F, 1.32F);
    }

    public static void followerProjectileGuard(NihilityCreature follower, NihilityCreature protectedAlly,
                                               Vec3 impact) {
        if (!(follower.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 guardCenter = follower.position().add(0.0, follower.getBbHeight() * 0.55, 0.0);
        Vec3 allyCenter = protectedAlly.position().add(0.0, protectedAlly.getBbHeight() * 0.55, 0.0);
        line(level, guardCenter, impact, shard(), 0.28);
        line(level, guardCenter, allyCenter, mote(), 0.42);
        cloud(level, impact, ripple(), 18, 0.18, 0.18, 0.18, 0.035);
        ring(level, allyCenter, 0.72, ripple(), 18, 0.015);
        sound(level, protectedAlly.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), 0.62F, 1.42F);
    }

    public static void envoyProjectileDeflect(NihilityCreature envoy, Vec3 position, Vec3 reflected) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 direction = reflected.lengthSqr() > 1.0E-6 ? reflected.normalize() : Vec3.ZERO;
        line(level, position, position.add(direction.scale(2.4)), mote(), 0.24);
        cloud(level, position, shard(), 16, 0.16, 0.2, 0.16, 0.045);
        ring(level, position, 0.5, ripple(), 16, 0.025);
        sound(level, envoy.blockPosition(), SoundEvents.TRIDENT_RETURN, 0.52F, 1.55F);
    }

    public static void envoyRiftTelegraph(NihilityCreature envoy, LivingEntity target) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = envoy.position().add(0.0, envoy.getBbHeight() * 0.5, 0.0);
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.18, 0.0);
        line(level, from, to, ripple(), 0.55);
        ring(level, to, 0.42, shard(), 10, 0.0);
    }

    public static void envoyRiftDirection(NihilityCreature envoy, LivingEntity target,
                                          Vec3 from, Vec3 destination, boolean moved) {
        if (!(envoy.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.48, 0.0);
        if (moved) {
            line(level, from.add(0.0, target.getBbHeight() * 0.48, 0.0),
                destination.add(0.0, target.getBbHeight() * 0.48, 0.0), ripple(), 0.3);
            cloud(level, center, mote(), 18, 0.28, 0.42, 0.28, 0.035);
            sound(level, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, 0.48F, 1.22F);
        } else {
            ring(level, center, 0.72, shard(), 18, -0.02);
            sound(level, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, 0.36F, 0.72F);
        }
    }

    private static void grandEnvoyShieldNodes(ServerLevel level, Vec3 center, int charges, int particlesPerNode) {
        int count = Math.max(0, Math.min(3, charges));
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / 3.0 - Math.PI / 2.0;
            Vec3 node = center.add(Math.cos(angle) * 1.05, 0.18 + i * 0.08, Math.sin(angle) * 1.05);
            cloud(level, node, i == 0 ? mote() : shard(), particlesPerNode,
                0.06, 0.08, 0.06, 0.012);
        }
    }

    public static void gravityFieldPulse(ServerLevel level, Vec3 center, int age, int targetCount) {
        if (age % 10 == 1) {
            ring(level, center, 0.8 + Math.min(2.4, age * 0.025), ripple(), 24, 0.03);
            cloud(level, center, shard(), Math.min(14, 5 + targetCount), 0.35, 0.18, 0.35, 0.035);
        }
        if (age % 20 == 1) {
            sound(level, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.25F, 0.65F);
        }
    }

    public static void vaultOpen(ServerLevel level, BlockPos pos, int vaultCount) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.15, 0.0);
        int scale = Math.min(4, Math.max(1, vaultCount));
        cloud(level, center, mote(), 14 + scale * 3, 0.42, 0.28, 0.42, 0.04);
        cloud(level, center.add(0.0, 0.58, 0.0), shard(), 10 + scale * 2, 0.22, 0.2, 0.22, 0.025);
        ring(level, center, 0.78 + scale * 0.08, ripple(), 22 + scale * 5, 0.018);
        ring(level, center.add(0.0, 0.5, 0.0), 0.48 + scale * 0.06, mote(), 18 + scale * 4, 0.018);
        beam(level, center.add(0.0, 0.25, 0.0), 5 + scale, shard());
        sound(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.55F, 1.05F + scale * 0.04F);
        sound(level, pos, SoundEvents.END_PORTAL_FRAME_FILL, 0.25F, 0.72F + scale * 0.04F);
    }

    public static void vaultUpgrade(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.25, 0.0);
        cloud(level, center, mote(), 22, 0.36, 0.3, 0.36, 0.045);
        cloud(level, center, shard(), 16, 0.5, 0.28, 0.5, 0.03);
        ring(level, center, 1.05, ripple(), 48, 0.035);
        ring(level, center.add(0.0, 0.45, 0.0), 0.72, mote(), 24, 0.015);
        beam(level, center.add(0.0, 0.2, 0.0), 7, shard());
        sound(level, pos, SoundEvents.BEACON_POWER_SELECT, 0.55F, 1.25F);
        sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.35F, 1.15F);
    }

    public static void barrierPulse(Player player, int charges) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        double radius = 0.78 + Math.min(8, charges) * 0.025;
        ring(level, center, radius, ripple(), 14 + Math.min(8, charges), 0.006);
    }

    public static void barrierImpact(Player player, Vec3 impact) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        cloud(level, impact, shard(), 14, 0.15, 0.15, 0.15, 0.035);
        ring(level, impact, 0.42, ripple(), 12, 0.025);
        sound(level, BlockPos.containing(impact), SoundEvents.SHIELD_BLOCK.value(), 0.55F, 1.45F);
    }

    public static void barrierBreak(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        cloud(level, center, shard(), 36, 0.65, 0.65, 0.65, 0.06);
        ring(level, center, 1.25, ripple(), 30, 0.045);
        sound(level, player.blockPosition(), SoundEvents.GLASS_BREAK, 0.75F, 0.65F);
    }

    public static void nullificationPulse(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.9, 0.0);
        ring(level, center, 0.68, ripple(), 12, -0.008);
        cloud(level, center, mote(), 4, 0.18, 0.3, 0.18, 0.008);
    }

    public static void stasisPulse(net.minecraft.world.entity.LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        ring(level, center, Math.max(0.45, target.getBbWidth() * 0.75), ripple(), 10, 0.0);
        cloud(level, center, shard(), 3, target.getBbWidth() * 0.25,
            target.getBbHeight() * 0.25, target.getBbWidth() * 0.25, 0.0);
    }

    public static void soulShieldImpact(Player player, float absorbed) {
        if (!(player.level() instanceof ServerLevel level) || absorbed <= 0.0F) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        cloud(level, center, mote(), Math.min(18, 4 + Math.round(absorbed * 2.0F)), 0.35, 0.45, 0.35, 0.02);
    }

    public static void riftFieldPulse(ServerLevel level, Vec3 center, int age, int targetCount) {
        if (age % 6 == 1) {
            double radius = 0.55 + Math.min(2.3, age * 0.018);
            ring(level, center, radius, ripple(), 18, -0.025);
            cloud(level, center, shard(), Math.min(12, 3 + targetCount), 0.35, 0.25, 0.35, 0.045);
        }
    }

    public static void drumPulse(Player player, int hits) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.45, 0.0);
        ring(level, center, 2.2 + Math.min(3, hits) * 0.35, abyss(), 36, 0.035);
        cloud(level, center, abyss(), 12 + Math.min(20, hits * 2), 0.55, 0.18, 0.55, 0.035);
        sound(level, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, 0.55F, 0.72F);
    }

    private static ParticleOptions mote() {
        return ModParticles.NIHILITY_MOTE.get();
    }

    private static ParticleOptions ripple() {
        return ModParticles.NIHILITY_RIPPLE.get();
    }

    private static ParticleOptions shard() {
        return ModParticles.NIHILITY_SHARD.get();
    }

    private static ParticleOptions abyss() {
        return ModParticles.ABYSS_EMBER.get();
    }

    private static ParticleOptions chaos() {
        return ModParticles.CHAOS_GLINT.get();
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.sendParticles(particle, x, center.y, z, 1, vx, 0.01, vz, speed);
        }
    }

    private static void cloud(ServerLevel level, Vec3 center, ParticleOptions particle, int count,
                              double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
    }

    private static void beam(ServerLevel level, Vec3 center, int height, ParticleOptions particle) {
        for (int i = 0; i < height; i++) {
            Vec3 point = center.add(0.0, i * 0.18, 0.0);
            cloud(level, point, particle, 2, 0.04, 0.02, 0.04, 0.01);
        }
    }

    private static void line(ServerLevel level, Vec3 start, Vec3 end,
                             ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 1.0E-5) {
            return;
        }
        int count = Math.max(1, (int) Math.ceil(length / Math.max(0.1, spacing)));
        for (int i = 0; i <= count; i++) {
            Vec3 point = start.add(delta.scale(i / (double) count));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private NihilityVisualEffects() {
    }
}
