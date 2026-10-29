package com.templenihility.client.model;

import com.templenihility.TempleNihilityMod;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.client.renderer.NihilityRenderState;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
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

/** Transparent humanoid silhouette with tiered flowing void bands. */
public class NihilityCreatureModel extends EntityModel<NihilityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_creature"),
        "main"
    );

    private final ModelPart head;
    private final ModelPart headSheen;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart leftShoulder;
    private final ModelPart rightShoulder;
    private final ModelPart leftFlow;
    private final ModelPart rightFlow;
    private final ModelPart lowerFlowLeft;
    private final ModelPart lowerFlowRight;
    private final ModelPart sealLeft;
    private final ModelPart sealRight;
    private final ModelPart crownLeft;
    private final ModelPart crownRight;
    private final ModelPart crownTop;

    public NihilityCreatureModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.head = root.getChild("head");
        this.headSheen = root.getChild("head_sheen");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.leftShoulder = root.getChild("left_shoulder");
        this.rightShoulder = root.getChild("right_shoulder");
        this.leftFlow = root.getChild("left_flow");
        this.rightFlow = root.getChild("right_flow");
        this.lowerFlowLeft = root.getChild("lower_flow_left");
        this.lowerFlowRight = root.getChild("lower_flow_right");
        this.sealLeft = root.getChild("seal_left");
        this.sealRight = root.getChild("seal_right");
        this.crownLeft = root.getChild("crown_left");
        this.crownRight = root.getChild("crown_right");
        this.crownTop = root.getChild("crown_top");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation fluid = new CubeDeformation(0.10f);

        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0f, -7.0f, -3.0f, 6.0f, 7.0f, 6.0f),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "head_sheen",
            CubeListBuilder.create()
                .texOffs(24, 0).addBox(-3.25f, -7.25f, -3.25f, 6.5f, 7.5f, 6.5f, fluid),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-3.0f, 0.0f, -1.5f, 6.0f, 11.0f, 3.0f)
                .texOffs(40, 16).addBox(-3.25f, 1.0f, -1.75f, 6.5f, 9.0f, 3.5f, fluid),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create()
                .texOffs(0, 20).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 11.0f, 3.0f, fluid),
            PartPose.offset(-4.75f, 1.5f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create()
                .texOffs(0, 20).mirror().addBox(-1.5f, -1.0f, -1.5f, 3.0f, 11.0f, 3.0f, fluid),
            PartPose.offset(4.75f, 1.5f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(32, 32).addBox(-1.5f, 0.0f, -1.35f, 3.0f, 11.0f, 3.0f, fluid),
            PartPose.offset(-1.65f, 11.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create()
                .texOffs(32, 32).mirror().addBox(-1.5f, 0.0f, -1.35f, 3.0f, 11.0f, 3.0f, fluid),
            PartPose.offset(1.65f, 11.0f, 0.0f)
        );

        root.addOrReplaceChild(
            "left_shoulder",
            CubeListBuilder.create()
                .texOffs(8, 36).addBox(-1.0f, -0.8f, -2.1f, 3.0f, 2.0f, 4.2f, fluid),
            PartPose.offset(-4.25f, 1.3f, 0.0f)
        );
        root.addOrReplaceChild(
            "right_shoulder",
            CubeListBuilder.create()
                .texOffs(8, 36).mirror().addBox(-2.0f, -0.8f, -2.1f, 3.0f, 2.0f, 4.2f, fluid),
            PartPose.offset(4.25f, 1.3f, 0.0f)
        );

        // Long transparent bands preserve a humanoid outline while making the motion readable.
        root.addOrReplaceChild(
            "left_flow",
            CubeListBuilder.create()
                .texOffs(48, 32).addBox(-0.8f, -1.0f, -0.35f, 1.6f, 15.0f, 0.7f, new CubeDeformation(0.04f)),
            PartPose.offset(-4.0f, 2.0f, 2.0f)
        );
        root.addOrReplaceChild(
            "right_flow",
            CubeListBuilder.create()
                .texOffs(54, 32).addBox(-0.8f, -1.0f, -0.35f, 1.6f, 15.0f, 0.7f, new CubeDeformation(0.04f)),
            PartPose.offset(4.0f, 2.0f, 2.0f)
        );
        root.addOrReplaceChild(
            "lower_flow_left",
            CubeListBuilder.create()
                .texOffs(0, 48).addBox(-0.75f, -1.0f, -0.4f, 1.5f, 12.0f, 0.8f, new CubeDeformation(0.04f)),
            PartPose.offset(-2.8f, 11.0f, 1.65f)
        );
        root.addOrReplaceChild(
            "lower_flow_right",
            CubeListBuilder.create()
                .texOffs(4, 48).addBox(-0.75f, -1.0f, -0.4f, 1.5f, 12.0f, 0.8f, new CubeDeformation(0.04f)),
            PartPose.offset(2.8f, 11.0f, 1.65f)
        );

        root.addOrReplaceChild(
            "seal_left",
            CubeListBuilder.create()
                .texOffs(16, 48).addBox(-0.75f, -2.0f, -0.3f, 1.5f, 5.0f, 0.6f, new CubeDeformation(0.04f)),
            PartPose.offset(-6.0f, 4.0f, 1.8f)
        );
        root.addOrReplaceChild(
            "seal_right",
            CubeListBuilder.create()
                .texOffs(20, 48).addBox(-0.75f, -2.0f, -0.3f, 1.5f, 5.0f, 0.6f, new CubeDeformation(0.04f)),
            PartPose.offset(6.0f, 4.0f, 1.8f)
        );

        // A broken crown replaces the old solid halo at the two highest tiers.
        root.addOrReplaceChild(
            "crown_left",
            CubeListBuilder.create()
                .texOffs(28, 48).addBox(-4.5f, -9.5f, -0.35f, 3.0f, 0.8f, 0.7f, new CubeDeformation(0.03f)),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "crown_right",
            CubeListBuilder.create()
                .texOffs(36, 48).addBox(1.5f, -9.5f, -0.35f, 3.0f, 0.8f, 0.7f, new CubeDeformation(0.03f)),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );
        root.addOrReplaceChild(
            "crown_top",
            CubeListBuilder.create()
                .texOffs(44, 48).addBox(-0.6f, -12.0f, -0.35f, 1.2f, 2.6f, 0.7f, new CubeDeformation(0.03f)),
            PartPose.offset(0.0f, 0.0f, 0.0f)
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(NihilityRenderState state) {
        super.setupAnim(state);

        int tier = Math.max(1, Math.min(5, state.tier));
        this.leftShoulder.visible = tier >= 2;
        this.rightShoulder.visible = tier >= 2;
        this.leftFlow.visible = tier >= 1;
        this.rightFlow.visible = tier >= 1;
        this.lowerFlowLeft.visible = tier >= 2;
        this.lowerFlowRight.visible = tier >= 2;
        this.sealLeft.visible = tier >= 3;
        this.sealRight.visible = tier >= 3;
        this.crownLeft.visible = tier >= 4;
        this.crownRight.visible = tier >= 4;
        this.crownTop.visible = tier >= 5;

        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0f);
        float fluidPulse = 0.018f + tier * 0.004f;

        // Reset base translations before applying animation; never accumulate frame-to-frame drift.
        this.head.y = 0.0f;
        this.body.y = 0.0f;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(age * 0.055f) * 0.025f;
        this.body.xRot = Mth.sin(age * 0.045f) * 0.018f;
        this.body.yRot = Mth.sin(age * 0.06f) * 0.035f;
        this.body.zRot = Mth.cos(age * 0.05f) * 0.022f;
        this.head.y += Mth.sin(age * 0.09f) * (0.08f + tier * 0.012f);
        this.body.y += Mth.sin(age * 0.08f + 0.7f) * (0.06f + tier * 0.01f);
        this.headSheen.y = this.head.y;
        this.headSheen.xRot = this.head.xRot;
        this.headSheen.yRot = this.head.yRot;
        this.headSheen.zRot = this.head.zRot;

        this.body.xScale = 1.0f + Mth.sin(age * 0.16f) * fluidPulse;
        this.body.yScale = 1.0f + Mth.cos(age * 0.14f) * fluidPulse;
        this.headSheen.xScale = 1.0f + Mth.sin(age * 0.18f + 1.2f) * fluidPulse;
        this.headSheen.yScale = 1.0f + Mth.cos(age * 0.17f) * fluidPulse;

        this.rightArm.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * speed * 0.65f - 0.05f;
        this.leftArm.xRot = Mth.cos(walk * 0.6662f) * speed * 0.65f - 0.05f;
        this.rightArm.yRot = 0.0f;
        this.leftArm.yRot = 0.0f;
        this.rightArm.zRot = -0.08f + Mth.sin(age * 0.08f) * 0.05f;
        this.leftArm.zRot = 0.08f - Mth.sin(age * 0.08f) * 0.05f;
        this.rightLeg.xRot = Mth.cos(walk * 0.6662f) * 0.9f * speed;
        this.leftLeg.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * 0.9f * speed;

        float flow = age * (0.06f + tier * 0.008f);
        this.leftFlow.yRot = 0.38f + Mth.sin(flow) * 0.18f;
        this.rightFlow.yRot = -0.38f - Mth.sin(flow * 0.87f + 1.3f) * 0.18f;
        this.leftFlow.zRot = -0.12f + Mth.cos(flow * 0.74f) * 0.08f;
        this.rightFlow.zRot = 0.12f - Mth.cos(flow * 0.68f + 0.5f) * 0.08f;
        this.leftFlow.y = 2.0f + Mth.sin(flow * 0.9f) * 0.24f;
        this.rightFlow.y = 2.0f + Mth.cos(flow * 0.82f) * 0.24f;

        this.lowerFlowLeft.yRot = 0.22f + Mth.sin(flow * 0.8f + 0.7f) * 0.22f;
        this.lowerFlowRight.yRot = -0.22f - Mth.cos(flow * 0.76f) * 0.22f;
        this.lowerFlowLeft.zRot = Mth.sin(flow * 0.62f) * 0.12f;
        this.lowerFlowRight.zRot = Mth.cos(flow * 0.58f) * 0.12f;

        this.sealLeft.y = 4.0f + Mth.sin(flow * 0.9f) * 0.55f;
        this.sealRight.y = 4.0f + Mth.cos(flow * 0.84f) * 0.55f;
        this.sealLeft.yRot = flow * 0.45f;
        this.sealRight.yRot = -flow * 0.4f;

        this.crownLeft.yRot = flow * 0.25f;
        this.crownRight.yRot = -flow * 0.25f;
        this.crownTop.yRot = flow * 0.38f;
        this.crownTop.xRot = Mth.sin(flow * 0.7f) * 0.14f;

        if (state.disposition == Disposition.TRADING) {
            this.rightArm.xRot = -0.62f;
            this.leftArm.xRot = -0.62f;
            this.rightArm.yRot = 0.24f;
            this.leftArm.yRot = -0.24f;
        } else if (state.disposition == Disposition.ALERT) {
            this.body.xRot -= 0.08f;
            this.rightArm.xRot = -0.82f;
            this.leftArm.xRot = -0.82f;
            this.rightArm.zRot = -0.18f;
            this.leftArm.zRot = 0.18f;
        } else if (state.disposition == Disposition.CHALLENGE) {
            this.body.xRot -= 0.04f;
            this.rightArm.xRot = -1.42f;
            this.leftArm.xRot = -1.42f;
            this.rightArm.zRot = -0.72f;
            this.leftArm.zRot = 0.72f;
            this.crownLeft.yRot += 0.18f;
            this.crownRight.yRot -= 0.18f;
        } else if (state.disposition == Disposition.HOSTILE) {
            this.body.xRot -= 0.16f;
            this.rightArm.xRot = -1.18f + Mth.sin(age * 0.24f) * 0.12f;
            this.leftArm.xRot = -1.18f - Mth.sin(age * 0.24f) * 0.12f;
        }

        if (state.ritualShieldCharges > 0) {
            float shieldPulse = Mth.sin(age * 0.18f) * 0.08f;
            this.crownLeft.yRot += 0.18f + shieldPulse;
            this.crownRight.yRot -= 0.18f + shieldPulse;
            this.crownTop.xRot -= 0.12f;
        }
        if (state.projectileGuarding) {
            this.body.xRot -= 0.18f;
            this.rightArm.xRot = -1.18f;
            this.leftArm.xRot = -1.18f;
            this.rightArm.yRot = -0.52f;
            this.leftArm.yRot = 0.52f;
        } else if (state.projectileDeflecting) {
            this.body.yRot += 0.22f;
            this.rightArm.xRot = -1.72f;
            this.rightArm.yRot = -0.62f;
            this.rightArm.zRot = -0.24f;
            this.leftArm.xRot = -0.42f;
        }

        if (state.action == ActionType.FOLLOWER_INTERCEPT) {
            if (state.actionPhase == Phase.WINDUP) {
                float charge = Math.min(1.0F, state.actionElapsed / ActionType.FOLLOWER_INTERCEPT.windupTicks());
                this.body.xRot -= 0.22f * charge;
                this.rightArm.xRot = -0.9f - charge * 0.55f;
                this.leftArm.xRot = -0.45f - charge * 0.35f;
                this.rightArm.zRot = -0.34f;
                this.leftArm.zRot = 0.24f;
            } else if (state.actionPhase == Phase.ACTIVE) {
                this.body.xRot = -0.48f;
                this.body.yRot = 0.18f;
                this.rightArm.xRot = -1.7f;
                this.rightArm.yRot = -0.58f;
                this.leftArm.xRot = -0.35f;
            }
        } else if (state.action == ActionType.ENVOY_BACKSTEP) {
            if (state.actionPhase == Phase.WINDUP) {
                this.body.xRot += 0.15f;
                this.rightArm.xRot = -0.72f;
                this.leftArm.xRot = -0.72f;
                this.rightArm.yRot = 0.42f;
                this.leftArm.yRot = -0.42f;
            } else if (state.actionPhase == Phase.ACTIVE) {
                this.body.xRot = 0.38f;
                this.rightArm.xRot = -0.25f;
                this.leftArm.xRot = -0.25f;
                this.leftFlow.zRot -= 0.3f;
                this.rightFlow.zRot += 0.3f;
            }
        } else if (state.action == ActionType.ENVOY_RIFT_DIRECTION) {
            float charge = Math.min(1.0F,
                state.actionElapsed / ActionType.ENVOY_RIFT_DIRECTION.windupTicks());
            this.body.yRot += 0.18f * charge;
            this.rightArm.xRot = -0.7f - charge * 0.95f;
            this.rightArm.yRot = -0.35f - charge * 0.32f;
            this.leftArm.xRot = -0.38f;
            this.crownTop.yRot += charge * 0.35f;
        } else if (state.action == ActionType.GRAND_ENVOY_DOMAIN) {
            float charge = Math.min(1.0F,
                state.actionElapsed / ActionType.GRAND_ENVOY_DOMAIN.windupTicks());
            this.rightArm.xRot = -0.85f - charge * 0.55f;
            this.leftArm.xRot = -0.85f - charge * 0.55f;
            this.rightArm.zRot = -0.28f - charge * 0.42f;
            this.leftArm.zRot = 0.28f + charge * 0.42f;
            this.body.xRot += charge * 0.12f;
        } else if (state.action == ActionType.GRAND_ENVOY_RECALL) {
            this.rightArm.xRot = -1.62f;
            this.leftArm.xRot = -1.62f;
            this.rightArm.yRot = 0.55f;
            this.leftArm.yRot = -0.55f;
            this.crownTop.xRot -= 0.32f;
        } else if (state.action == ActionType.GRAND_ENVOY_SHIELD) {
            this.rightArm.xRot = -1.05f;
            this.leftArm.xRot = -1.05f;
            this.rightArm.yRot = -0.42f;
            this.leftArm.yRot = 0.42f;
            this.rightArm.zRot = 0.12f;
            this.leftArm.zRot = -0.12f;
        } else if (state.action == ActionType.SAINT_ERASURE_LINE) {
            float charge = Math.min(1.0F,
                state.actionElapsed / ActionType.SAINT_ERASURE_LINE.windupTicks());
            this.body.xRot -= 0.08f * charge;
            this.rightArm.xRot = -0.72f - charge * 1.0f;
            this.rightArm.yRot = -0.18f;
            this.leftArm.xRot = -0.45f;
            this.crownTop.xRot -= charge * 0.38f;
        } else if (state.action == ActionType.SAINT_MISSING_ZONE) {
            float charge = Math.min(1.0F,
                state.actionElapsed / ActionType.SAINT_MISSING_ZONE.windupTicks());
            this.rightArm.xRot = -1.25f - charge * 0.28f;
            this.leftArm.xRot = -1.25f - charge * 0.28f;
            this.rightArm.zRot = -0.55f - charge * 0.22f;
            this.leftArm.zRot = 0.55f + charge * 0.22f;
            this.body.yRot += Mth.sin(age * 0.18f) * 0.08f;
        }
    }
}
