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

public class NihilityCreatureModel extends EntityModel<NihilityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_creature"),
        "main"
    );

    private final ModelPart head;
    private final ModelPart halo;
    private final ModelPart body;
    private final ModelPart core;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public NihilityCreatureModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.head = root.getChild("head");
        this.halo = root.getChild("halo");
        this.body = root.getChild("body");
        this.core = this.body.getChild("core");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation glow = new CubeDeformation(0.18f);

        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
                .texOffs(32, 0).addBox(-4.5f, -8.5f, -4.5f, 9.0f, 9.0f, 9.0f, glow),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "halo",
            CubeListBuilder.create()
                .texOffs(0, 48).addBox(-6.0f, -1.0f, -0.5f, 12.0f, 2.0f, 1.0f)
                .texOffs(26, 48).addBox(-1.0f, -6.0f, -0.5f, 2.0f, 12.0f, 1.0f),
            PartPose.offset(0.0f, -9.5f, 0.0f)
        );

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 13.0f, 4.0f)
                .texOffs(40, 16).addBox(-4.5f, 2.0f, -2.5f, 9.0f, 10.0f, 5.0f, glow),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );

        body.addOrReplaceChild(
            "core",
            CubeListBuilder.create()
                .texOffs(0, 40).addBox(-2.0f, 2.5f, -2.75f, 4.0f, 4.0f, 1.0f),
            PartPose.ZERO
        );

        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create()
                .texOffs(0, 20).addBox(-2.25f, -1.5f, -2.0f, 4.0f, 13.0f, 4.0f),
            PartPose.offset(-6.0f, 2.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create()
                .texOffs(0, 20).mirror().addBox(-1.75f, -1.5f, -2.0f, 4.0f, 13.0f, 4.0f),
            PartPose.offset(6.0f, 2.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(32, 32).addBox(-2.0f, 0.0f, -1.75f, 4.0f, 12.0f, 4.0f),
            PartPose.offset(-2.0f, 12.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create()
                .texOffs(32, 32).mirror().addBox(-2.0f, 0.0f, -1.75f, 4.0f, 12.0f, 4.0f),
            PartPose.offset(2.0f, 12.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "right_wing",
            CubeListBuilder.create()
                .texOffs(48, 32).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 14.0f, 1.0f),
            PartPose.offsetAndRotation(-4.5f, 2.0f, 2.5f, 0.0f, 0.55f, -0.25f)
        );

        root.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(54, 32).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 14.0f, 1.0f),
            PartPose.offsetAndRotation(4.5f, 2.0f, 2.5f, 0.0f, -0.55f, 0.25f)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NihilityRenderState state) {
        super.setupAnim(state);
        float tierPulse = 0.04f + state.tier * 0.008f;
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0f);

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.y += Mth.sin(age * 0.09f) * 0.35f;

        this.body.y += Mth.sin(age * 0.08f) * 0.25f;
        this.core.xScale = 1.0f + Mth.sin(age * 0.18f) * tierPulse;
        this.core.yScale = 1.0f + Mth.cos(age * 0.18f) * tierPulse;

        this.halo.yRot = age * 0.035f;
        this.halo.zRot = Mth.sin(age * 0.05f) * 0.12f;

        this.rightArm.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * speed * 0.75f - 0.12f;
        this.leftArm.xRot = Mth.cos(walk * 0.6662f) * speed * 0.75f - 0.12f;
        this.rightArm.zRot = -0.12f + Mth.sin(age * 0.08f) * 0.08f;
        this.leftArm.zRot = 0.12f - Mth.sin(age * 0.08f) * 0.08f;

        this.rightLeg.xRot = Mth.cos(walk * 0.6662f) * 1.1f * speed;
        this.leftLeg.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * 1.1f * speed;

        float wing = 0.45f + Mth.sin(age * 0.12f) * 0.18f;
        this.rightWing.yRot = wing;
        this.leftWing.yRot = -wing;
    }
}
