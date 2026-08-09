package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
import com.templenihility.entity.NihilityCreature;
import com.templenihility.entity.NihilityGrandEnvoy;
import com.templenihility.entity.NihilityFollower;
import com.templenihility.entity.NihilityEnvoy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class NihilityCreatureRenderer<T extends NihilityCreature>
        extends MobRenderer<T, NihilityRenderState, NihilityCreatureModel> {
    private final Identifier texture;

    public NihilityCreatureRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new NihilityCreatureModel(context.bakeLayer(NihilityCreatureModel.LAYER_LOCATION)), 0.45f);
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
        state.alert = state.disposition.blocksTrading();
        state.ritualShieldCharges = entity instanceof NihilityGrandEnvoy envoy
            ? envoy.getRitualShieldCharges()
            : 0;
        state.projectileGuarding = entity instanceof NihilityFollower follower
            && follower.isProjectileGuarding();
        state.projectileDeflecting = entity instanceof NihilityEnvoy envoy
            && envoy.isProjectileDeflecting();
    }

    @Override
    public Identifier getTextureLocation(NihilityRenderState state) {
        return texture;
    }

    @Override
    protected void scale(NihilityRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0f, Mth.sin(state.ageInTicks * 0.08f) * 0.04f, 0.0f);
    }
}
