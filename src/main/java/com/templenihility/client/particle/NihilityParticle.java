package com.templenihility.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class NihilityParticle extends SingleQuadParticle {
    public enum Style {
        MOTE,
        RIPPLE,
        SHARD,
        ABYSS,
        CHAOS
    }

    private final SpriteSet sprites;
    private final Style style;
    private final float baseSize;
    private final float spin;
    private final double driftPhase;

    private NihilityParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed,
                             RandomSource random, SpriteSet sprites, Style style) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.get(random));
        this.sprites = sprites;
        this.style = style;
        this.hasPhysics = false;
        this.driftPhase = random.nextDouble() * Math.PI * 2.0;

        switch (style) {
            case MOTE -> {
                this.lifetime = 24 + random.nextInt(12);
                this.baseSize = 0.10F + random.nextFloat() * 0.04F;
                this.spin = (random.nextFloat() - 0.5F) * 0.14F;
                this.friction = 0.93F;
                setColor(0.76F + random.nextFloat() * 0.22F,
                    0.94F + random.nextFloat() * 0.06F, 1.0F);
            }
            case RIPPLE -> {
                this.lifetime = 22 + random.nextInt(8);
                this.baseSize = 0.115F + random.nextFloat() * 0.04F;
                this.spin = (random.nextFloat() - 0.5F) * 0.06F;
                this.friction = 0.88F;
                setColor(0.68F, 0.95F, 1.0F);
            }
            case SHARD -> {
                this.lifetime = 22 + random.nextInt(10);
                this.baseSize = 0.125F + random.nextFloat() * 0.045F;
                this.spin = (random.nextBoolean() ? 1.0F : -1.0F) * (0.14F + random.nextFloat() * 0.09F);
                this.friction = 0.91F;
                setColor(0.86F + random.nextFloat() * 0.14F,
                    0.95F + random.nextFloat() * 0.05F, 1.0F);
            }
            case ABYSS -> {
                this.lifetime = 26 + random.nextInt(14);
                this.baseSize = 0.115F + random.nextFloat() * 0.045F;
                this.spin = (random.nextBoolean() ? 1.0F : -1.0F) * (0.08F + random.nextFloat() * 0.08F);
                this.friction = 0.92F;
                if (random.nextFloat() < 0.55F) {
                    setColor(0.92F, 0.22F + random.nextFloat() * 0.12F, 0.34F);
                } else {
                    setColor(0.98F, 0.82F + random.nextFloat() * 0.14F, 0.44F);
                }
            }
            case CHAOS -> {
                this.lifetime = 24 + random.nextInt(12);
                this.baseSize = 0.115F + random.nextFloat() * 0.045F;
                this.spin = (random.nextFloat() - 0.5F) * 0.24F;
                this.friction = 0.90F;
                float selector = random.nextFloat();
                if (selector < 0.35F) {
                    setColor(0.96F, 0.86F, 0.52F);
                } else if (selector < 0.70F) {
                    setColor(0.45F, 0.94F, 1.0F);
                } else {
                    setColor(0.92F, 0.58F, 1.0F);
                }
            }
            default -> throw new IllegalStateException("Unhandled style " + style);
        }
        this.quadSize = this.baseSize;
        this.alpha = style == Style.RIPPLE ? 0.62F : 0.78F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        float progress = this.age / (float) this.lifetime;
        switch (style) {
            case MOTE -> {
                double angle = driftPhase + this.age * 0.22;
                this.xd += Math.cos(angle) * 0.0014;
                this.zd += Math.sin(angle) * 0.0014;
                this.yd += 0.0016;
                this.quadSize = baseSize * (0.78F + (float) Math.sin(progress * Math.PI) * 0.42F);
            }
            case RIPPLE -> {
                this.xd *= 0.62;
                this.yd *= 0.62;
                this.zd *= 0.62;
                this.quadSize = baseSize * (0.62F + progress * 0.83F);
            }
            case SHARD -> {
                this.yd += 0.0018;
                this.quadSize = baseSize * (1.08F - progress * 0.45F);
            }
            case ABYSS -> {
                this.yd += 0.0042;
                this.xd += Math.sin(driftPhase + age * 0.2) * 0.0018;
                this.zd += Math.cos(driftPhase + age * 0.2) * 0.0018;
                this.quadSize = baseSize * (0.75F + (float) Math.sin(progress * Math.PI) * 0.55F);
            }
            case CHAOS -> {
                this.xd += Math.sin(driftPhase + age * 0.65) * 0.003;
                this.yd += Math.cos(driftPhase + age * 0.47) * 0.002;
                this.zd += Math.sin(driftPhase * 0.7 + age * 0.53) * 0.003;
                this.quadSize = baseSize * (0.85F + (float) Math.sin(age * 0.9) * 0.2F);
            }
        }
        move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.roll += spin;
        float fade = (1.0F - progress) * (1.0F - progress * 0.35F);
        setAlpha(fade * (style == Style.RIPPLE ? 0.62F : 0.78F));
        setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Provider(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed,
                                       RandomSource random) {
            return new NihilityParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                random, sprites, style);
        }
    }
}
