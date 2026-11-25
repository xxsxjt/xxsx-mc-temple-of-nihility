package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.templenihility.TempleNihilityMod;
import com.templenihility.client.model.NihilityCreatureModel;
import com.templenihility.client.model.NihilityFlowModel;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

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
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            flowingTint(state),
            null
        );
    }

    private static int flowingTint(NihilityRenderState state) {
        float pulse = 0.88f + 0.12f * Mth.sin(state.ageInTicks * 0.14f + state.tier);
        int red = Math.round(188 * pulse);
        int green = Math.round(244 * pulse);
        int blue = Math.round(255 * pulse);
        int alpha = 232;
        if (state.disposition == Disposition.ALERT) {
            red = Math.round(255 * pulse);
            green = Math.round(220 * pulse);
            blue = Math.round(145 * pulse);
            alpha = 245;
        } else if (state.disposition == Disposition.CHALLENGE) {
            red = 248;
            green = 252;
            blue = 255;
            alpha = 255;
        } else if (state.disposition == Disposition.HOSTILE) {
            red = 255;
            green = Math.round(132 * pulse);
            blue = Math.round(168 * pulse);
            alpha = 255;
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
