package com.templenihility.client.renderer;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders only the moving translucent bands with a separate emissive texture. */
public class NihilityCreatureFlowLayer extends RenderLayer<NihilityRenderState, NihilityCreatureModel> {
    private static final Identifier FLOW_TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/entity/nihility_flow.png");
    private final NihilityFlowModel flowModel;

    public NihilityCreatureFlowLayer(RenderLayerParent<NihilityRenderState, NihilityCreatureModel> parent,
                                     NihilityFlowModel flowModel) {
        super(parent);
        this.flowModel = flowModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       NihilityRenderState state, float yRot, float xRot) {
        flowModel.setupAnim(state);
        submitNodeCollector.order(1).submitModel(
            flowModel,
            state,
            poseStack,
            RenderTypes.entityTranslucentEmissive(FLOW_TEXTURE),
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            flowingTint(state),
            null
        );
    }

    private static int flowingTint(NihilityRenderState state) {
        float phase = state.ageInTicks * 0.045f + state.tier * 0.83f;
        int red = Math.round(232.0f + Mth.sin(phase) * 18.0f);
        int green = Math.round(238.0f + Mth.sin(phase + 2.1f) * 14.0f);
        int blue = Math.round(242.0f + Mth.sin(phase + 4.2f) * 13.0f);
        int alpha = Math.round(184.0f + Mth.sin(phase * 1.37f) * 28.0f)
            + state.ritualShieldCharges * 7;

        if (state.disposition == Disposition.ALERT) {
            red += 12;
            green += 7;
            blue -= 15;
            alpha += 12;
        } else if (state.disposition == Disposition.CHALLENGE) {
            red += 18;
            green += 16;
            blue += 13;
            alpha += 34;
        } else if (state.disposition == Disposition.HOSTILE) {
            red += 20;
            green -= 10;
            blue -= 18;
            alpha += 22;
        }

        return (clampByte(alpha) << 24)
            | (clampByte(red) << 16)
            | (clampByte(green) << 8)
            | clampByte(blue);
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
