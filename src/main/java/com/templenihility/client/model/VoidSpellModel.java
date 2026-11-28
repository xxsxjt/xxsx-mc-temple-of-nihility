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

    public VoidSpellModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.floatingSword = root.getChild("floating_sword");
        this.wingFeather = root.getChild("wing_feather");
        this.judgmentLance = root.getChild("judgment_lance");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation def = CubeDeformation.NONE;

        // 1. Detached Floating Void Greatsword (points along +Z forward motion!)
        root.addOrReplaceChild("floating_sword",
            CubeListBuilder.create()
                // Pommel & hilt
                .texOffs(100, 36).addBox(-0.7f, -0.7f, -10.0f, 1.4f, 1.4f, 5.5f, def)
                // Sleek gold crossguard
                .texOffs(100, 36).addBox(-3.6f, -0.9f, -4.5f, 7.2f, 1.8f, 1.8f, def)
                // Tapered dark-obsidian greatsword blade pointing +Z
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
                // Upper golden astrolabe ring collar
                .texOffs(88, 0).addBox(-4.2f, -26.0f, -0.7f, 8.4f, 1.4f, 1.4f, def)
                .texOffs(88, 0).addBox(-0.7f, -26.0f, -4.2f, 1.4f, 1.4f, 8.4f, def)
                // Tall obsidian-gold spear shaft & tapered point
                .texOffs(100, 8).addBox(-1.4f, -34.0f, -1.4f, 2.8f, 28.0f, 2.8f, def)
                .texOffs(88, 0).addBox(-0.8f, -6.0f, -0.8f, 1.6f, 8.0f, 1.6f, def),
            PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(VoidSpellRenderState state) {
        super.setupAnim(state);
        this.floatingSword.visible = state.kind == SpellKind.FLOATING_SWORD;
        this.wingFeather.visible = state.kind == SpellKind.WING_FEATHER;
        this.judgmentLance.visible = state.kind == SpellKind.JUDGMENT_LANCE;

        if (state.kind == SpellKind.FLOATING_SWORD) {
            this.floatingSword.zRot = state.ageInTicks * 0.35f;
        } else if (state.kind == SpellKind.WING_FEATHER) {
            this.wingFeather.zRot = state.ageInTicks * 0.55f;
        } else if (state.kind == SpellKind.JUDGMENT_LANCE) {
            this.judgmentLance.yRot = state.ageInTicks * (state.dropping ? 0.55f : 0.18f);
        }
    }
}
