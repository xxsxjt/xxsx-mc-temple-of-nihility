package com.templenihility.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class NihilityVisualEffects {
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
        VOID_BEACON
    }

    public static void itemUse(Player player, Burst burst) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        switch (burst) {
            case SHADOW -> {
                cloud(level, center, ParticleTypes.PORTAL, 34, 0.52, 0.42, 0.52, 0.18);
                cloud(level, center.add(player.getLookAngle().scale(1.1)), ParticleTypes.REVERSE_PORTAL, 18, 0.2, 0.18, 0.2, 0.05);
                ring(level, center, 1.15, ParticleTypes.SMOKE, 26, 0.015);
                sound(level, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 0.55F, 1.35F);
            }
            case PURIFY -> {
                ring(level, center, 1.6, ParticleTypes.ENCHANT, 48, 0.035);
                ring(level, center, 2.45, ParticleTypes.SOUL_FIRE_FLAME, 54, 0.025);
                cloud(level, center, ParticleTypes.END_ROD, 20, 0.4, 0.18, 0.4, 0.025);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.25F);
                sound(level, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.45F, 1.2F);
            }
            case ECHO -> {
                ring(level, center, 2.2, ParticleTypes.ENCHANT, 58, 0.02);
                cloud(level, center.add(0.0, 0.45, 0.0), ParticleTypes.ELECTRIC_SPARK, 18, 0.35, 0.2, 0.35, 0.015);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.65F, 1.45F);
            }
            case BARRIER -> {
                ring(level, center, 1.25, ParticleTypes.END_ROD, 44, 0.02);
                ring(level, center, 1.85, ParticleTypes.ENCHANT, 54, 0.025);
                cloud(level, center, ParticleTypes.WITCH, 12, 0.32, 0.22, 0.32, 0.02);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.75F, 1.05F);
            }
            case PHASE -> {
                Vec3 look = player.getLookAngle().normalize();
                for (int i = 0; i < 8; i++) {
                    Vec3 point = center.subtract(look.scale(i * 0.35));
                    cloud(level, point, ParticleTypes.REVERSE_PORTAL, 3, 0.08, 0.06, 0.08, 0.02);
                }
                cloud(level, center.add(look.scale(1.4)), ParticleTypes.END_ROD, 16, 0.16, 0.12, 0.16, 0.045);
                sound(level, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.5F, 1.35F);
            }
            case GRAVITY -> {
                Vec3 field = center.add(player.getLookAngle().normalize().scale(8.0)).add(0.0, 0.4, 0.0);
                ring(level, field, 1.25, ParticleTypes.REVERSE_PORTAL, 46, 0.045);
                cloud(level, field, ParticleTypes.PORTAL, 52, 0.65, 0.32, 0.65, 0.18);
                sound(level, BlockPos.containing(field), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.7F, 0.75F);
            }
            case WAR -> {
                ring(level, center, 2.6, ParticleTypes.ELECTRIC_SPARK, 64, 0.04);
                ring(level, center, 3.35, ParticleTypes.SOUL_FIRE_FLAME, 72, 0.02);
                cloud(level, center, ParticleTypes.WITCH, 24, 0.55, 0.22, 0.55, 0.02);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.72F);
            }
            case HEAL -> {
                cloud(level, center, ParticleTypes.HEART, 8, 0.25, 0.25, 0.25, 0.02);
                cloud(level, center, ParticleTypes.ENCHANT, 32, 0.42, 0.2, 0.42, 0.035);
                sound(level, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.55F, 1.55F);
            }
            case NULLIFY -> {
                ring(level, center, 1.85, ParticleTypes.SMOKE, 54, 0.025);
                cloud(level, center, ParticleTypes.REVERSE_PORTAL, 40, 0.45, 0.32, 0.45, 0.08);
                sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 0.65F);
            }
            case STASIS -> {
                ring(level, center, 2.1, ParticleTypes.ELECTRIC_SPARK, 60, 0.018);
                ring(level, center.add(0.0, 0.35, 0.0), 2.1, ParticleTypes.ENCHANT, 60, 0.012);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.55F, 0.65F);
            }
            case RIFT -> {
                ring(level, center, 1.35, ParticleTypes.REVERSE_PORTAL, 54, 0.06);
                cloud(level, center.add(0.0, 0.2, 0.0), ParticleTypes.PORTAL, 50, 0.5, 0.2, 0.5, 0.22);
                sound(level, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.65F, 0.85F);
            }
            case VOID_BEACON -> {
                for (int y = 0; y < 7; y++) {
                    cloud(level, center.add(0.0, y * 0.45, 0.0), ParticleTypes.END_ROD, 3, 0.09, 0.03, 0.09, 0.01);
                }
                ring(level, center, 2.8, ParticleTypes.ENCHANT, 72, 0.02);
                sound(level, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.45F);
            }
        }
    }

    public static void lanternPulse(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 1.0, 0.0);
        cloud(level, center, ParticleTypes.END_ROD, 8, 0.22, 0.35, 0.22, 0.02);
        cloud(level, center, ParticleTypes.REVERSE_PORTAL, 10, 0.35, 0.25, 0.35, 0.04);
        sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 1.55F);
    }

    public static void voidPhaseTrail(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.95, 0.0);
        cloud(level, center, ParticleTypes.REVERSE_PORTAL, 8, 0.32, 0.45, 0.32, 0.055);
        cloud(level, center.add(0.0, 0.25, 0.0), ParticleTypes.PORTAL, 5, 0.22, 0.25, 0.22, 0.08);
        if (player.tickCount % 32 == 0) {
            ring(level, center, 0.72, ParticleTypes.ENCHANT, 18, 0.012);
        }
    }

    public static void magnetPulse(Player player, int targetCount) {
        if (!(player.level() instanceof ServerLevel level) || targetCount <= 0) {
            return;
        }
        Vec3 center = player.position().add(0.0, 0.75, 0.0);
        int count = Math.min(14, 4 + targetCount / 4);
        cloud(level, center, ParticleTypes.REVERSE_PORTAL, count, 0.45, 0.2, 0.45, 0.08);
        ring(level, center, 0.85, ParticleTypes.ENCHANT, 18, 0.014);
    }

    public static void gravityFieldPulse(ServerLevel level, Vec3 center, int age, int targetCount) {
        if (age % 10 == 1) {
            ring(level, center, 0.8 + Math.min(2.4, age * 0.025), ParticleTypes.REVERSE_PORTAL, 28, 0.035);
            cloud(level, center, ParticleTypes.PORTAL, Math.min(18, 6 + targetCount), 0.35, 0.18, 0.35, 0.12);
        }
        if (age % 20 == 1) {
            sound(level, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.25F, 0.65F);
        }
    }

    public static void vaultOpen(ServerLevel level, BlockPos pos, int vaultCount) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.15, 0.0);
        int scale = Math.min(4, Math.max(1, vaultCount));
        cloud(level, center, ParticleTypes.PORTAL, 22 + scale * 5, 0.42, 0.28, 0.42, 0.14);
        cloud(level, center.add(0.0, 0.58, 0.0), ParticleTypes.REVERSE_PORTAL, 16 + scale * 3, 0.22, 0.2, 0.22, 0.08);
        ring(level, center, 0.78 + scale * 0.08, ParticleTypes.ENCHANT, 32 + scale * 8, 0.025);
        ring(level, center.add(0.0, 0.5, 0.0), 0.48 + scale * 0.06, ParticleTypes.ELECTRIC_SPARK, 18 + scale * 4, 0.018);
        beam(level, center.add(0.0, 0.25, 0.0), 5 + scale, ParticleTypes.END_ROD);
        sound(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.55F, 1.05F + scale * 0.04F);
        sound(level, pos, SoundEvents.END_PORTAL_FRAME_FILL, 0.25F, 0.72F + scale * 0.04F);
    }

    public static void vaultUpgrade(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0, 0.25, 0.0);
        cloud(level, center, ParticleTypes.END_ROD, 22, 0.36, 0.3, 0.36, 0.045);
        cloud(level, center, ParticleTypes.REVERSE_PORTAL, 20, 0.5, 0.28, 0.5, 0.1);
        ring(level, center, 1.05, ParticleTypes.ELECTRIC_SPARK, 48, 0.035);
        ring(level, center.add(0.0, 0.45, 0.0), 0.72, ParticleTypes.ENCHANT, 36, 0.02);
        beam(level, center.add(0.0, 0.2, 0.0), 7, ParticleTypes.END_ROD);
        sound(level, pos, SoundEvents.BEACON_POWER_SELECT, 0.55F, 1.25F);
        sound(level, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.35F, 1.15F);
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

    private static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private NihilityVisualEffects() {
    }
}
