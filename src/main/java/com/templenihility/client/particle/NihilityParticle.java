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
                this.lifetime = 22 + random.nextInt(18);
                this.baseSize = 0.09F + random.nextFloat() * 0.08F;
                this.spin = (random.nextFloat() - 0.5F) * 0.10F;
                this.friction = 0.94F;
                setColor(0.62F + random.nextFloat() * 0.25F,
                    0.90F + random.nextFloat() * 0.10F, 0.94F + random.nextFloat() * 0.06F);
            }
            case RIPPLE -> {
                this.lifetime = 18 + random.nextInt(8);
                this.baseSize = 0.22F + random.nextFloat() * 0.12F;
                this.spin = (random.nextFloat() - 0.5F) * 0.035F;
                this.friction = 0.88F;
                setColor(0.70F, 0.96F, 1.0F);
            }
            case SHARD -> {
                this.lifetime = 16 + random.nextInt(14);
                this.baseSize = 0.12F + random.nextFloat() * 0.10F;
                this.spin = (random.nextBoolean() ? 1.0F : -1.0F) * (0.12F + random.nextFloat() * 0.08F);
                this.friction = 0.91F;
                setColor(0.78F + random.nextFloat() * 0.18F,
                    0.88F + random.nextFloat() * 0.12F, 1.0F);
            }
            case ABYSS -> {
                this.lifetime = 24 + random.nextInt(18);
                this.baseSize = 0.11F + random.nextFloat() * 0.11F;
                this.spin = (random.nextBoolean() ? 1.0F : -1.0F) * (0.06F + random.nextFloat() * 0.08F);
                this.friction = 0.92F;
                if (random.nextBoolean()) {
                    setColor(0.42F + random.nextFloat() * 0.20F, 0.035F, 0.07F);
                } else {
                    setColor(0.12F, 0.72F + random.nextFloat() * 0.22F, 0.78F + random.nextFloat() * 0.18F);
                }
            }
            case CHAOS -> {
                this.lifetime = 18 + random.nextInt(20);
                this.baseSize = 0.10F + random.nextFloat() * 0.13F;
                this.spin = (random.nextFloat() - 0.5F) * 0.22F;
                this.friction = 0.90F;
                float selector = random.nextFloat();
                if (selector < 0.33F) {
                    setColor(0.82F, 0.30F, 1.0F);
                } else if (selector < 0.66F) {
                    setColor(0.22F, 0.88F, 1.0F);
                } else {
                    setColor(1.0F, 0.48F, 0.78F);
                }
            }
            default -> throw new IllegalStateException("Unhandled particle style " + style);
        }
        this.quadSize = this.baseSize;
        this.alpha = style == Style.RIPPLE ? 0.72F : 0.90F;
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
                this.xd += Math.cos(angle) * 0.0018;
                this.zd += Math.sin(angle) * 0.0018;
                this.yd += 0.0012;
                this.quadSize = baseSize * (0.72F + (float) Math.sin(progress * Math.PI) * 0.55F);
            }
            case RIPPLE -> {
                this.xd *= 0.55;
                this.yd *= 0.55;
                this.zd *= 0.55;
                this.quadSize = baseSize * (0.55F + progress * 2.5F);
            }
            case SHARD -> {
                this.yd += 0.0025;
                this.quadSize = baseSize * (1.0F - progress * 0.35F);
            }
            case ABYSS -> {
                this.yd += 0.0035;
                this.xd += Math.sin(driftPhase + age * 0.18) * 0.0015;
                this.zd += Math.cos(driftPhase + age * 0.18) * 0.0015;
                this.quadSize = baseSize * (0.7F + (float) Math.sin(progress * Math.PI) * 0.75F);
            }
            case CHAOS -> {
                this.xd += Math.sin(driftPhase + age * 0.65) * 0.004;
                this.yd += Math.cos(driftPhase + age * 0.47) * 0.002;
                this.zd += Math.sin(driftPhase * 0.7 + age * 0.53) * 0.004;
                this.quadSize = baseSize * (0.8F + (float) Math.sin(age * 0.9) * 0.22F);
            }
        }

        move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        this.roll += spin;
        setAlpha((1.0F - progress) * (style == Style.RIPPLE ? 0.72F : 0.92F));
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
