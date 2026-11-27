package com.templenihility.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.templenihility.action.PlayerWeaponActions;
import com.templenihility.item.AshenAxe;
import com.templenihility.item.AshenSword;
import com.templenihility.item.NihilityAxe;
import com.templenihility.item.NihilityDagger;
import com.templenihility.item.NihilityGreatsword;
import com.templenihility.item.NihilitySword;
import com.templenihility.item.RiftAxe;
import com.templenihility.item.RiftBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class NihilityWeaponAnimation {
    private enum ClientSkill {
        NONE,
        ZERO_SLASH_RELEASE,
        BOUNDARY_BREAK,
        MISSING_STEP,
        AXE_SHATTER,
        RIFT_CUT
    }

    private static ClientSkill activeSkill = ClientSkill.NONE;
    private static InteractionHand activeHand = InteractionHand.MAIN_HAND;
    private static long skillStartTick = -1L;
    private static int skillDurationTicks = 0;

    private static float prevSwingProgress = 0.0F;
    private static int meleeComboParity = 0;

    public static final IClientItemExtensions WEAPON_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                               ItemStack itemInHand, float partialTick,
                                               float equipProcess, float swingProcess) {
            return applyFirstPersonTransform(poseStack, player, arm, itemInHand, partialTick, equipProcess, swingProcess);
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            Minecraft mc = Minecraft.getInstance();
            if (entity != mc.player || mc.level == null) {
                return null;
            }
            if (stack.getItem() instanceof NihilitySword && entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                return HumanoidModel.ArmPose.THROW_TRIDENT;
            }
            float skillAge = getActiveSkillAge(mc, hand, 0.0F);
            if (skillAge >= 0.0F) {
                if (activeSkill == ClientSkill.BOUNDARY_BREAK && skillAge < 16.0F) {
                    return HumanoidModel.ArmPose.THROW_TRIDENT;
                }
                if (activeSkill == ClientSkill.MISSING_STEP && skillAge < 3.5F) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }
                if (activeSkill == ClientSkill.AXE_SHATTER && skillAge < 4.0F) {
                    return HumanoidModel.ArmPose.THROW_TRIDENT;
                }
            }
            return null;
        }
    };

    /** Trigger client-side skill choreography on right-click for Greatsword & Dagger (persists after button release!). */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.level == null) return;
        ItemStack stack = event.getItemStack();
        if (mc.player.getCooldowns().isOnCooldown(stack)) return;
        Item item = stack.getItem();
        if (item instanceof NihilityGreatsword) {
            startSkill(ClientSkill.BOUNDARY_BREAK, event.getHand(), PlayerWeaponActions.BOUNDARY_BREAK_DURATION, mc.level.getGameTime());
        } else if (item instanceof NihilityDagger) {
            startSkill(ClientSkill.MISSING_STEP, event.getHand(), PlayerWeaponActions.MISSING_STEP_DURATION, mc.level.getGameTime());
        }
    }

    /** Trigger explosive iaijutsu cross-slash when releasing a charged Blade of Zero! */
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.level == null) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof NihilitySword)) return;
        int chargeTicks = stack.getUseDuration(mc.player) - event.getDuration();
        if (chargeTicks >= 8) {
            startSkill(ClientSkill.ZERO_SLASH_RELEASE, mc.player.getUsedItemHand(), 12, mc.level.getGameTime());
        }
    }

    public static void handleClientSkillCallback(net.minecraft.world.entity.player.Player player, InteractionHand hand, String skillName) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null) return;
        long now = mc.level.getGameTime();
        switch (skillName) {
            case "BOUNDARY_BREAK" -> startSkill(ClientSkill.BOUNDARY_BREAK, hand, PlayerWeaponActions.BOUNDARY_BREAK_DURATION, now);
            case "MISSING_STEP" -> startSkill(ClientSkill.MISSING_STEP, hand, PlayerWeaponActions.MISSING_STEP_DURATION, now);
            case "ZERO_SLASH_RELEASE" -> startSkill(ClientSkill.ZERO_SLASH_RELEASE, hand, 12, now);
            case "AXE_SHATTER" -> startSkill(ClientSkill.AXE_SHATTER, hand, 14, now);
            case "RIFT_CUT" -> startSkill(ClientSkill.RIFT_CUT, hand, 10, now);
            default -> {}
        }
    }

    private static void startSkill(ClientSkill skill, InteractionHand hand, int duration, long gameTime) {
        activeSkill = skill;
        activeHand = hand;
        skillDurationTicks = duration;
        skillStartTick = gameTime;
    }

    private static float getActiveSkillAge(Minecraft mc, InteractionHand hand, float partialTick) {
        if (activeSkill == ClientSkill.NONE || mc.level == null || activeHand != hand) {
            return -1.0F;
        }
        float age = (float) (mc.level.getGameTime() - skillStartTick) + partialTick;
        if (age < 0.0F || age > skillDurationTicks) {
            activeSkill = ClientSkill.NONE;
            return -1.0F;
        }
        return age;
    }

    /** Third-person arm & torso swing synchronization so all weapon skills visibly animate on the player avatar! */
    public static void renderPlayerPre(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (event.getRenderState().id != mc.player.getId()) return;
        var state = event.getRenderState();
        float partialTick = event.getPartialTick();
        ItemStack mainStack = mc.player.getMainHandItem();

        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && mainStack.getItem() instanceof NihilitySword) {
            state.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
            return;
        }

        float t = getActiveSkillAge(mc, InteractionHand.MAIN_HAND, partialTick);
        if (t < 0.0F) return;

        switch (activeSkill) {
            case BOUNDARY_BREAK -> {
                if (t < 16.0F) {
                    state.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                    state.attackTime = 0.0F;
                } else if (t < 20.0F) {
                    state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                    state.attackTime = Mth.clamp((t - 16.0F) / 4.0F, 0.08F, 0.96F);
                } else if (t < 24.0F) {
                    state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                    state.attackTime = Mth.clamp((t - 20.0F) / 4.0F, 0.08F, 0.96F);
                } else if (t < 31.0F) {
                    state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                    state.attackTime = Mth.clamp((t - 24.0F) / 7.0F, 0.08F, 0.98F);
                }
            }
            case ZERO_SLASH_RELEASE -> {
                state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                state.attackTime = Mth.clamp(t / 10.0F, 0.08F, 0.98F);
            }
            case MISSING_STEP -> {
                if (t < 3.5F) {
                    state.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                } else {
                    state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                    state.attackTime = Mth.clamp((t - 3.5F) / 7.5F, 0.08F, 0.98F);
                }
            }
            case AXE_SHATTER -> {
                if (t < 4.0F) {
                    state.rightArmPose = HumanoidModel.ArmPose.THROW_TRIDENT;
                    state.attackTime = 0.0F;
                } else if (t < 11.5F) {
                    state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                    state.attackTime = Mth.clamp((t - 4.0F) / 7.5F, 0.08F, 0.98F);
                }
            }
            case RIFT_CUT -> {
                state.rightArmPose = HumanoidModel.ArmPose.ITEM;
                state.attackTime = Mth.clamp(t / 9.0F, 0.08F, 0.98F);
            }
            default -> {}
        }
    }

    private static boolean isAnimatedWeapon(Item item) {
        return item instanceof NihilitySword
            || item instanceof NihilityGreatsword
            || item instanceof NihilityDagger
            || item instanceof NihilityAxe
            || item instanceof RiftBlade
            || item instanceof RiftAxe
            || item instanceof AshenSword
            || item instanceof AshenAxe;
    }

    /** Complete First-Person weapon grip, charge, skill & alternating melee swing choreography. */
    private static boolean applyFirstPersonTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                                     ItemStack stack, float partialTick,
                                                     float equipProcess, float swingProcess) {
        Item item = stack.getItem();
        if (!isAnimatedWeapon(item)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand = (arm == player.getMainArm()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        float side = (arm == HumanoidArm.RIGHT) ? 1.0F : -1.0F;

        if (hand == InteractionHand.MAIN_HAND) {
            if (swingProcess > 0.0F && prevSwingProgress == 0.0F) {
                meleeComboParity = (meleeComboParity + 1) & 1;
            } else if (swingProcess > 0.0F && swingProcess < prevSwingProgress - 0.35F) {
                meleeComboParity = (meleeComboParity + 1) & 1;
            }
            prevSwingProgress = swingProcess;
        }

        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);

        // 1) Active Right-Click Skill Choreography (takes priority over vanilla swing)
        float skillAge = getActiveSkillAge(mc, hand, partialTick);
        if (skillAge >= 0.0F) {
            if (activeSkill == ClientSkill.ZERO_SLASH_RELEASE && item instanceof NihilitySword) {
                applyZeroSlashRelease(poseStack, side, skillAge);
                return true;
            }
            if (activeSkill == ClientSkill.BOUNDARY_BREAK && item instanceof NihilityGreatsword) {
                applyBoundaryBreakFirstPerson(poseStack, side, skillAge);
                return true;
            }
            if (activeSkill == ClientSkill.MISSING_STEP && item instanceof NihilityDagger) {
                applyMissingStepFirstPerson(poseStack, side, skillAge);
                return true;
            }
            if (activeSkill == ClientSkill.AXE_SHATTER && (item instanceof NihilityAxe || item instanceof RiftAxe)) {
                applyAxeShatterFirstPerson(poseStack, side, skillAge);
                return true;
            }
            if (activeSkill == ClientSkill.RIFT_CUT && item instanceof RiftBlade) {
                applyRiftCutFirstPerson(poseStack, side, skillAge);
                return true;
            }
        }

        // 2) Blade of Zero Charge Stance
        if (item instanceof NihilitySword && player.isUsingItem() && player.getUsedItemHand() == hand) {
            float ticks = player.getTicksUsingItem(partialTick);
            float charge = easeOutCubic(Mth.clamp(ticks / 20.0F, 0.0F, 1.0F));
            float hum = Mth.sin(ticks * 0.85F) * (1.2F + 1.6F * charge) * charge;
            poseStack.translate(side * (-0.14F * charge), 0.08F * charge, -0.10F * charge);
            poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * charge + hum));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * (22.0F * charge)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * (26.0F * charge)));
            return true;
        }

        // 3) Custom Expressive Left-Click Melee Swings per Weapon Class!
        if (swingProcess > 0.001F) {
            if (item instanceof NihilitySword || item instanceof RiftBlade || item instanceof AshenSword) {
                applySwordMeleeSwing(poseStack, side, swingProcess, meleeComboParity);
            } else if (item instanceof NihilityGreatsword) {
                applyGreatswordMeleeSwing(poseStack, side, swingProcess);
            } else if (item instanceof NihilityDagger) {
                applyDaggerMeleeSwing(poseStack, side, swingProcess, meleeComboParity);
            } else if (item instanceof NihilityAxe || item instanceof RiftAxe || item instanceof AshenAxe) {
                applyAxeMeleeSwing(poseStack, side, swingProcess, meleeComboParity);
            }
            return true;
        }

        return true;
    }

    /** Blade of Zero: explosive wide diagonal iaijutsu draw-slash sweeping completely across the screen! */
    private static void applyZeroSlashRelease(PoseStack ps, float side, float t) {
        if (t < 3.5F) {
            float p = easeOutCubic(t / 3.5F);
            float tx = Mth.lerp(p, -0.14F, -0.74F);
            float ty = Mth.lerp(p, 0.08F, -0.22F);
            float tz = Mth.lerp(p, -0.10F, -0.34F);
            ps.translate(side * tx, ty, tz);
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -14.0F, -84.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 22.0F, 52.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 26.0F, 92.0F)));
        } else if (t < 6.0F) {
            ps.translate(side * (-0.74F), -0.22F, -0.34F);
            ps.mulPose(Axis.XP.rotationDegrees(-84.0F));
            ps.mulPose(Axis.YP.rotationDegrees(side * 52.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(side * 92.0F));
        } else {
            float p = easeInOutQuad(Mth.clamp((t - 6.0F) / 6.0F, 0.0F, 1.0F));
            ps.translate(side * Mth.lerp(p, -0.74F, 0.0F), Mth.lerp(p, -0.22F, 0.0F), Mth.lerp(p, -0.34F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -84.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 52.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 92.0F, 0.0F)));
        }
    }

    /** Rift Blade right-click: crisp quick arc slash across center. */
    private static void applyRiftCutFirstPerson(PoseStack ps, float side, float t) {
        if (t < 2.2F) {
            float p = easeOutCubic(t / 2.2F);
            ps.translate(side * (0.10F * p), 0.14F * p, 0.06F * p);
            ps.mulPose(Axis.XP.rotationDegrees(22.0F * p));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (-26.0F * p)));
        } else if (t < 5.5F) {
            float p = easeOutCubic((t - 2.2F) / 3.3F);
            ps.translate(side * Mth.lerp(p, 0.10F, -0.66F), Mth.lerp(p, 0.14F, -0.18F), Mth.lerp(p, 0.06F, -0.32F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 22.0F, -78.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 0.0F, 48.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -26.0F, 82.0F)));
        } else {
            float p = easeInOutQuad(Mth.clamp((t - 5.5F) / 4.5F, 0.0F, 1.0F));
            ps.translate(side * Mth.lerp(p, -0.66F, 0.0F), Mth.lerp(p, -0.18F, 0.0F), Mth.lerp(p, -0.32F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -78.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 48.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 82.0F, 0.0F)));
        }
    }

    /** Boundary Greatsword: 16t dramatic overhead hoist -> Wave 1 Slam -> Wave 2 Reverse Sweep -> Wave 3 Crushing Finish! */
    private static void applyBoundaryBreakFirstPerson(PoseStack ps, float side, float t) {
        if (t < 16.0F) {
            float p = easeOutCubic(t / 16.0F);
            float tremor = Mth.sin(t * 1.4F) * 1.6F * p;
            ps.translate(side * (-0.10F * p), 0.30F * p, 0.10F * p);
            ps.mulPose(Axis.XP.rotationDegrees(44.0F * p + tremor));
            ps.mulPose(Axis.YP.rotationDegrees(side * (-16.0F * p)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (20.0F * p)));
        } else if (t < 20.0F) {
            float p = easeOutCubic((t - 16.0F) / 4.0F);
            ps.translate(side * Mth.lerp(p, -0.10F, -0.58F), Mth.lerp(p, 0.30F, -0.34F), Mth.lerp(p, 0.10F, -0.36F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 44.0F, -98.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, -16.0F, 36.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 20.0F, 72.0F)));
        } else if (t < 24.5F) {
            float p = easeOutCubic((t - 20.0F) / 4.5F);
            ps.translate(side * Mth.lerp(p, -0.58F, 0.16F), Mth.lerp(p, -0.34F, 0.18F), Mth.lerp(p, -0.36F, -0.24F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -98.0F, -32.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 36.0F, -32.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 72.0F, -58.0F)));
        } else if (t < 29.5F) {
            float p = easeOutCubic((t - 24.5F) / 5.0F);
            ps.translate(side * Mth.lerp(p, 0.16F, -0.34F), Mth.lerp(p, 0.18F, -0.40F), Mth.lerp(p, -0.24F, -0.36F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -32.0F, -106.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, -32.0F, 24.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -58.0F, 28.0F)));
        } else {
            float p = easeInOutQuad(Mth.clamp((t - 29.5F) / 9.5F, 0.0F, 1.0F));
            ps.translate(side * Mth.lerp(p, -0.34F, 0.0F), Mth.lerp(p, -0.40F, 0.0F), Mth.lerp(p, -0.36F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -106.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 24.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 28.0F, 0.0F)));
        }
    }

    /** Rift Dagger: Coiled low draw -> Razor-fast lunging stab & cross-rip! */
    private static void applyMissingStepFirstPerson(PoseStack ps, float side, float t) {
        if (t < 3.0F) {
            float p = easeOutCubic(t / 3.0F);
            ps.translate(side * (0.10F * p), -0.08F * p, 0.16F * p);
            ps.mulPose(Axis.XP.rotationDegrees(24.0F * p));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (-20.0F * p)));
        } else if (t < 7.0F) {
            float p = easeOutCubic((t - 3.0F) / 4.0F);
            ps.translate(side * Mth.lerp(p, 0.10F, -0.42F), Mth.lerp(p, -0.08F, 0.04F), Mth.lerp(p, 0.16F, -0.52F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 24.0F, -86.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 0.0F, 34.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -20.0F, 72.0F)));
        } else {
            float p = easeInOutQuad(Mth.clamp((t - 7.0F) / 5.0F, 0.0F, 1.0F));
            ps.translate(side * Mth.lerp(p, -0.42F, 0.0F), Mth.lerp(p, 0.04F, 0.0F), Mth.lerp(p, -0.52F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -86.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 34.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 72.0F, 0.0F)));
        }
    }

    /** Nihility & Rift Axe Right-Click: high shoulder windup -> armor-shattering downward-diagonal cleave -> weighty recovery! */
    private static void applyAxeShatterFirstPerson(PoseStack ps, float side, float t) {
        if (t < 3.5F) {
            float p = easeOutCubic(t / 3.5F);
            ps.translate(side * (0.14F * p), 0.24F * p, 0.10F * p);
            ps.mulPose(Axis.XP.rotationDegrees(40.0F * p));
            ps.mulPose(Axis.YP.rotationDegrees(side * (-16.0F * p)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (-22.0F * p)));
        } else if (t < 8.5F) {
            float p = easeOutCubic((t - 3.5F) / 5.0F);
            ps.translate(side * Mth.lerp(p, 0.14F, -0.52F), Mth.lerp(p, 0.24F, -0.34F), Mth.lerp(p, 0.10F, -0.38F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 40.0F, -98.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, -16.0F, 34.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -22.0F, 56.0F)));
        } else {
            float p = easeInOutQuad(Mth.clamp((t - 8.5F) / 5.5F, 0.0F, 1.0F));
            ps.translate(side * Mth.lerp(p, -0.52F, 0.0F), Mth.lerp(p, -0.34F, 0.0F), Mth.lerp(p, -0.38F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -98.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 34.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 56.0F, 0.0F)));
        }
    }

    /** Axes left-click: heavy alternating diagonal chop & rising hook cleave. */
    private static void applyAxeMeleeSwing(PoseStack ps, float side, float swing, int parity) {
        if (parity == 0) {
            // Heavy overhead diagonal chop
            if (swing < 0.24F) {
                float p = easeOutCubic(swing / 0.24F);
                ps.translate(side * (0.08F * p), 0.20F * p, 0.08F * p);
                ps.mulPose(Axis.XP.rotationDegrees(30.0F * p));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (-16.0F * p)));
            } else if (swing < 0.58F) {
                float p = easeOutCubic((swing - 0.24F) / 0.34F);
                ps.translate(side * Mth.lerp(p, 0.08F, -0.46F), Mth.lerp(p, 0.20F, -0.30F), Mth.lerp(p, 0.08F, -0.32F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 30.0F, -88.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 0.0F, 28.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -16.0F, 48.0F)));
            } else {
                float p = easeInOutQuad((swing - 0.58F) / 0.42F);
                ps.translate(side * Mth.lerp(p, -0.46F, 0.0F), Mth.lerp(p, -0.30F, 0.0F), Mth.lerp(p, -0.32F, 0.0F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -88.0F, 0.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 28.0F, 0.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 48.0F, 0.0F)));
            }
        } else {
            // Heavy side-hook backhand cleave
            if (swing < 0.22F) {
                float p = easeOutCubic(swing / 0.22F);
                ps.translate(side * (-0.34F * p), -0.14F * p, 0.02F * p);
                ps.mulPose(Axis.XP.rotationDegrees(-20.0F * p));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (56.0F * p)));
            } else if (swing < 0.58F) {
                float p = easeOutCubic((swing - 0.22F) / 0.36F);
                ps.translate(side * Mth.lerp(p, -0.34F, 0.10F), Mth.lerp(p, -0.14F, 0.14F), Mth.lerp(p, 0.02F, -0.28F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -20.0F, -64.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 16.0F, -24.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 56.0F, -34.0F)));
            } else {
                float p = easeInOutQuad((swing - 0.58F) / 0.42F);
                ps.translate(side * Mth.lerp(p, 0.10F, 0.0F), Mth.lerp(p, 0.14F, 0.0F), Mth.lerp(p, -0.28F, 0.0F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -64.0F, 0.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, -24.0F, 0.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -34.0F, 0.0F)));
            }
        }
    }

    /** Swords left-click alternating diagonal combo slashes (Forehand -> Backhand). */
    private static void applySwordMeleeSwing(PoseStack ps, float side, float swing, int parity) {
        if (parity == 0) {
            if (swing < 0.20F) {
                float p = swing / 0.20F;
                ps.translate(side * (0.06F * p), 0.10F * p, 0.04F * p);
                ps.mulPose(Axis.XP.rotationDegrees(18.0F * p));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (-20.0F * p)));
            } else if (swing < 0.56F) {
                float p = easeOutCubic((swing - 0.20F) / 0.36F);
                ps.translate(side * Mth.lerp(p, 0.06F, -0.58F), Mth.lerp(p, 0.10F, -0.20F), Mth.lerp(p, 0.04F, -0.28F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 18.0F, -74.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 0.0F, 44.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -20.0F, 78.0F)));
            } else {
                float p = easeInOutQuad((swing - 0.56F) / 0.44F);
                ps.translate(side * Mth.lerp(p, -0.58F, 0.0F), Mth.lerp(p, -0.20F, 0.0F), Mth.lerp(p, -0.28F, 0.0F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -74.0F, 0.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 44.0F, 0.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 78.0F, 0.0F)));
            }
        } else {
            if (swing < 0.22F) {
                float p = swing / 0.22F;
                ps.translate(side * (-0.44F * p), -0.18F * p, -0.06F * p);
                ps.mulPose(Axis.XP.rotationDegrees(-26.0F * p));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (68.0F * p)));
            } else if (swing < 0.58F) {
                float p = easeOutCubic((swing - 0.22F) / 0.36F);
                ps.translate(side * Mth.lerp(p, -0.44F, 0.12F), Mth.lerp(p, -0.18F, 0.16F), Mth.lerp(p, -0.06F, -0.24F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -26.0F, -58.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 20.0F, -28.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 68.0F, -42.0F)));
            } else {
                float p = easeInOutQuad((swing - 0.58F) / 0.42F);
                ps.translate(side * Mth.lerp(p, 0.12F, 0.0F), Mth.lerp(p, 0.16F, 0.0F), Mth.lerp(p, -0.24F, 0.0F));
                ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -58.0F, 0.0F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, -28.0F, 0.0F)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, -42.0F, 0.0F)));
            }
        }
    }

    /** Boundary Greatsword left-click: heavy overhead windup -> crashing downward cleave. */
    private static void applyGreatswordMeleeSwing(PoseStack ps, float side, float swing) {
        if (swing < 0.26F) {
            float p = easeOutCubic(swing / 0.26F);
            ps.translate(side * (-0.06F * p), 0.24F * p, 0.08F * p);
            ps.mulPose(Axis.XP.rotationDegrees(34.0F * p));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (14.0F * p)));
        } else if (swing < 0.60F) {
            float p = easeOutCubic((swing - 0.26F) / 0.34F);
            ps.translate(side * Mth.lerp(p, -0.06F, -0.38F), Mth.lerp(p, 0.24F, -0.34F), Mth.lerp(p, 0.08F, -0.32F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, 34.0F, -96.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 0.0F, 24.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 14.0F, 38.0F)));
        } else {
            float p = easeInOutQuad((swing - 0.60F) / 0.40F);
            ps.translate(side * Mth.lerp(p, -0.38F, 0.0F), Mth.lerp(p, -0.34F, 0.0F), Mth.lerp(p, -0.32F, 0.0F));
            ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(p, -96.0F, 0.0F)));
            ps.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(p, 24.0F, 0.0F)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(p, 38.0F, 0.0F)));
        }
    }

    /** Rift Dagger left-click: razor-fast alternating criss-cross assassin slashes. */
    private static void applyDaggerMeleeSwing(PoseStack ps, float side, float swing, int parity) {
        float arc = Mth.sin(Mth.clamp(swing * 1.15F, 0.0F, 1.0F) * Mth.PI);
        if (parity == 0) {
            ps.translate(side * (-0.44F * arc), -0.06F * arc, -0.34F * arc);
            ps.mulPose(Axis.XP.rotationDegrees(-74.0F * arc));
            ps.mulPose(Axis.YP.rotationDegrees(side * (32.0F * arc)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (72.0F * arc)));
        } else {
            ps.translate(side * (-0.18F * arc), 0.08F * arc, -0.38F * arc);
            ps.mulPose(Axis.XP.rotationDegrees(-82.0F * arc));
            ps.mulPose(Axis.YP.rotationDegrees(side * (-26.0F * arc)));
            ps.mulPose(Axis.ZP.rotationDegrees(side * (-54.0F * arc)));
        }
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0F - Mth.clamp(x, 0.0F, 1.0F);
        return 1.0F - inv * inv * inv;
    }

    private static float easeInOutQuad(float x) {
        float c = Mth.clamp(x, 0.0F, 1.0F);
        return c < 0.5F ? 2.0F * c * c : 1.0F - (-2.0F * c + 2.0F) * (-2.0F * c + 2.0F) * 0.5F;
    }

    private NihilityWeaponAnimation() {
    }
}
