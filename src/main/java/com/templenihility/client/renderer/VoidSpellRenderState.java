package com.templenihility.client.renderer;

import com.templenihility.entity.VoidSpellEntity.SpellKind;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class VoidSpellRenderState extends EntityRenderState {
    public SpellKind kind = SpellKind.WING_FEATHER;
    public float yRot;
    public float xRot;
    public boolean returning;
    public boolean dropping;
}
