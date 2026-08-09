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
        CubeDeformation edge = new CubeDeformation(0.045f);

        root.addOrReplaceChild("left_flow", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.75f, -1.0f, -0.3f, 1.5f, 15.0f, 0.6f, edge),
            PartPose.offset(-4.0f, 2.0f, 2.0f));
        root.addOrReplaceChild("right_flow", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-0.75f, -1.0f, -0.3f, 1.5f, 15.0f, 0.6f, edge),
            PartPose.offset(4.0f, 2.0f, 2.0f));
        root.addOrReplaceChild("lower_left", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.7f, -1.0f, -0.35f, 1.4f, 12.0f, 0.7f, edge),
            PartPose.offset(-2.8f, 11.0f, 1.65f));
        root.addOrReplaceChild("lower_right", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-0.7f, -1.0f, -0.35f, 1.4f, 12.0f, 0.7f, edge),
            PartPose.offset(2.8f, 11.0f, 1.65f));
        root.addOrReplaceChild("crown_left", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.5f, -9.5f, -0.3f, 3.0f, 0.7f, 0.6f, edge),
            PartPose.ZERO);
        root.addOrReplaceChild("crown_right", CubeListBuilder.create()
                .texOffs(16, 32).addBox(1.5f, -9.5f, -0.3f, 3.0f, 0.7f, 0.6f, edge),
            PartPose.ZERO);
        root.addOrReplaceChild("crown_top", CubeListBuilder.create()
                .texOffs(32, 32).addBox(-0.55f, -12.0f, -0.3f, 1.1f, 2.6f, 0.6f, edge),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
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
