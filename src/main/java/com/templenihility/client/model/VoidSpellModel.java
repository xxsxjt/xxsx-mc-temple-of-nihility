package com.templenihility.client.model;

import com.templenihility.TempleNihilityMod;
import com.templenihility.client.renderer.VoidSpellRenderState;
import com.templenihility.entity.VoidSpellEntity.SpellKind;
import net.minecraft.client.model.Model;
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

public class VoidSpellModel extends Model<VoidSpellRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "void_spell"), "main");

    private final ModelPart floatingSword;
    private final ModelPart wingFeather;
    private final ModelPart judgmentLance;
    private final ModelPart crystalBolt;
    private final ModelPart monolithPillar;
    private final ModelPart aegisShield;

    public VoidSpellModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.floatingSword = root.getChild("floating_sword");
        this.wingFeather = root.getChild("wing_feather");
        this.judgmentLance = root.getChild("judgment_lance");
        this.crystalBolt = root.getChild("crystal_bolt");
        this.monolithPillar = root.getChild("monolith_pillar");
        this.aegisShield = root.getChild("aegis_shield");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation def = CubeDeformation.NONE;

        // 1. Detached Floating Void Greatsword (points along +Z forward motion!)
        root.addOrReplaceChild("floating_sword",
            CubeListBuilder.create()
                .texOffs(100, 36).addBox(-0.7f, -0.7f, -10.0f, 1.4f, 1.4f, 5.5f, def)
                .texOffs(100, 36).addBox(-3.6f, -0.9f, -4.5f, 7.2f, 1.8f, 1.8f, def)
                .texOffs(100, 8).addBox(-1.4f, -0.55f, -2.7f, 2.8f, 1.1f, 16.5f, def)
                .texOffs(100, 8).addBox(-0.85f, -0.4f, 13.8f, 1.7f, 0.8f, 4.5f, def),
            PartPose.ZERO);

        // 2. Razor Obsidian Wing-Feather Blade (points along +Z)
        root.addOrReplaceChild("wing_feather",
            CubeListBuilder.create()
                .texOffs(76, 0).addBox(-1.1f, -0.4f, -7.0f, 2.2f, 0.8f, 14.0f, def)
                .texOffs(88, 0).addBox(-0.55f, -0.3f, 7.0f, 1.1f, 0.6f, 4.0f, def),
            PartPose.ZERO);

        // 3. Towering Vertical Astrolabe Judgment Lance (points straight down -Y)
        root.addOrReplaceChild("judgment_lance",
            CubeListBuilder.create()
                .texOffs(88, 0).addBox(-4.2f, -26.0f, -0.7f, 8.4f, 1.4f, 1.4f, def)
                .texOffs(88, 0).addBox(-0.7f, -26.0f, -4.2f, 1.4f, 1.4f, 8.4f, def)
                .texOffs(100, 8).addBox(-1.4f, -34.0f, -1.4f, 2.8f, 28.0f, 2.8f, def)
                .texOffs(88, 0).addBox(-0.8f, -6.0f, -0.8f, 1.6f, 8.0f, 1.6f, def),
            PartPose.ZERO);

        // 4. Believer Faceted Focus-Crystal Bolt (diamond shard pointing +Z)
        root.addOrReplaceChild("crystal_bolt",
            CubeListBuilder.create()
                .texOffs(88, 8).addBox(-1.5f, -1.5f, -4.5f, 3.0f, 3.0f, 9.0f, def)
                .texOffs(88, 0).addBox(-0.8f, -0.8f, 4.5f, 1.6f, 1.6f, 3.5f, def)
                .texOffs(88, 0).addBox(-0.8f, -0.8f, -7.0f, 1.6f, 1.6f, 2.5f, def),
            PartPose.ZERO);

        // 5. Envoy Detached Runic Monolith Tablet (upright obsidian-gold slab that clamps inward!)
        root.addOrReplaceChild("monolith_pillar",
            CubeListBuilder.create()
                .texOffs(88, 8).addBox(-3.0f, -11.0f, -1.4f, 6.0f, 22.0f, 2.8f, def)
                .texOffs(88, 0).addBox(-3.6f, -9.5f, -0.8f, 7.2f, 2.0f, 1.6f, def)
                .texOffs(88, 0).addBox(-3.6f, 7.5f, -0.8f, 7.2f, 2.0f, 1.6f, def),
            PartPose.ZERO);

        // 6. Grand Envoy Hurled Aegis Shield Pillar (broad beveled obsidian-gold bastion shield!)
        root.addOrReplaceChild("aegis_shield",
            CubeListBuilder.create()
                .texOffs(88, 8).addBox(-5.2f, -10.0f, -1.6f, 10.4f, 20.0f, 3.2f, def)
                .texOffs(88, 0).addBox(-6.0f, -11.0f, -1.1f, 12.0f, 2.2f, 2.2f, def)
                .texOffs(88, 0).addBox(-6.0f, 8.8f, -1.1f, 12.0f, 2.2f, 2.2f, def)
                .texOffs(100, 36).addBox(-1.4f, -12.5f, -2.0f, 2.8f, 25.0f, 4.0f, def),
            PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(VoidSpellRenderState state) {
        super.setupAnim(state);
        this.floatingSword.visible = state.kind == SpellKind.FLOATING_SWORD;
        this.wingFeather.visible = state.kind == SpellKind.WING_FEATHER;
        this.judgmentLance.visible = state.kind == SpellKind.JUDGMENT_LANCE;
        this.crystalBolt.visible = state.kind == SpellKind.CRYSTAL_BOLT;
        this.monolithPillar.visible = state.kind == SpellKind.MONOLITH_PILLAR;
        this.aegisShield.visible = state.kind == SpellKind.AEGIS_SHIELD;

        if (state.kind == SpellKind.FLOATING_SWORD) {
            this.floatingSword.zRot = state.ageInTicks * 0.35f;
        } else if (state.kind == SpellKind.WING_FEATHER) {
            this.wingFeather.zRot = state.ageInTicks * 0.55f;
        } else if (state.kind == SpellKind.JUDGMENT_LANCE) {
            this.judgmentLance.yRot = state.ageInTicks * (state.dropping ? 0.55f : 0.18f);
        } else if (state.kind == SpellKind.CRYSTAL_BOLT) {
            this.crystalBolt.zRot = state.ageInTicks * 0.65f;
        } else if (state.kind == SpellKind.MONOLITH_PILLAR) {
            this.monolithPillar.yRot = state.dropping ? 0.0f : state.ageInTicks * 0.14f;
        } else if (state.kind == SpellKind.AEGIS_SHIELD) {
            this.aegisShield.yRot = state.ageInTicks * 0.45f;
        }
    }
}
