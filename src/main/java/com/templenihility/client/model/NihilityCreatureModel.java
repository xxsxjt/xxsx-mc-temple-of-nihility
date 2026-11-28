package com.templenihility.client.model;

import com.templenihility.TempleNihilityMod;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.client.renderer.NihilityRenderState;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import com.templenihility.entity.NihilityFollower;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class NihilityCreatureModel extends EntityModel<NihilityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_creature"), "main");

    protected final ModelPart head;
    protected final ModelPart hood;
    protected final ModelPart body;
    protected final ModelPart robeSkirt;
    protected final ModelPart cape;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;
    protected final ModelPart halberd;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;
    protected final ModelPart leftPauldron;
    protected final ModelPart rightPauldron;

    // Tier-specific floating relics, shield pillars, 6 blade-wings, astrolabe halo, twin greatswords
    protected final ModelPart focusOrb;
    protected final ModelPart orbitLeft;
    protected final ModelPart orbitRight;
    protected final ModelPart shield0;
    protected final ModelPart shield1;
    protected final ModelPart shield2;
    protected final ModelPart shield3;
    protected final ModelPart wingUL;
    protected final ModelPart wingUR;
    protected final ModelPart wingML;
    protected final ModelPart wingMR;
    protected final ModelPart wingLL;
    protected final ModelPart wingLR;
    protected final ModelPart haloGroup;
    protected final ModelPart haloOuterL;
    protected final ModelPart haloOuterR;
    protected final ModelPart haloTop;
    protected final ModelPart haloBot;
    protected final ModelPart crownSpire;
    protected final ModelPart swordLeft;
    protected final ModelPart swordRight;

    public NihilityCreatureModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.hood = this.head.getChild("hood");
        this.body = root.getChild("body");
        this.robeSkirt = this.body.getChild("robe_skirt");
        this.cape = this.body.getChild("cape");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.halberd = this.rightArm.getChild("halberd");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.leftPauldron = root.getChild("left_pauldron");
        this.rightPauldron = root.getChild("right_pauldron");

        this.focusOrb = root.getChild("focus_orb");
        this.orbitLeft = root.getChild("orbit_left");
        this.orbitRight = root.getChild("orbit_right");
        this.shield0 = root.getChild("shield_0");
        this.shield1 = root.getChild("shield_1");
        this.shield2 = root.getChild("shield_2");
        this.shield3 = root.getChild("shield_3");
        this.wingUL = root.getChild("wing_ul");
        this.wingUR = root.getChild("wing_ur");
        this.wingML = root.getChild("wing_ml");
        this.wingMR = root.getChild("wing_mr");
        this.wingLL = root.getChild("wing_ll");
        this.wingLR = root.getChild("wing_lr");
        this.haloGroup = root.getChild("halo_group");
        this.haloOuterL = this.haloGroup.getChild("halo_outer_l");
        this.haloOuterR = this.haloGroup.getChild("halo_outer_r");
        this.haloTop = this.haloGroup.getChild("halo_top");
        this.haloBot = this.haloGroup.getChild("halo_bot");
        this.crownSpire = this.haloGroup.getChild("crown_spire");
        this.swordLeft = root.getChild("sword_left");
        this.swordRight = root.getChild("sword_right");
    }

    public static LayerDefinition createBodyLayer() {
        return createLayer(0.0f);
    }

    public static LayerDefinition createLayer(float inflate) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation def = new CubeDeformation(inflate);
        CubeDeformation hoodDef = new CubeDeformation(inflate + 0.12f);

        // Head & sculpted recessed hood
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -7.0f, -3.0f, 6.0f, 7.0f, 6.0f, def),
            PartPose.offset(0.0f, 0.5f, 0.0f));
        head.addOrReplaceChild("hood",
            CubeListBuilder.create().texOffs(24, 0).addBox(-3.5f, -7.6f, -3.5f, 7.0f, 8.0f, 7.0f, hoodDef),
            PartPose.ZERO);

        // Sculpted ceremonial torso + floating pleated robe skirt + sovereign cape
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 16).addBox(-3.0f, 0.0f, -2.0f, 6.0f, 11.0f, 4.0f, def),
            PartPose.offset(0.0f, 0.5f, 0.0f));
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create().texOffs(20, 16).addBox(-4.0f, 10.0f, -3.0f, 8.0f, 12.0f, 6.0f, def),
            PartPose.ZERO);
        body.addOrReplaceChild("cape",
            CubeListBuilder.create().texOffs(52, 0).addBox(-5.0f, -1.5f, 2.1f, 10.0f, 14.0f, 2.0f, def),
            PartPose.offsetAndRotation(0.0f, 0.5f, 0.0f, 0.14f, 0.0f, 0.0f));

        // Articulated arms + T2 Follower Crescent Halberd attached to rightArm
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(0, 32).addBox(-2.0f, -1.0f, -1.5f, 3.0f, 11.0f, 3.0f, def),
            PartPose.offset(-4.4f, 1.8f, 0.0f));
        rightArm.addOrReplaceChild("halberd",
            CubeListBuilder.create()
                .texOffs(100, 36).addBox(-0.6f, -15.0f, -0.6f, 1.2f, 22.0f, 1.2f, def)
                .texOffs(100, 8).addBox(-0.6f, -19.0f, -0.5f, 1.2f, 4.2f, 1.0f, def)
                .texOffs(100, 8).addBox(-3.6f, -15.8f, -0.4f, 3.0f, 5.6f, 0.8f, def)
                .texOffs(100, 36).addBox(0.6f, -14.2f, -0.5f, 1.6f, 1.4f, 1.0f, def),
            PartPose.offsetAndRotation(-0.6f, 8.8f, -3.2f, 1.28f, 0.0f, 0.0f));

        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.0f, -1.0f, -1.5f, 3.0f, 11.0f, 3.0f, def),
            PartPose.offset(4.4f, 1.8f, 0.0f));

        // Tucked greave legs beneath the floating robe
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(12, 32).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, def),
            PartPose.offset(-1.7f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(12, 32).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, def),
            PartPose.offset(1.7f, 12.0f, 0.0f));

        // Broad flared layered pauldrons (T2, T4, T5)
        root.addOrReplaceChild("left_pauldron",
            CubeListBuilder.create().texOffs(24, 34).addBox(-1.2f, -1.6f, -3.0f, 5.0f, 4.0f, 6.0f, def),
            PartPose.offsetAndRotation(3.8f, 1.2f, 0.0f, 0.0f, 0.0f, -0.22f));
        root.addOrReplaceChild("right_pauldron",
            CubeListBuilder.create().texOffs(24, 34).mirror().addBox(-3.8f, -1.6f, -3.0f, 5.0f, 4.0f, 6.0f, def),
            PartPose.offsetAndRotation(-3.8f, 1.2f, 0.0f, 0.0f, 0.0f, 0.22f));

        // T1 Believer floating focus crystal
        root.addOrReplaceChild("focus_orb",
            CubeListBuilder.create().texOffs(88, 8).addBox(-1.5f, -3.5f, -1.0f, 3.0f, 7.0f, 2.0f, def),
            PartPose.offset(-7.2f, 6.0f, -1.5f));

        // T3 Envoy twin orbiting runic monolith tablets
        root.addOrReplaceChild("orbit_left",
            CubeListBuilder.create().texOffs(88, 8).addBox(-1.5f, -5.0f, -1.0f, 3.0f, 10.0f, 2.0f, def),
            PartPose.offset(8.0f, 4.0f, 1.5f));
        root.addOrReplaceChild("orbit_right",
            CubeListBuilder.create().texOffs(88, 8).addBox(-1.5f, -5.0f, -1.0f, 3.0f, 10.0f, 2.0f, def),
            PartPose.offset(-8.0f, 4.0f, 1.5f));

        // T4 Grand Envoy 4 orbiting Aegis Shield Pillars
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("shield_" + i,
                CubeListBuilder.create().texOffs(88, 8).addBox(-1.5f, -5.0f, -1.0f, 3.0f, 10.0f, 2.0f, def),
                PartPose.offset(0.0f, 5.0f, 0.0f));
        }

        // T5 Saint 6 articulated razor-thin Seraphic Obsidian Blade-Wings (swept cleanly behind cape!)
        root.addOrReplaceChild("wing_ul",
            CubeListBuilder.create().texOffs(76, 0).addBox(-0.6f, -17.5f, -1.2f, 1.2f, 19.0f, 2.8f, def),
            PartPose.offset(2.6f, 1.2f, 4.6f));
        root.addOrReplaceChild("wing_ur",
            CubeListBuilder.create().texOffs(76, 0).mirror().addBox(-0.6f, -17.5f, -1.2f, 1.2f, 19.0f, 2.8f, def),
            PartPose.offset(-2.6f, 1.2f, 4.6f));
        root.addOrReplaceChild("wing_ml",
            CubeListBuilder.create().texOffs(76, 0).addBox(-0.6f, -15.5f, -1.2f, 1.2f, 17.0f, 2.6f, def),
            PartPose.offset(3.0f, 4.6f, 4.9f));
        root.addOrReplaceChild("wing_mr",
            CubeListBuilder.create().texOffs(76, 0).mirror().addBox(-0.6f, -15.5f, -1.2f, 1.2f, 17.0f, 2.6f, def),
            PartPose.offset(-3.0f, 4.6f, 4.9f));
        root.addOrReplaceChild("wing_ll",
            CubeListBuilder.create().texOffs(76, 0).addBox(-0.5f, -13.0f, -1.1f, 1.0f, 14.5f, 2.4f, def),
            PartPose.offset(2.5f, 8.2f, 5.2f));
        root.addOrReplaceChild("wing_lr",
            CubeListBuilder.create().texOffs(76, 0).mirror().addBox(-0.5f, -13.0f, -1.1f, 1.0f, 14.5f, 2.4f, def),
            PartPose.offset(-2.5f, 8.2f, 5.2f));

        // T4/T5 Delicate Golden Astrolabe Halo & Slender Sovereign Crown Spire
        PartDefinition halo = root.addOrReplaceChild("halo_group",
            CubeListBuilder.create(),
            PartPose.offset(0.0f, -5.8f, 4.8f));
        halo.addOrReplaceChild("halo_top",
            CubeListBuilder.create().texOffs(88, 0).addBox(-4.5f, -6.8f, -0.5f, 9.0f, 1.0f, 1.0f, def),
            PartPose.ZERO);
        halo.addOrReplaceChild("halo_bot",
            CubeListBuilder.create().texOffs(88, 0).addBox(-4.5f, 5.8f, -0.5f, 9.0f, 1.0f, 1.0f, def),
            PartPose.ZERO);
        halo.addOrReplaceChild("halo_outer_l",
            CubeListBuilder.create().texOffs(88, 0).addBox(-4.5f, -0.5f, -0.5f, 9.0f, 1.0f, 1.0f, def),
            PartPose.offsetAndRotation(6.2f, 0.0f, 0.0f, 0.0f, 0.0f, Mth.HALF_PI));
        halo.addOrReplaceChild("halo_outer_r",
            CubeListBuilder.create().texOffs(88, 0).addBox(-4.5f, -0.5f, -0.5f, 9.0f, 1.0f, 1.0f, def),
            PartPose.offsetAndRotation(-6.2f, 0.0f, 0.0f, 0.0f, 0.0f, Mth.HALF_PI));
        halo.addOrReplaceChild("crown_spire",
            CubeListBuilder.create().texOffs(88, 0).addBox(-0.7f, -11.5f, -0.6f, 1.4f, 6.2f, 1.2f, def),
            PartPose.offset(0.0f, -0.5f, 0.0f));

        // T5 Saint Twin Hovering Void Greatswords — true tapered dark-obsidian blades pointing SKYWARD with sleek gold crossguards!
        root.addOrReplaceChild("sword_left",
            CubeListBuilder.create()
                .texOffs(100, 36).addBox(-0.6f, 4.0f, -0.6f, 1.2f, 5.0f, 1.2f, def)
                .texOffs(100, 36).addBox(-2.8f, 2.6f, -0.8f, 5.6f, 1.4f, 1.6f, def)
                .texOffs(100, 8).addBox(-1.1f, -13.4f, -0.45f, 2.2f, 16.0f, 0.9f, def)
                .texOffs(100, 8).addBox(-0.7f, -17.0f, -0.35f, 1.4f, 3.6f, 0.7f, def),
            PartPose.offset(14.5f, 2.0f, 2.8f));
        root.addOrReplaceChild("sword_right",
            CubeListBuilder.create()
                .texOffs(100, 36).addBox(-0.6f, 4.0f, -0.6f, 1.2f, 5.0f, 1.2f, def)
                .texOffs(100, 36).addBox(-2.8f, 2.6f, -0.8f, 5.6f, 1.4f, 1.6f, def)
                .texOffs(100, 8).mirror().addBox(-1.1f, -13.4f, -0.45f, 2.2f, 16.0f, 0.9f, def)
                .texOffs(100, 8).mirror().addBox(-0.7f, -17.0f, -0.35f, 1.4f, 3.6f, 0.7f, def),
            PartPose.offset(-14.5f, 2.0f, 2.8f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    private static float easeOutExpo(float t) {
        float x = Mth.clamp(t, 0.0f, 1.0f);
        return x >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -9.0 * x);
    }

    @Override
    public void setupAnim(NihilityRenderState state) {
        super.setupAnim(state);
        int tier = Math.max(1, Math.min(5, state.tier));
        float age = state.ageInTicks;
        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0f);

        // Tier-exclusive silhouette toggles
        this.hood.visible = true;
        this.robeSkirt.visible = true;
        this.cape.visible = tier >= 3;
        this.leftPauldron.visible = tier == 2 || tier >= 4;
        this.rightPauldron.visible = tier == 2 || tier >= 4;
        this.focusOrb.visible = tier == 1;
        this.halberd.visible = tier == 2;
        boolean envoyMonolithsDocked = tier == 3 && !state.envoyMonolithsDeployed;
        this.orbitLeft.visible = envoyMonolithsDocked;
        this.orbitRight.visible = envoyMonolithsDocked;

        boolean showShields = tier == 4;
        int dockedShields = Math.max(0, 4 - state.grandEnvoyDeployedShields);
        this.shield0.visible = showShields && dockedShields >= 1;
        this.shield1.visible = showShields && dockedShields >= 2;
        this.shield2.visible = showShields && dockedShields >= 3;
        this.shield3.visible = showShields && dockedShields >= 4;

        boolean isSaint = tier >= 5;
        this.wingUL.visible = isSaint;
        this.wingUR.visible = isSaint;
        this.wingML.visible = isSaint;
        this.wingMR.visible = isSaint;
        this.wingLL.visible = isSaint;
        this.wingLR.visible = isSaint;
        boolean saintSwordsDocked = isSaint && !state.saintSwordsDeployed;
        this.swordLeft.visible = saintSwordsDocked;
        this.swordRight.visible = saintSwordsDocked;

        this.haloGroup.visible = tier >= 4;
        this.haloBot.visible = isSaint;
        this.crownSpire.visible = isSaint;

        // Clean baseline reset
        float hover = isSaint
            ? Mth.sin(age * 0.085f) * (state.saintSwordLiberation ? 1.15f : 0.85f) - (state.saintSwordLiberation ? 1.4f : 0.6f)
            : (tier >= 3 ? Mth.sin(age * 0.09f) * 0.55f : Mth.sin(age * 0.08f) * 0.18f);
        this.head.y = 0.5f + hover;
        this.body.y = 0.5f + hover;
        this.rightArm.y = 1.8f + hover;
        this.leftArm.y = 1.8f + hover;
        this.leftPauldron.y = 1.2f + hover;
        this.rightPauldron.y = 1.2f + hover;

        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(age * 0.05f) * 0.03f;

        this.body.xRot = Mth.sin(age * 0.045f) * 0.03f;
        this.body.yRot = 0.0f;
        this.body.zRot = 0.0f;
        this.cape.xRot = 0.18f + speed * 0.35f + Mth.sin(age * 0.11f) * 0.08f;
        this.robeSkirt.xRot = speed * 0.18f + Mth.sin(age * 0.08f) * 0.04f;

        if (tier >= 3) {
            // Tiers 3..5 (Envoy / Grand Envoy / Saint): True aerial levitation — tucked drifting legs, flowing cape, zero villager walking!
            float legDrift = Mth.sin(age * 0.08f) * 0.09f;
            this.rightLeg.y = 12.2f + hover * 0.65f;
            this.leftLeg.y = 12.2f + hover * 0.65f;
            this.rightLeg.xRot = (tier == 3 ? 0.32f : 0.24f) + speed * 0.36f + legDrift;
            this.leftLeg.xRot = (tier == 3 ? 0.46f : 0.38f) + speed * 0.40f - legDrift;
            this.rightLeg.zRot = 0.06f;
            this.leftLeg.zRot = -0.06f;
            this.body.xRot = (tier == 3 ? 0.14f : 0.07f) + speed * 0.22f + Mth.sin(age * 0.055f) * 0.04f;
            this.cape.xRot = 0.42f + speed * 0.55f + Mth.sin(age * 0.13f) * 0.14f;
            this.robeSkirt.xRot = 0.26f + speed * 0.32f + Mth.sin(age * 0.10f) * 0.08f;
            this.rightArm.xRot = -0.34f + Mth.sin(age * 0.09f) * 0.09f;
            this.leftArm.xRot = -0.34f - Mth.sin(age * 0.09f) * 0.09f;
            this.rightArm.yRot = 0.0f;
            this.leftArm.yRot = 0.0f;
            this.rightArm.zRot = (isSaint ? 0.30f : 0.22f) + Mth.sin(age * 0.08f) * 0.07f;
            this.leftArm.zRot = -(isSaint ? 0.30f : 0.22f) - Mth.sin(age * 0.08f) * 0.07f;
        } else if (tier == 2) {
            // Tier 2 Follower: Grounded heavy armored halberd vanguard — powerful wide stride & couched polearm stance!
            this.rightLeg.y = 12.0f;
            this.leftLeg.y = 12.0f;
            this.rightLeg.zRot = 0.05f;
            this.leftLeg.zRot = -0.05f;
            this.rightLeg.xRot = Mth.cos(walk * 0.6662f) * 0.72f * speed;
            this.leftLeg.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * 0.72f * speed;
            this.body.xRot = 0.08f + speed * 0.14f;
            this.rightArm.xRot = -0.62f + Mth.cos(walk * 0.6662f + Mth.PI) * speed * 0.28f;
            this.rightArm.yRot = -0.20f;
            this.rightArm.zRot = 0.14f;
            this.leftArm.xRot = -0.45f + Mth.cos(walk * 0.6662f) * speed * 0.32f;
            this.leftArm.yRot = 0.22f;
            this.leftArm.zRot = -0.14f;
        } else {
            // Tier 1 Believer: Light robe-gliding crystal caster — graceful reduced leg swing & conducting hand!
            this.rightLeg.y = 12.0f;
            this.leftLeg.y = 12.0f;
            this.rightLeg.zRot = 0.0f;
            this.leftLeg.zRot = 0.0f;
            this.rightLeg.xRot = Mth.cos(walk * 0.6662f) * 0.28f * speed + 0.08f;
            this.leftLeg.xRot = Mth.cos(walk * 0.6662f + Mth.PI) * 0.28f * speed + 0.08f;
            this.rightArm.xRot = -0.48f + Mth.sin(age * 0.11f) * 0.08f;
            this.rightArm.yRot = 0.14f;
            this.rightArm.zRot = 0.12f;
            this.leftArm.xRot = -0.35f;
            this.leftArm.yRot = -0.22f;
            this.leftArm.zRot = -0.10f;
        }

        // T1 Focus Crystal baseline orbit (resets cleanly every frame)
        this.focusOrb.x = -7.2f + Mth.cos(age * 0.10f) * 0.9f;
        this.focusOrb.y = 5.2f + Mth.sin(age * 0.14f) * 1.1f;
        this.focusOrb.z = -1.5f + Mth.sin(age * 0.10f) * 0.9f;
        this.focusOrb.xRot = 0.0f;
        this.focusOrb.yRot = age * 0.11f;
        this.focusOrb.zRot = 0.25f;

        // T3 Envoy twin orbiting runic tablets
        float orbPhase = age * 0.085f;
        this.orbitLeft.x = 7.6f + Mth.cos(orbPhase) * 1.2f;
        this.orbitLeft.y = 4.5f + hover + Mth.sin(orbPhase * 1.3f) * 1.6f;
        this.orbitLeft.z = 1.5f;
        this.orbitLeft.xRot = 0.0f;
        this.orbitLeft.yRot = orbPhase * 1.4f;
        this.orbitLeft.zRot = 0.14f;
        this.orbitRight.x = -7.6f - Mth.cos(orbPhase) * 1.2f;
        this.orbitRight.y = 4.5f + hover - Mth.sin(orbPhase * 1.3f) * 1.6f;
        this.orbitRight.z = 1.5f;
        this.orbitRight.xRot = 0.0f;
        this.orbitRight.yRot = -orbPhase * 1.4f;
        this.orbitRight.zRot = -0.14f;

        // T4 Grand Envoy 4 orbiting Aegis Shield Pillars (expand wide in Domain, tighten in Ritual Shield!)
        float shieldSpeed = state.grandEnvoyDomainActive ? 0.115f : (state.ritualShieldCharges > 0 ? 0.095f : 0.055f);
        float shieldRadius = state.grandEnvoyDomainActive ? 14.8f : (state.ritualShieldCharges > 0 ? 10.6f : 9.8f);
        ModelPart[] shields = { this.shield0, this.shield1, this.shield2, this.shield3 };
        for (int i = 0; i < 4; i++) {
            float a = age * shieldSpeed + i * Mth.HALF_PI;
            shields[i].x = Mth.cos(a) * shieldRadius;
            shields[i].z = Mth.sin(a) * shieldRadius;
            shields[i].y = 4.5f + hover + Mth.sin(age * 0.11f + i * 1.4f) * 1.3f;
            shields[i].yRot = -a + Mth.HALF_PI;
            shields[i].xRot = 0.1f * Mth.sin(age * 0.09f + i);
        }

        // T4/T5 Celestial Astrolabe Halo — spin & overhead crown tilt reflect Astrolabe Charge (0..6) & Sword Liberation
        float saintHaloRate = 0.048f + state.saintAstrolabeCharge * 0.045f + (state.saintSwordLiberation ? 0.06f : 0.0f);
        this.haloGroup.y = -5.2f + hover - (isSaint && state.saintSwordLiberation ? 2.2f : (isSaint ? state.saintAstrolabeCharge * 0.22f : 0.0f));
        this.haloGroup.zRot = age * (isSaint ? saintHaloRate : 0.03f);
        this.haloGroup.xRot = isSaint && state.saintSwordLiberation
            ? 0.48f + 0.08f * Mth.sin(age * 0.09f)
            : (isSaint ? 0.10f + state.saintAstrolabeCharge * 0.04f + 0.06f * Mth.sin(age * 0.06f) : 0.08f * Mth.sin(age * 0.06f));
        this.crownSpire.zRot = -this.haloGroup.zRot; // Keeps spire vertical!
        this.crownSpire.y = -1.0f + Mth.sin(age * 0.12f) * 0.45f;

        // T5 Saint 6 Seraphic Blade-Wings baseline
        float wingBreath = Mth.sin(age * 0.1f) * 0.09f;
        float wingSpread = state.saintSwordLiberation ? 1.24f : 1.0f;
        float wingSweepBack = state.saintSwordLiberation ? 0.16f : 0.28f;
        float wingYawFold = 0.0f;
        float wingZShift = 0.0f;

        // T5 Saint Twin Hovering Void Greatswords baseline (tighter, aggressive forward cant in Sword Liberation)
        float swordOrbitX = state.saintSwordLiberation ? 12.2f : 14.5f;
        float swordOrbitZ = state.saintSwordLiberation ? -1.4f : 2.8f;
        float swordForwardPitch = state.saintSwordLiberation ? 0.42f : 0.08f;
        this.swordLeft.x = swordOrbitX + Mth.sin(age * 0.07f) * 0.85f;
        this.swordLeft.y = 1.0f + hover + Mth.sin(age * 0.12f) * 1.15f;
        this.swordLeft.z = swordOrbitZ;
        this.swordLeft.xRot = swordForwardPitch + Mth.sin(age * 0.08f) * 0.07f;
        this.swordLeft.yRot = -0.22f;
        this.swordLeft.zRot = state.saintSwordLiberation ? 0.36f : 0.26f;

        this.swordRight.x = -swordOrbitX - Mth.sin(age * 0.07f) * 0.85f;
        this.swordRight.y = 1.0f + hover + Mth.cos(age * 0.12f) * 1.15f;
        this.swordRight.z = swordOrbitZ;
        this.swordRight.xRot = swordForwardPitch + Mth.cos(age * 0.08f) * 0.07f;
        this.swordRight.yRot = 0.22f;
        this.swordRight.zRot = state.saintSwordLiberation ? -0.36f : -0.26f;

        // Disposition stances
        if (state.disposition == Disposition.TRADING) {
            this.rightArm.xRot = -0.58f;
            this.leftArm.xRot = -0.58f;
            this.rightArm.yRot = 0.25f;
            this.leftArm.yRot = -0.25f;
        } else if (state.disposition == Disposition.ALERT) {
            this.body.xRot -= 0.08f;
            this.rightArm.xRot = -0.85f;
            this.leftArm.xRot = -0.85f;
        } else if (state.disposition == Disposition.CHALLENGE) {
            wingSpread = 1.28f;
            this.haloGroup.zRot = age * 0.11f;
            this.crownSpire.zRot = -this.haloGroup.zRot;
            this.rightArm.xRot = -1.25f;
            this.leftArm.xRot = -1.25f;
            this.rightArm.zRot = 0.55f;
            this.leftArm.zRot = -0.55f;
        } else if (state.disposition == Disposition.HOSTILE) {
            wingSpread = 1.18f;
            this.body.xRot -= 0.12f;
            this.rightArm.xRot = -1.05f + Mth.sin(age * 0.22f) * 0.14f;
            this.leftArm.xRot = -0.95f - Mth.sin(age * 0.22f) * 0.14f;
        }

        // Expressive Tier-Specific Melee Swing Choreography (state.attackSwing 0..1)
        if (state.attackSwing > 0.001f) {
            float swingSin = Mth.sin(state.attackSwing * Mth.PI);
            float follow = Mth.sin((1.0f - (1.0f - state.attackSwing) * (1.0f - state.attackSwing)) * Mth.PI);
            this.body.yRot += swingSin * 0.42f;
            this.body.xRot += swingSin * 0.16f;
            if (tier == 1) {
                this.rightArm.xRot = -1.65f * swingSin - 0.35f;
                this.rightArm.yRot = -0.45f * follow;
                this.leftArm.xRot = -1.1f * swingSin;
                this.focusOrb.z -= 4.2f * swingSin;
                this.focusOrb.y -= 2.0f * swingSin;
            } else if (tier == 2) {
                // Full heavy Halberd overhead diagonal cleave!
                this.rightArm.xRot = -2.15f + 2.45f * follow;
                this.rightArm.yRot = -0.58f * swingSin;
                this.rightArm.zRot = 0.25f * swingSin;
                this.leftArm.xRot = -1.25f * swingSin;
            } else if (tier == 3) {
                // Twin runic monolith pincer lunge!
                this.rightArm.xRot = -1.45f * swingSin;
                this.leftArm.xRot = -1.45f * swingSin;
                this.orbitLeft.z -= 7.5f * swingSin;
                this.orbitRight.z -= 7.5f * swingSin;
                this.orbitLeft.xRot += 0.95f * swingSin;
                this.orbitRight.xRot += 0.95f * swingSin;
            } else if (tier == 4) {
                // Grand Envoy 4-shield slam outward + forward!
                this.rightArm.xRot = -1.55f * swingSin;
                this.leftArm.xRot = -1.55f * swingSin;
                for (int i = 0; i < 4; i++) {
                    shields[i].z -= 4.0f * swingSin;
                    shields[i].xRot += 0.65f * swingSin;
                }
            } else {
                // Saint twin greatswords scissor cross-cleave!
                this.rightArm.xRot = -1.75f * swingSin;
                this.leftArm.xRot = -1.75f * swingSin;
                wingSpread += 0.35f * swingSin;
                this.swordLeft.z -= 10.5f * swingSin;
                this.swordRight.z -= 10.5f * swingSin;
                this.swordLeft.xRot = 1.45f * swingSin;
                this.swordRight.xRot = 1.45f * swingSin;
                this.swordLeft.zRot = 0.95f * follow;
                this.swordRight.zRot = -0.95f * follow;
            }
        }

        // Reactive Projectile Guard (T2) & Deflect (T3)
        if (state.projectileGuarding && tier == 2) {
            this.rightArm.xRot = -1.18f;
            this.rightArm.yRot = 0.72f;
            this.rightArm.zRot = -0.52f;
            this.leftArm.xRot = -0.92f;
        }
        if (state.projectileDeflecting && tier == 3) {
            this.rightArm.xRot = -1.45f;
            this.leftArm.xRot = -1.45f;
            this.rightArm.zRot = 0.55f;
            this.leftArm.zRot = -0.55f;
            this.orbitLeft.x = 3.2f; this.orbitLeft.z = -7.2f; this.orbitLeft.zRot = 0.72f;
            this.orbitRight.x = -3.2f; this.orbitRight.z = -7.2f; this.orbitRight.zRot = -0.72f;
        }

        // Action Choreography (Snappy Anticipation -> Explosive Strike -> Elastic Recovery)
        float windup = state.action != ActionType.NONE && state.action.windupTicks() > 0
            ? Mth.clamp(state.actionElapsed / state.action.windupTicks(), 0.0f, 1.0f)
            : 0.0f;
        float snap = state.actionPhase == Phase.ACTIVE ? 1.0f
            : state.actionPhase == Phase.RECOVERY
                ? Math.max(0.0f, 1.0f - (state.actionElapsed - state.action.windupTicks()) / Math.max(1.0f, state.action.totalTicks() - state.action.windupTicks()))
                : 0.0f;
        float snapEase = easeOutExpo(snap);

        if (state.action == ActionType.FOLLOWER_INTERCEPT) {
            if (state.actionPhase == Phase.WINDUP) {
                this.body.yRot = -0.45f * windup;
                this.rightArm.xRot = -1.95f * windup - 0.35f;
                this.rightArm.zRot = 0.35f * windup;
            } else {
                this.body.xRot = 0.32f * snapEase;
                this.body.yRot = 0.55f * snapEase;
                this.rightArm.xRot = -0.15f + 1.15f * snapEase;
                this.rightArm.yRot = -0.55f * snapEase;
            }
        } else if (state.action == ActionType.ENVOY_RIFT_DIRECTION) {
            float power = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            this.head.y -= 1.5f * power;
            this.body.y -= 1.5f * power;
            this.rightArm.xRot = -1.65f * power - 0.3f;
            this.leftArm.xRot = -1.65f * power - 0.3f;
            this.rightArm.zRot = 0.78f * power;
            this.leftArm.zRot = -0.78f * power;
            this.orbitLeft.x = 11.0f * (1.0f - 0.55f * snapEase);
            this.orbitRight.x = -11.0f * (1.0f - 0.55f * snapEase);
            this.orbitLeft.z = -3.0f - 7.5f * power;
            this.orbitRight.z = -3.0f - 7.5f * power;
            this.orbitLeft.yRot = age * 0.45f;
            this.orbitRight.yRot = -age * 0.45f;
        } else if (state.action == ActionType.ENVOY_BACKSTEP) {
            float p = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            this.body.xRot = -0.34f * p;
            this.rightArm.xRot = -0.75f * p;
            this.leftArm.xRot = -0.75f * p;
            this.cape.xRot += 0.55f * p;
        } else if (state.action == ActionType.GRAND_ENVOY_RECALL) {
            float p = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            this.rightArm.xRot = -2.25f * p;
            this.leftArm.xRot = -2.25f * p;
            this.rightArm.zRot = 0.55f * p;
            this.leftArm.zRot = -0.55f * p;
            this.haloGroup.zRot = age * 0.28f;
            for (int i = 0; i < 4; i++) {
                float a = age * 0.3f + i * Mth.HALF_PI;
                shields[i].x = Mth.cos(a) * (12.0f + p * 5.0f);
                shields[i].z = Mth.sin(a) * (12.0f + p * 5.0f);
                shields[i].y = 1.0f - p * 4.0f;
            }
        } else if (state.action == ActionType.GRAND_ENVOY_DOMAIN || state.action == ActionType.GRAND_ENVOY_SHIELD) {
            float lift = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            this.rightArm.xRot = -1.65f * lift - 0.4f;
            this.leftArm.xRot = -1.65f * lift - 0.4f;
            this.rightArm.zRot = 0.65f * lift;
            this.leftArm.zRot = -0.65f * lift;
            this.haloGroup.zRot = age * 0.18f;
            this.crownSpire.zRot = -this.haloGroup.zRot;
            for (int i = 0; i < 4; i++) {
                float a = age * 0.22f + i * Mth.HALF_PI;
                shields[i].x = Mth.cos(a) * (10.5f + lift * 4.5f);
                shields[i].z = Mth.sin(a) * (10.5f + lift * 4.5f);
                shields[i].y = 2.5f - lift * 3.0f;
            }
        } else if (state.action == ActionType.SAINT_ERASURE_LINE) {
            if (state.actionPhase == Phase.WINDUP) {
                // Coil back & aim twin greatswords forward along the erasure line
                this.body.yRot = -0.55f * windup;
                this.rightArm.xRot = -1.75f * windup - 0.3f;
                this.leftArm.xRot = -0.9f * windup;
                wingSpread = 0.85f - 0.15f * windup;
                this.swordRight.x = -9.0f;
                this.swordRight.z = -5.5f - 6.0f * windup;
                this.swordRight.xRot = 1.55f;
                this.swordLeft.x = 9.0f;
                this.swordLeft.z = -5.5f - 6.0f * windup;
                this.swordLeft.xRot = 1.55f;
                this.haloGroup.zRot = age * 0.22f;
                this.crownSpire.zRot = -this.haloGroup.zRot;
            } else {
                // Explosive cross-slash follow-through!
                this.body.yRot = 0.68f * snapEase;
                this.rightArm.xRot = -0.2f + 1.1f * snapEase;
                this.leftArm.xRot = -1.4f * snapEase;
                wingSpread = 1.55f * snapEase + 0.85f * (1.0f - snapEase);
                this.swordRight.x = -3.0f + 9.0f * snapEase;
                this.swordRight.z = -12.0f * snapEase - 2.0f;
                this.swordRight.xRot = 1.65f;
                this.swordRight.zRot = -1.25f * snapEase;
                this.swordLeft.x = 3.0f - 9.0f * snapEase;
                this.swordLeft.z = -12.0f * snapEase - 2.0f;
                this.swordLeft.xRot = 1.65f;
                this.swordLeft.zRot = 1.25f * snapEase;
            }
        } else if (state.action == ActionType.SAINT_MISSING_ZONE) {
            float power = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            this.head.y -= power * 2.4f;
            this.body.y -= power * 2.4f;
            this.rightArm.xRot = -1.45f - power * 0.45f;
            this.leftArm.xRot = -1.45f - power * 0.45f;
            this.rightArm.zRot = 0.85f * power;
            this.leftArm.zRot = -0.85f * power;
            wingSpread = 1.38f;
            this.swordLeft.y = -5.0f - power * 4.0f;
            this.swordRight.y = -5.0f - power * 4.0f;
            this.swordLeft.xRot = 3.14f; // Blade points straight down for judgment drop!
            this.swordRight.xRot = 3.14f;
        } else if (state.action == ActionType.SAINT_VOID_TERMINUS) {
            // Ultimate Sovereign Ascension: float skyward, 6 wings flare max width, halo spins wildly, swords cross overhead
            float power = state.actionPhase == Phase.WINDUP ? windup : snapEase;
            float rise = 3.8f * power;
            this.head.y -= rise;
            this.body.y -= rise;
            this.rightArm.y -= rise;
            this.leftArm.y -= rise;
            this.haloGroup.y -= rise;
            this.rightArm.xRot = -2.25f * power - 0.35f;
            this.leftArm.xRot = -2.25f * power - 0.35f;
            this.rightArm.zRot = 0.95f * power;
            this.leftArm.zRot = -0.95f * power;
            wingSpread = 1.65f;
            wingSweepBack = 0.1f;
            this.haloGroup.zRot = age * 0.34f;
            this.crownSpire.zRot = -this.haloGroup.zRot;
            this.swordLeft.x = 13.5f - 6.0f * snapEase;
            this.swordLeft.y = -2.0f - rise;
            this.swordLeft.z = -4.5f - 7.0f * snapEase;
            this.swordLeft.xRot = 0.85f + 0.8f * snapEase;
            this.swordLeft.zRot = 0.72f;
            this.swordRight.x = -13.5f + 6.0f * snapEase;
            this.swordRight.y = -2.0f - rise;
            this.swordRight.z = -4.5f - 7.0f * snapEase;
            this.swordRight.xRot = 0.85f + 0.8f * snapEase;
            this.swordRight.zRot = -0.72f;
        }

        // Tier 1 Believer: Focus-Crystal Salvo Channel & Void Repulsion Pulse!
        if (tier == 1) {
            if (state.believerPulseTicks > 0) {
                this.rightArm.xRot = -1.25f;
                this.leftArm.xRot = -1.25f;
                this.rightArm.zRot = 1.05f;
                this.leftArm.zRot = -1.05f;
                this.focusOrb.x = 0.0f;
                this.focusOrb.y = 3.5f;
                this.focusOrb.z = -9.5f;
                this.focusOrb.yRot = age * 0.95f;
            } else if (state.believerCastingTicks > 0) {
                this.body.xRot = -0.10f;
                this.rightArm.xRot = -1.52f + Mth.sin(age * 0.55f) * 0.12f;
                this.rightArm.yRot = 0.18f;
                this.leftArm.xRot = -0.78f;
                this.leftArm.yRot = -0.35f;
                this.focusOrb.x = -2.4f + Mth.sin(age * 0.45f) * 0.8f;
                this.focusOrb.y = 2.8f + Mth.cos(age * 0.45f) * 0.6f;
                this.focusOrb.z = -8.6f;
                this.focusOrb.yRot = age * 0.68f;
                this.focusOrb.zRot = 0.0f;
            }
        }

        // Tier 2 Follower: Heavy Halberd Sweep, Parry Block & Counter-Cleave Choreography!
        if (tier == 2 && state.followerHalberdStance != NihilityFollower.STANCE_IDLE) {
            if (state.followerHalberdStance == NihilityFollower.STANCE_SWEEP_WINDUP) {
                // Coil torso far back-right loading a 160-degree horizontal halberd cleave!
                this.body.yRot = -0.62f;
                this.body.xRot = 0.12f;
                this.rightArm.xRot = -1.45f;
                this.rightArm.yRot = -0.85f;
                this.rightArm.zRot = 0.95f;
                this.leftArm.xRot = -0.95f;
                this.leftArm.yRot = 0.55f;
            } else if (state.followerHalberdStance == NihilityFollower.STANCE_SWEEP_ACTIVE) {
                // Explosive wide horizontal halberd sweep across front!
                this.body.yRot = 0.78f;
                this.body.xRot = 0.24f;
                this.rightArm.xRot = -1.12f;
                this.rightArm.yRot = 1.25f;
                this.rightArm.zRot = -0.92f;
                this.leftArm.xRot = -0.82f;
                this.leftArm.yRot = -0.65f;
            } else if (state.followerHalberdStance == NihilityFollower.STANCE_GUARD_PARRY) {
                // Brace halberd horizontally across chest in armored parry stance!
                this.body.xRot = -0.06f;
                this.rightArm.xRot = -1.18f;
                this.rightArm.yRot = 0.85f;
                this.rightArm.zRot = -0.82f;
                this.leftArm.xRot = -1.08f;
                this.leftArm.yRot = -0.58f;
                this.leftArm.zRot = 0.35f;
            } else if (state.followerHalberdStance == NihilityFollower.STANCE_COUNTER_SLAM) {
                // Crushing overhead vertical halberd counter-cleave!
                this.body.xRot = 0.38f;
                this.rightArm.xRot = 0.48f;
                this.rightArm.yRot = -0.25f;
                this.rightArm.zRot = 0.12f;
                this.leftArm.xRot = -1.25f;
                this.leftArm.zRot = -0.45f;
            }
        }

        // Tier 3 Envoy: Spatial Backstab Twin-Monolith Forward Cross-Thrust!
        if (tier == 3 && state.envoyBackstabTicks > 0) {
            this.body.xRot = 0.36f;
            this.rightArm.xRot = -1.48f;
            this.leftArm.xRot = -1.48f;
            this.rightArm.zRot = 0.35f;
            this.leftArm.zRot = -0.35f;
            if (envoyMonolithsDocked) {
                this.orbitLeft.x = 3.2f;
                this.orbitLeft.z = -9.2f;
                this.orbitLeft.xRot = 1.38f;
                this.orbitLeft.zRot = 0.55f;
                this.orbitRight.x = -3.2f;
                this.orbitRight.z = -9.2f;
                this.orbitRight.xRot = 1.38f;
                this.orbitRight.zRot = -0.55f;
            }
        }

        // Tier 4 Grand Envoy: Commanding Palm-Thrust when hurling Aegis Shield Pillars!
        if (tier == 4 && state.grandEnvoyCommandTicks > 0) {
            this.rightArm.xRot = -1.58f;
            this.rightArm.yRot = 0.18f;
            this.rightArm.zRot = 0.28f;
            this.leftArm.xRot = -0.95f;
            this.leftArm.zRot = -0.32f;
            this.haloGroup.zRot = age * 0.22f;
        }

        // Saint Live Armament States — each floating piece visibly performs its exact combat role!
        if (isSaint) {
            if (state.saintSwordsGuardTicks > 0 && saintSwordsDocked) {
                // Close-quarters X-Guard Sword Counter: both swords snap forward in front of Saint and whirl in a scissor cross!
                float guardSpin = age * 0.78f;
                this.rightArm.xRot = -1.32f;
                this.rightArm.yRot = 0.52f;
                this.leftArm.xRot = -1.32f;
                this.leftArm.yRot = -0.52f;
                this.swordLeft.x = 3.2f + Mth.cos(guardSpin) * 2.2f;
                this.swordLeft.y = 2.0f + hover + Mth.sin(guardSpin) * 1.6f;
                this.swordLeft.z = -9.6f;
                this.swordLeft.xRot = 1.48f;
                this.swordLeft.zRot = 0.88f + Mth.sin(guardSpin) * 0.65f;
                this.swordRight.x = -3.2f - Mth.cos(guardSpin) * 2.2f;
                this.swordRight.y = 2.0f + hover - Mth.sin(guardSpin) * 1.6f;
                this.swordRight.z = -9.6f;
                this.swordRight.xRot = 1.48f;
                this.swordRight.zRot = -0.88f - Mth.sin(guardSpin) * 0.65f;
            }
            if (state.saintWingShieldTicks > 0) {
                // 「六翼合拢 · 相位折射茧」: all 6 obsidian wings fold tightly forward around torso forming a prism cocoon!
                wingSpread = 0.24f;
                wingSweepBack = -1.08f;
                wingYawFold = 0.92f;
                wingZShift = -2.4f;
                wingBreath = Mth.sin(age * 0.9f) * 0.035f;
                this.head.xRot = 0.18f;
                this.rightArm.xRot = -1.08f;
                this.rightArm.yRot = 0.68f;
                this.rightArm.zRot = -0.18f;
                this.leftArm.xRot = -1.08f;
                this.leftArm.yRot = -0.68f;
                this.leftArm.zRot = 0.18f;
            } else if (state.saintWingSalvoTicks > 0) {
                // 「六翼齐射 · 归零羽刃」: all 6 wings flare wide & cant forward, vibrating with staggered feather launches!
                wingSpread = 1.56f;
                wingSweepBack = -0.34f;
                wingYawFold = 0.22f;
                wingBreath = Mth.sin(age * 1.45f) * 0.14f;
                this.rightArm.xRot = -1.42f;
                this.leftArm.xRot = -1.42f;
                this.rightArm.zRot = 0.78f;
                this.leftArm.zRot = -0.78f;
            }
        }

        // Apply final 6-wing seraphic spread, fold, & breathing!
        this.wingUL.y = 1.5f + hover;
        this.wingUR.y = 1.5f + hover;
        this.wingML.y = 4.5f + hover;
        this.wingMR.y = 4.5f + hover;
        this.wingLL.y = 7.8f + hover;
        this.wingLR.y = 7.8f + hover;

        this.wingUL.z = 4.6f + wingZShift;
        this.wingUR.z = 4.6f + wingZShift;
        this.wingML.z = 4.9f + wingZShift;
        this.wingMR.z = 4.9f + wingZShift;
        this.wingLL.z = 5.2f + wingZShift;
        this.wingLR.z = 5.2f + wingZShift;

        this.wingUL.zRot = (0.58f + wingBreath) * wingSpread;
        this.wingUR.zRot = -(0.58f + wingBreath) * wingSpread;
        this.wingUL.xRot = -wingSweepBack;
        this.wingUR.xRot = -wingSweepBack;
        this.wingUL.yRot = wingYawFold;
        this.wingUR.yRot = -wingYawFold;

        this.wingML.zRot = (1.08f + wingBreath * 1.15f) * wingSpread;
        this.wingMR.zRot = -(1.08f + wingBreath * 1.15f) * wingSpread;
        this.wingML.xRot = -wingSweepBack * 0.85f;
        this.wingMR.xRot = -wingSweepBack * 0.85f;
        this.wingML.yRot = wingYawFold * 0.9f;
        this.wingMR.yRot = -wingYawFold * 0.9f;

        this.wingLL.zRot = (1.62f + wingBreath * 0.9f) * wingSpread;
        this.wingLR.zRot = -(1.62f + wingBreath * 0.9f) * wingSpread;
        this.wingLL.xRot = -wingSweepBack * 0.7f;
        this.wingLR.xRot = -wingSweepBack * 0.7f;
        this.wingLL.yRot = wingYawFold * 0.75f;
        this.wingLR.yRot = -wingYawFold * 0.75f;
    }
}
