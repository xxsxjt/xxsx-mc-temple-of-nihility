package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
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
        state.ritualShieldCharges = entity instanceof NihilityGrandEnvoy envoy
            ? envoy.getRitualShieldCharges()
            : 0;
        state.projectileGuarding = entity instanceof NihilityFollower follower
            && follower.isProjectileGuarding();
        state.projectileDeflecting = entity instanceof NihilityEnvoy envoy
            && envoy.isProjectileDeflecting();
        if (entity instanceof NihilitySaint saint) {
            state.saintSwordsDeployed = saint.areSwordsDeployed();
            state.saintSwordsGuardTicks = saint.getSwordsGuardTicks();
            state.saintWingShieldTicks = saint.getWingShieldTicks();
            state.saintWingSalvoTicks = saint.getWingSalvoTicks();
            state.saintAstrolabeCharge = saint.getAstrolabeCharge();
            state.saintSwordLiberation = saint.isSwordLiberation();
        } else {
            state.saintSwordsDeployed = false;
            state.saintSwordsGuardTicks = 0;
            state.saintWingShieldTicks = 0;
            state.saintWingSalvoTicks = 0;
            state.saintAstrolabeCharge = 0;
            state.saintSwordLiberation = false;
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
            case 2 -> 1.04f;
            case 3 -> 1.08f;
            case 4 -> 1.16f;
            default -> 1.30f;
        };
        float lift = tier >= 3 ? -0.16f - (tier - 3) * 0.08f : 0.0f;
        if (tier >= 5) {
            // Saint floats visibly higher as a sovereign spellcaster, ascending further in Sword Liberation / Salvo
            lift -= 0.14f;
            if (state.saintSwordLiberation) {
                lift -= 0.18f;
            }
            if (state.saintWingSalvoTicks > 0) {
                lift -= 0.10f;
            }
        }
        float bob = Mth.sin(state.ageInTicks * 0.09f + tier) * (tier >= 5 ? 0.085f : (tier >= 3 ? 0.055f : 0.02f));
        poseStack.translate(0.0f, lift + bob, 0.0f);
        poseStack.scale(s, s, s);
    }
}
