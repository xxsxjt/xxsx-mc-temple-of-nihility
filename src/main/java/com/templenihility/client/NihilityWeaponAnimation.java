package com.templenihility.client;

import com.mojang.math.Axis;
import com.templenihility.action.PlayerWeaponActions;
import com.templenihility.item.NihilityDagger;
import com.templenihility.item.NihilityGreatsword;
import com.templenihility.item.NihilitySword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class NihilityWeaponAnimation {
    public static void renderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null
            || !player.isUsingItem()
            || player.getUsedItemHand() != event.getHand()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        float ticks = player.getTicksUsingItem(event.getPartialTick());
        if (stack.getItem() instanceof NihilityDagger) {
            animateMissingStep(event, ticks);
        } else if (stack.getItem() instanceof NihilitySword) {
            animateZeroSlashCharge(event, ticks);
        } else if (stack.getItem() instanceof NihilityGreatsword) {
            animateBoundaryBreak(event, ticks);
        }
    }

    private static void animateMissingStep(RenderHandEvent event, float ticks) {
        float windup = Mth.clamp(ticks / 4.0F, 0.0F, 1.0F);
        float release = Mth.clamp((ticks - 4.0F) / (PlayerWeaponActions.MISSING_STEP_DURATION - 4.0F), 0.0F, 1.0F);
        float handSign = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
        event.getPoseStack().translate(0.10F * handSign * windup, -0.08F * windup, -0.42F * release);
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-18.0F * windup + 34.0F * release));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(handSign * (-42.0F * windup + 76.0F * release)));
    }

    private static void animateZeroSlashCharge(RenderHandEvent event, float ticks) {
        float charge = Mth.clamp(ticks / 24.0F, 0.0F, 1.0F);
        float pulse = Mth.sin(Math.min(ticks, 24.0F) * 0.42F) * 2.5F * charge;
        float handSign = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
        event.getPoseStack().translate(-0.08F * handSign * charge, -0.18F * charge, 0.12F * charge);
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-34.0F * charge + pulse));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(handSign * 18.0F * charge));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(handSign * 48.0F * charge));
    }

    private static void animateBoundaryBreak(RenderHandEvent event, float ticks) {
        float windup = Mth.clamp(ticks / 16.0F, 0.0F, 1.0F);
        float waveTime = Math.max(0.0F, ticks - 16.0F);
        float wave = Mth.sin(Mth.clamp(waveTime / 12.0F, 0.0F, 1.0F) * Mth.PI * 3.0F);
        float handSign = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
        event.getPoseStack().translate(-0.18F * handSign * windup, -0.24F * windup, 0.18F * windup - 0.18F * Math.abs(wave));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-52.0F * windup + 38.0F * wave));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(handSign * (22.0F * windup - 28.0F * wave)));
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(handSign * (66.0F * windup - 82.0F * wave)));
    }

    private NihilityWeaponAnimation() {
    }
}
