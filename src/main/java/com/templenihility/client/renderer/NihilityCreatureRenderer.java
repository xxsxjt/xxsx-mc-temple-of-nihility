package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.entity.NihilityCreature;
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
    }

    @Override
    public NihilityRenderState createRenderState() {
        return new NihilityRenderState();
    }

    @Override
    public void extractRenderState(T entity, NihilityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tier = entity.getTier();
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
