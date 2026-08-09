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
    public boolean alert;
    public int ritualShieldCharges;
    public boolean projectileGuarding;
    public boolean projectileDeflecting;
}
