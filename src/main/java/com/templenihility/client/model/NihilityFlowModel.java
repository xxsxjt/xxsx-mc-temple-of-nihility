package com.templenihility.client.model;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.renderer.NihilityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Emissive overlay geometry for the moving iridescent flow ribbons. */
public class NihilityFlowModel extends EntityModel<NihilityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_creature_flow"),
        "main"
    );

    private final ModelPart leftFlow;
    private final ModelPart rightFlow;
    private final ModelPart lowerLeft;
    private final ModelPart lowerRight;
    private final ModelPart crownLeft;
    private final ModelPart crownRight;
    private final ModelPart crownTop;

    public NihilityFlowModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucentEmissive);
        this.leftFlow = root.getChild("left_flow");
        this.rightFlow = root.getChild("right_flow");
        this.lowerLeft = root.getChild("lower_left");
        this.lowerRight = root.getChild("lower_right");
        this.crownLeft = root.getChild("crown_left");
        this.crownRight = root.getChild("crown_right");
        this.crownTop = root.getChild("crown_top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation edge = new CubeDeformation(0.035f);

        root.addOrReplaceChild("left_flow", CubeListBuilder.create()
                .texOffs(0, 160).addBox(-1.1f, 0.0f, -0.4f, 2.2f, 5.0f, 1.0f, edge)
                .texOffs(16, 160).addBox(-1.0f, 5.0f, -0.35f, 2.2f, 5.0f, 1.0f, edge)
                .texOffs(32, 160).addBox(-0.9f, 10.0f, -0.3f, 2.2f, 5.0f, 1.0f, edge)
            , PartPose.offset(-5.2f, 9.0f, 2.6f));

        root.addOrReplaceChild("right_flow", CubeListBuilder.create()
                .texOffs(48, 160).addBox(-1.1f, 0.0f, -0.4f, 2.2f, 5.0f, 1.0f, edge)
                .texOffs(64, 160).addBox(-1.0f, 5.0f, -0.35f, 2.2f, 5.0f, 1.0f, edge)
                .texOffs(80, 160).addBox(-0.9f, 10.0f, -0.3f, 2.2f, 5.0f, 1.0f, edge)
            , PartPose.offset(5.2f, 9.0f, 2.6f));

        root.addOrReplaceChild("lower_left", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-1.0f, 0.0f, -0.4f, 2.0f, 4.4f, 1.0f, edge)
                .texOffs(16, 192).addBox(-0.9f, 4.4f, -0.35f, 2.0f, 4.4f, 1.0f, edge)
                .texOffs(32, 192).addBox(-0.8f, 8.8f, -0.3f, 2.0f, 4.4f, 1.0f, edge)
            , PartPose.offset(-1.8f, 23.0f, 2.2f));

        root.addOrReplaceChild("lower_right", CubeListBuilder.create()
                .texOffs(48, 192).addBox(-1.0f, 0.0f, -0.4f, 2.0f, 4.4f, 1.0f, edge)
                .texOffs(64, 192).addBox(-0.9f, 4.4f, -0.35f, 2.0f, 4.4f, 1.0f, edge)
                .texOffs(80, 192).addBox(-0.8f, 8.8f, -0.3f, 2.0f, 4.4f, 1.0f, edge)
            , PartPose.offset(1.8f, 23.0f, 2.2f));

        root.addOrReplaceChild("seal_left", CubeListBuilder.create()
                .texOffs(0, 224).addBox(-0.5f, -1.5f, -0.5f, 1.2f, 3.2f, 1.2f, edge)
                .texOffs(8, 224).addBox(-0.5f, 1.7f, -0.5f, 1.2f, 2.2f, 1.2f, edge)
            , PartPose.offset(-7.6f, 15.0f, 1.2f));

        root.addOrReplaceChild("seal_right", CubeListBuilder.create()
                .texOffs(16, 224).addBox(-0.7f, -1.5f, -0.5f, 1.2f, 3.2f, 1.2f, edge)
                .texOffs(24, 224).addBox(-0.7f, 1.7f, -0.5f, 1.2f, 2.2f, 1.2f, edge)
            , PartPose.offset(7.6f, 15.0f, 1.2f));

        root.addOrReplaceChild("crown_left", CubeListBuilder.create()
                .texOffs(32, 224).addBox(-5.2f, -1.4f, -0.6f, 3.2f, 1.2f, 1.2f, edge)
                .texOffs(48, 224).addBox(-6.0f, -3.4f, -0.6f, 1.2f, 2.0f, 1.2f, edge)
            , PartPose.offset(0f, 3.0f, 0f));

        root.addOrReplaceChild("crown_right", CubeListBuilder.create()
                .texOffs(56, 224).addBox(2.0f, -1.4f, -0.6f, 3.2f, 1.2f, 1.2f, edge)
                .texOffs(72, 224).addBox(4.8f, -3.4f, -0.6f, 1.2f, 2.0f, 1.2f, edge)
            , PartPose.offset(0f, 3.0f, 0f));

        root.addOrReplaceChild("crown_top", CubeListBuilder.create()
                .texOffs(80, 224).addBox(-0.6f, -4.2f, -0.6f, 1.2f, 2.8f, 1.2f, edge)
            , PartPose.offset(0f, 3.0f, 0f));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(NihilityRenderState state) {
        super.setupAnim(state);
        int tier = Math.max(1, Math.min(5, state.tier));
        leftFlow.visible = true;
        rightFlow.visible = true;
        lowerLeft.visible = tier >= 2;
        lowerRight.visible = tier >= 2;
        crownLeft.visible = tier >= 4;
        crownRight.visible = tier >= 4;
        crownTop.visible = tier >= 5;

        float phase = state.ageInTicks * (0.06f + tier * 0.008f);
        leftFlow.yRot = 0.38f + Mth.sin(phase) * 0.18f;
        rightFlow.yRot = -0.38f - Mth.sin(phase * 0.87f + 1.3f) * 0.18f;
        leftFlow.zRot = -0.12f + Mth.cos(phase * 0.74f) * 0.08f;
        rightFlow.zRot = 0.12f - Mth.cos(phase * 0.68f + 0.5f) * 0.08f;
        leftFlow.y = 2.0f + Mth.sin(phase * 0.9f) * 0.24f;
        rightFlow.y = 2.0f + Mth.cos(phase * 0.82f) * 0.24f;
        lowerLeft.yRot = 0.22f + Mth.sin(phase * 0.8f + 0.7f) * 0.22f;
        lowerRight.yRot = -0.22f - Mth.cos(phase * 0.76f) * 0.22f;
        lowerLeft.zRot = Mth.sin(phase * 0.62f) * 0.12f;
        lowerRight.zRot = Mth.cos(phase * 0.58f) * 0.12f;
        crownLeft.yRot = phase * 0.25f;
        crownRight.yRot = -phase * 0.25f;
        crownTop.yRot = phase * 0.38f;
        crownTop.xRot = Mth.sin(phase * 0.7f) * 0.14f;
    }
}
