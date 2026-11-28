package com.templenihility.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.templenihility.TempleNihilityMod;
import com.templenihility.client.model.VoidSpellModel;
import com.templenihility.entity.VoidSpellEntity;
import com.templenihility.entity.VoidSpellEntity.SpellKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class VoidSpellRenderer extends EntityRenderer<VoidSpellEntity, VoidSpellRenderState> {
    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/entity/nihility_saint.png");
    private static final Identifier FLOW_TEXTURE = Identifier.fromNamespaceAndPath(
        TempleNihilityMod.MOD_ID, "textures/entity/nihility_flow.png");

    private final VoidSpellModel model;

    public VoidSpellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VoidSpellModel(context.bakeLayer(VoidSpellModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public VoidSpellRenderState createRenderState() {
        return new VoidSpellRenderState();
    }

    @Override
    public void extractRenderState(VoidSpellEntity entity, VoidSpellRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.getSpellKind();
        state.yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.returning = entity.isReturning();
        state.dropping = entity.isDropping();
    }

    @Override
    public void submit(VoidSpellRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.kind != SpellKind.JUDGMENT_LANCE) {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));
        }
        float scale = switch (state.kind) {
            case FLOATING_SWORD -> 1.32f;
            case WING_FEATHER -> 1.05f;
            case JUDGMENT_LANCE -> state.dropping ? 1.55f : 1.15f;
        };
        poseStack.scale(scale, scale, scale);

        model.setupAnim(state);
        submitNodeCollector.submitModel(
            model,
            state,
            poseStack,
            RenderTypes.entityCutout(BASE_TEXTURE),
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF,
            null
        );
        int auraTint = state.kind == SpellKind.JUDGMENT_LANCE
            ? 0xF0FFE69E
            : (state.returning ? 0xEEB5F8FF : 0xEE7CEEFF);
        poseStack.scale(1.06f, 1.06f, 1.06f);
        submitNodeCollector.order(1).submitModel(
            model,
            state,
            poseStack,
            RenderTypes.entityTranslucentEmissive(FLOW_TEXTURE),
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            auraTint,
            null
        );
        poseStack.popPose();
    }
}
