package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
import com.templenihility.entity.NihilityBeliever;
import com.templenihility.entity.NihilityCreature;
import com.templenihility.entity.NihilityEnvoy;
import com.templenihility.entity.NihilityFollower;
import com.templenihility.entity.NihilityGrandEnvoy;
import com.templenihility.entity.NihilitySaint;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class NihilityCreatureRenderer<T extends NihilityCreature>
        extends MobRenderer<T, NihilityRenderState, NihilityCreatureModel> {
    private final Identifier texture;

    public NihilityCreatureRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new NihilityCreatureModel(context.bakeLayer(NihilityCreatureModel.LAYER_LOCATION)), 0.52f);
        this.texture = texture;
        addLayer(new NihilityCreatureFlowLayer(this,
            new NihilityFlowModel(context.bakeLayer(NihilityFlowModel.LAYER_LOCATION))));
    }

    @Override
    public NihilityRenderState createRenderState() {
        return new NihilityRenderState();
    }

    @Override
    public void extractRenderState(T entity, NihilityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tier = entity.getTier();
        state.disposition = entity.getDisposition();
        state.action = entity.getCreatureAction();
        state.actionPhase = entity.getCreatureActionPhase();
        state.actionElapsed = Math.max(0.0F,
            entity.level().getGameTime() - entity.getCreatureActionStartGameTime() + partialTicks);
        state.attackSwing = entity.getAttackAnim(partialTicks);
        state.alert = state.disposition.blocksTrading();

        // Reset archetype-specific states cleanly before populating
        state.believerCastingTicks = 0;
        state.believerPulseTicks = 0;
        state.followerHalberdStance = 0;
        state.followerStanceTicks = 0;
        state.projectileGuarding = false;
        state.projectileDeflecting = false;
        state.envoyMonolithsDeployed = false;
        state.envoyBackstabTicks = 0;
        state.ritualShieldCharges = 0;
        state.grandEnvoyDeployedShields = 0;
        state.grandEnvoyCommandTicks = 0;
        state.grandEnvoyDomainActive = false;
        state.saintSwordsDeployed = false;
        state.saintSwordsGuardTicks = 0;
        state.saintWingShieldTicks = 0;
        state.saintWingSalvoTicks = 0;
        state.saintAstrolabeCharge = 0;
        state.saintSwordLiberation = false;

        if (entity instanceof NihilityBeliever believer) {
            state.believerCastingTicks = believer.getCastingTicks();
            state.believerPulseTicks = believer.getPulseTicks();
        } else if (entity instanceof NihilityFollower follower) {
            state.projectileGuarding = follower.isProjectileGuarding();
            state.followerHalberdStance = follower.getHalberdStance();
            state.followerStanceTicks = follower.getStanceTicks();
        } else if (entity instanceof NihilityEnvoy envoy) {
            state.projectileDeflecting = envoy.isProjectileDeflecting();
            state.envoyMonolithsDeployed = envoy.areMonolithsDeployed();
            state.envoyBackstabTicks = envoy.getBackstabTicks();
        } else if (entity instanceof NihilityGrandEnvoy grand) {
            state.ritualShieldCharges = grand.getRitualShieldCharges();
            state.grandEnvoyDeployedShields = grand.getDeployedShields();
            state.grandEnvoyCommandTicks = grand.getCommandTicks();
            state.grandEnvoyDomainActive = grand.isDomainActiveClient();
        } else if (entity instanceof NihilitySaint saint) {
            state.saintSwordsDeployed = saint.areSwordsDeployed();
            state.saintSwordsGuardTicks = saint.getSwordsGuardTicks();
            state.saintWingShieldTicks = saint.getWingShieldTicks();
            state.saintWingSalvoTicks = saint.getWingSalvoTicks();
            state.saintAstrolabeCharge = saint.getAstrolabeCharge();
            state.saintSwordLiberation = saint.isSwordLiberation();
        }
    }

    @Override
    public Identifier getTextureLocation(NihilityRenderState state) {
        return texture;
    }

    @Override
    protected void scale(NihilityRenderState state, PoseStack poseStack) {
        int tier = Math.max(1, Math.min(5, state.tier));
        float s = switch (tier) {
            case 1 -> 0.96f;
            case 2 -> 1.05f;
            case 3 -> 1.08f;
            case 4 -> 1.18f;
            default -> 1.30f;
        };
        // Tier 2 Follower stays planted like a heavy armored vanguard; Tiers 3..5 float progressively higher;
        // Tier 1 Believer rises gently while channeling crystal spells!
        float lift = switch (tier) {
            case 1 -> state.believerCastingTicks > 0 ? -0.09f : 0.0f;
            case 2 -> 0.0f;
            case 3 -> -0.20f;
            case 4 -> state.grandEnvoyDomainActive ? -0.34f : -0.26f;
            default -> -0.32f;
        };
        if (tier >= 5) {
            if (state.saintSwordLiberation) {
                lift -= 0.18f;
            }
            if (state.saintWingSalvoTicks > 0) {
                lift -= 0.10f;
            }
        }
        float bobAmp = switch (tier) {
            case 1 -> state.believerCastingTicks > 0 ? 0.035f : 0.01f;
            case 2 -> 0.0f;
            case 3 -> 0.055f;
            case 4 -> 0.065f;
            default -> 0.085f;
        };
        float bob = bobAmp > 0.0f ? Mth.sin(state.ageInTicks * 0.09f + tier) * bobAmp : 0.0f;
        poseStack.translate(0.0f, lift + bob, 0.0f);
        poseStack.scale(s, s, s);
    }
}
