package com.templenihility.client.renderer;

import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.action.CreatureActionPolicy.Phase;
import com.templenihility.entity.CreatureDispositionPolicy.Disposition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class NihilityRenderState extends LivingEntityRenderState {
    public int tier;
    public Disposition disposition = Disposition.CALM;
    public ActionType action = ActionType.NONE;
    public Phase actionPhase = Phase.IDLE;
    public float actionElapsed;
    public float attackSwing;
    public boolean alert;
    public int ritualShieldCharges;
    public boolean projectileGuarding;
    public boolean projectileDeflecting;

    // NihilitySaint floating-armament synced visual states
    public boolean saintSwordsDeployed;
    public int saintSwordsGuardTicks;
    public int saintWingShieldTicks;
    public int saintWingSalvoTicks;
    public int saintAstrolabeCharge;
    public boolean saintSwordLiberation;
}
