package com.templenihility.client.model;

import com.templenihility.TempleNihilityMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

public class NihilityFlowModel extends NihilityCreatureModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, "nihility_flow"), "main");

    public NihilityFlowModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return NihilityCreatureModel.createLayer(0.06f);
    }
}
