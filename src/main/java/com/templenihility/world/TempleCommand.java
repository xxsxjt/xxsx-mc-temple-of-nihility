package com.templenihility.world;

import com.mojang.brigadier.CommandDispatcher;
import com.templenihility.TempleNihilityMod;
import com.templenihility.action.CreatureActionPolicy.ActionType;
import com.templenihility.compat.CuriosCompat;
import com.templenihility.energy.VoidPower;
import com.templenihility.entity.*;
import com.templenihility.init.ModBlocks;
import com.templenihility.init.ModEntities;
import com.templenihility.init.ModItems;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class TempleCommand {
    private enum SceneMode {
        NONE, STRUCTURE, ARMOR, CREATURES, BOSS, WEAPONS, CURIOS
    }

    private static SceneMode activeScene = SceneMode.NONE;
    private static long sceneStartTick = -1L;
    private static int sceneStage = 0;
    private static UUID scenePlayerId = null;
    private static BlockPos sceneOrigin = BlockPos.ZERO;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("temple")
            .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || src.getServer().isSingleplayer())
            .then(Commands.literal("shrine").executes(ctx -> spawn(ctx.getSource(), TempleType.SHRINE)))
            .then(Commands.literal("temple").executes(ctx -> spawn(ctx.getSource(), TempleType.TEMPLE)))
            .then(Commands.literal("complex").executes(ctx -> spawn(ctx.getSource(), TempleType.TEMPLE_COMPLEX)))
            .then(Commands.literal("demo").executes(ctx -> setupScene(ctx.getSource(), SceneMode.CREATURES)))
            .then(Commands.literal("scene")
                .then(Commands.literal("structure").executes(ctx -> setupScene(ctx.getSource(), SceneMode.STRUCTURE)))
                .then(Commands.literal("armor").executes(ctx -> setupScene(ctx.getSource(), SceneMode.ARMOR)))
                .then(Commands.literal("creatures").executes(ctx -> setupScene(ctx.getSource(), SceneMode.CREATURES)))
                .then(Commands.literal("boss").executes(ctx -> setupScene(ctx.getSource(), SceneMode.BOSS)))
                .then(Commands.literal("weapons").executes(ctx -> setupScene(ctx.getSource(), SceneMode.WEAPONS)))
                .then(Commands.literal("curios").executes(ctx -> setupScene(ctx.getSource(), SceneMode.CURIOS)))
            )
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§e/temple shrine|temple|complex|scene <structure|armor|creatures|boss|weapons>"), false);
                return 1;
            })
        );
    }

    private static int spawn(CommandSourceStack src, TempleType type) {
        if (!(src.getLevel() instanceof ServerLevel level)) return 0;
        BlockPos pos = BlockPos.containing(src.getPosition());
        try {
            TempleGenerator.generateTemple(level, pos, type);
            src.sendSuccess(() -> Component.literal("§a已生成: " + type.name()), false);
        } catch (Exception e) {
            src.sendFailure(Component.literal("§c生成失败: " + e.getMessage()));
        }
        return 1;
    }

    private static int setupScene(CommandSourceStack src, SceneMode mode) {
        if (!(src.getLevel() instanceof ServerLevel level)) return 0;
        ServerPlayer player = src.getPlayer();
        if (player == null) return 0;

        // CRITICAL: use server-level CommandSourceStack (not non-op singleplayer src!) so difficulty & time ALWAYS apply!
        CommandSourceStack silent = level.getServer().createCommandSourceStack().withSuppressedOutput();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        // Use a consistent clean plateau at fixed coordinates per scene so worlds never overlap or get messy!
        BlockPos origin = new BlockPos(-514, 70, 100);

        // Golden-twilight lighting tuned per scene for maximum readability AND dramatic VFX contrast!
        String timeStr = switch (mode) {
            case STRUCTURE -> "12250"; // Warm golden sunset — gothic cathedral & cyan spires pop crisply
            case ARMOR -> "6000";      // Clear noon daylight — zero shadow ambiguity on armor UV & gold trim
            case CREATURES -> "2200";  // Bright morning daylight — silhouettes, armor & combat sparks read cleanly
            case BOSS -> "1800";       // Crisp morning light — Saint Boss Bar, spacing & void beams stay legible
            case WEAPONS, CURIOS -> "2200"; // Bright daylight — weapon swings, Curios procs & enemy squads stay crystal clear
            default -> "12400";
        };
        level.getServer().getCommands().performPrefixedCommand(silent, "time set " + timeStr);
        level.getServer().getCommands().performPrefixedCommand(silent, "weather clear");
        level.getServer().getCommands().performPrefixedCommand(silent, "difficulty normal");
        level.getServer().getCommands().performPrefixedCommand(silent, "gamerule doDaylightCycle false");
        level.getServer().getCommands().performPrefixedCommand(silent, "gamerule doMobSpawning false");
        TempleNihilityMod.LOGGER.debug("Scene env check diff={} time={}", level.getDifficulty(), timeStr);

        // Clear surrounding entities
        AABB clearBox = new AABB(origin.getX() - 48, origin.getY() - 16, origin.getZ() - 48,
            origin.getX() + 48, origin.getY() + 36, origin.getZ() + 48);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, clearBox)) mob.discard();
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, clearBox)) stand.discard();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, clearBox)) item.discard();

        // Equip player with full Nihility Armor + signature weapons
        equipNihilityKit(player);
        VoidPower.set(player, 100);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());

        switch (mode) {
            case STRUCTURE -> buildStructureScene(level, player, origin);
            case ARMOR -> buildArmorScene(level, player, origin);
            case CREATURES -> buildCreaturesScene(level, player, origin);
            case BOSS -> buildBossScene(level, player, origin);
            case WEAPONS -> buildWeaponsScene(level, player, origin);
            case CURIOS -> buildCuriosScene(level, player, origin);
            default -> {}
        }

        activeScene = mode;
        sceneOrigin = origin;
        sceneStartTick = level.getGameTime();
        sceneStage = 0;
        scenePlayerId = player.getUUID();
        TempleNihilityMod.LOGGER.debug("Temple scene ready: {}", mode.name());
        return 1;
    }

    private static void equipNihilityKit(ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NIHILITY_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NIHILITY_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NIHILITY_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NIHILITY_BOOTS.get()));
        player.getInventory().setItem(0, new ItemStack(ModItems.NIHILITY_SWORD.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.NIHILITY_GREATSWORD.get()));
        player.getInventory().setItem(2, new ItemStack(ModItems.NIHILITY_DAGGER.get()));
        player.getInventory().setItem(3, new ItemStack(ModItems.NIHILITY_AXE.get()));
        player.getInventory().setItem(4, new ItemStack(ModItems.NIHILITY_BARRIER_CORE.get()));
        player.getInventory().setItem(5, new ItemStack(ModItems.NIHILITY_ABYSSAL_DRUM.get()));
    }

    private static void buildPlateau(ServerLevel level, BlockPos origin, int radius) {
        var air = Blocks.AIR.defaultBlockState();
        var deepslate = Blocks.DEEPSLATE.defaultBlockState();
        var polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        var gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
        var rune = ModBlocks.NIHILITY_RUNE_BRICKS.get().defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean rim = Math.abs(dx) == radius || Math.abs(dz) == radius;
                boolean accent = (Math.abs(dx) == 8 && Math.abs(dz) <= 8) || (Math.abs(dz) == 8 && Math.abs(dx) <= 8);
                for (int dy = -2; dy <= -1; dy++) {
                    BlockPos bp = origin.offset(dx, dy, dz);
                    var st = (dy == -1) ? (rim ? gilded : accent ? rune : polished) : deepslate;
                    if (level.getBlockState(bp) != st) level.setBlock(bp, st, 2);
                }
                for (int dy = 0; dy <= 24; dy++) {
                    BlockPos bp = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(bp).isAir()) level.setBlock(bp, air, 2);
                }
            }
        }
    }

    // ==================== SCENE 1: TEMPLE ARCHITECTURE ====================
    private static void buildStructureScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 19);
        // Generate full rebuilt TEMPLE_COMPLEX centered at origin
        TempleGenerator.generateTemple(level, origin, TempleType.TEMPLE_COMPLEX);
        // Also clear entities inside structure so architectural flyover is crystal clean
        AABB box = new AABB(origin.getX() - 32, origin.getY() - 4, origin.getZ() - 32,
            origin.getX() + 32, origin.getY() + 30, origin.getZ() + 32);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        // Start ALREADY positioned & aimed at the exact start of the orbital flyover (zero sky frame!)
        double a0 = -Math.PI * 0.72;
        double r0 = 23.5;
        double cx0 = origin.getX() + 0.5 + Math.cos(a0) * r0;
        double cz0 = origin.getZ() + 0.5 + Math.sin(a0) * r0;
        double cy0 = origin.getY() + 9.5;
        double dx0 = (origin.getX() + 0.5) - cx0;
        double dy0 = (origin.getY() + 6.5) - cy0;
        double dz0 = (origin.getZ() + 2.0) - cz0;
        float yaw0 = (float) (Math.toDegrees(Math.atan2(dz0, dx0)) - 90.0);
        float pitch0 = (float) (-Math.toDegrees(Math.atan2(dy0, Math.sqrt(dx0 * dx0 + dz0 * dz0))));
        player.teleportTo(level, cx0, cy0, cz0, Set.of(), yaw0, pitch0, false);
    }

    // ==================== SCENE 2: ARMOR 360 SHOWCASE ====================
    private static void buildArmorScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 16);
        // Place a Shrine backdrop behind the armor inspection pedestal
        TempleGenerator.generateTemple(level, origin.offset(0, 0, -11), TempleType.SHRINE);
        AABB box = new AABB(origin.getX() - 24, origin.getY() - 4, origin.getZ() - 24,
            origin.getX() + 24, origin.getY() + 20, origin.getZ() + 24);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        // Well-lit golden-obsidian inspection dais with recessed Nihility Lamps
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean lampPos = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                level.setBlock(origin.offset(dx, -1, dz),
                    lampPos ? ModBlocks.NIHILITY_LAMP.get().defaultBlockState()
                        : ((Math.abs(dx) == 3 || Math.abs(dz) == 3)
                            ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                            : ModBlocks.NIHILITY_TILES.get().defaultBlockState()), 2);
            }
        }

        // Spawn 2 Armored Mannequins (Front-facing left, Back-facing right) so BOTH front & back are always crystal clear!
        ArmorStand frontStand = (ArmorStand) createVanilla(level, "armor_stand");
        if (frontStand != null) {
            frontStand.snapTo(origin.getX() - 1.8 + 0.5, origin.getY(), origin.getZ() - 2.2 + 0.5, 0.0f, 0.0f);
            frontStand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NIHILITY_HELMET.get()));
            frontStand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NIHILITY_CHESTPLATE.get()));
            frontStand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NIHILITY_LEGGINGS.get()));
            frontStand.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NIHILITY_BOOTS.get()));
            frontStand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.NIHILITY_SWORD.get()));
            level.addFreshEntity(frontStand);
        }
        ArmorStand backStand = (ArmorStand) createVanilla(level, "armor_stand");
        if (backStand != null) {
            backStand.snapTo(origin.getX() + 2.2 + 0.5, origin.getY(), origin.getZ() - 2.2 + 0.5, 180.0f, 0.0f);
            backStand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.NIHILITY_HELMET.get()));
            backStand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.NIHILITY_CHESTPLATE.get()));
            backStand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.NIHILITY_LEGGINGS.get()));
            backStand.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.NIHILITY_BOOTS.get()));
            backStand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.NIHILITY_GREATSWORD.get()));
            level.addFreshEntity(backStand);
        }

        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 0.05, origin.getZ() + 0.5,
            Set.of(), 180.0f, 6.0f, false);
    }

    // ==================== SCENE 3: T1-T4 CREATURE COMBAT ====================
    private static void buildCreaturesScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 17);
        // Rebuilt Temple Shrine backdrop
        TempleGenerator.generateTemple(level, origin.offset(0, 0, -14), TempleType.SHRINE);
        AABB box = new AABB(origin.getX() - 32, origin.getY() - 4, origin.getZ() - 32,
            origin.getX() + 32, origin.getY() + 24, origin.getZ() + 32);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        // Spawn T1 Believer, T2 Follower, T3 Envoy on the left side facing hostile raiders on the right!
        NihilityCreature t1 = spawnTempleMob(level, ModEntities.NIHILITY_BELIEVER.get(), origin.getX() - 4.0, origin.getY(), origin.getZ() - 3.5);
        NihilityCreature t2 = spawnTempleMob(level, ModEntities.NIHILITY_FOLLOWER.get(), origin.getX() - 3.0, origin.getY(), origin.getZ() - 1.5);
        NihilityCreature t3 = spawnTempleMob(level, ModEntities.NIHILITY_ENVOY.get(), origin.getX() - 5.2, origin.getY(), origin.getZ() - 2.0);
        // Authentic combat stats — no artificial resistance or regeneration crutches

        // Spawn real hostile enemies (Vindicators + Skeleton archers so T2 guards & T3 deflects arrows!)
        Mob e1 = spawnRaider(level, "vindicator", origin.getX() + 3.5, origin.getY(), origin.getZ() - 3.0, new ItemStack(Items.IRON_AXE));
        Mob e2 = spawnRaider(level, "vindicator", origin.getX() + 4.2, origin.getY(), origin.getZ() - 1.0, new ItemStack(Items.IRON_AXE));
        Mob e3 = spawnRaider(level, "skeleton", origin.getX() + 6.5, origin.getY(), origin.getZ() - 2.5, new ItemStack(Items.BOW));
        Mob e4 = spawnRaider(level, "pillager", origin.getX() + 6.2, origin.getY(), origin.getZ() - 0.5, new ItemStack(Items.CROSSBOW));

        // Lock mutual combat targets immediately!
        if (e1 != null) { t1.becomeHostileTo(e1); e1.setTarget(t1); }
        if (e2 != null) { t2.becomeHostileTo(e2); e2.setTarget(t2); }
        if (e3 != null) { t3.becomeHostileTo(e3); e3.setTarget(t3); }
        if (e4 != null) { e4.setTarget(t2); }

        // Elevate observer camera cleanly overlooking the entire arena (closer dramatic framing!)
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 2.9, origin.getZ() + 5.8,
            Set.of(), 180.0f, 18.0f, false);
    }

    // ==================== SCENE 4: T5 SAINT BOSS DUEL ====================
    private static void buildBossScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 19);
        // Generate full rebuilt TEMPLE_COMPLEX and fight inside the North Challenge Court (z = +7..+15)!
        TempleGenerator.generateTemple(level, origin, TempleType.TEMPLE_COMPLEX);
        AABB box = new AABB(origin.getX() - 32, origin.getY() - 4, origin.getZ() - 32,
            origin.getX() + 32, origin.getY() + 30, origin.getZ() + 32);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        // Spawn T5 Nihility Saint right before the Sovereign Throne Arch at (0, 1, 13.8)
        NihilitySaint saint = (NihilitySaint) spawnTempleMob(level, ModEntities.NIHILITY_SAINT.get(),
            origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 13.8);
        saint.setYRot(180.0f);
        saint.setYHeadRot(180.0f);

        // Authentic SURVIVAL MODE duel — no artificial Resistance or Regeneration crutches!
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 1.05, origin.getZ() + 7.2,
            Set.of(), 0.0f, -2.0f, false);
        saint.becomeHostileTo(player);
    }

    // ==================== SCENE 5: WEAPON COMBAT GAUNTLET ====================
    private static void buildWeaponsScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 17);
        TempleGenerator.generateTemple(level, origin.offset(0, 0, -16), TempleType.SHRINE);
        AABB box = new AABB(origin.getX() - 32, origin.getY() - 4, origin.getZ() - 32,
            origin.getX() + 32, origin.getY() + 24, origin.getZ() + 32);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        // Spawn Wave 1 enemy formation directly ahead of player (for Blade of Zero slash!)
        spawnWeaponTargetWave(level, origin, 1);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) m.setTarget(player);

        CuriosCompat.equipShowcaseLoadout(player, "clear");
        // Authentic SURVIVAL MODE weapon test — no artificial Resistance or Regeneration crutches!
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 0.05, origin.getZ() + 4.2,
            Set.of(), 180.0f, 2.0f, false);
    }

    private static void spawnWeaponTargetWave(ServerLevel level, BlockPos origin, int wave) {
        if (wave == 1) {
            // Well-framed squad of armored Husks & Vindicator 9-12 blocks ahead for clean Blade of Zero draw & knockback
            double[][] pts = { {-1.8, -1.6}, {0.0, -1.2}, {1.8, -1.6}, {-1.4, -3.6}, {1.4, -3.6} };
            for (int i = 0; i < pts.length; i++) {
                Mob m = spawnRaider(level, i == 1 ? "vindicator" : "husk",
                    origin.getX() + 0.5 + pts[i][0], origin.getY(), origin.getZ() + pts[i][1],
                    new ItemStack(Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    m.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                    if (i == 1) {
                        m.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 0, false, true));
                        m.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 0, false, true));
                    }
                    m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
                }
            }
        } else if (wave == 2) {
            // Heavy Ravager + 2 Armored Husks framed cleanly for Boundary Greatsword 3-wave fissure pillars
            Mob rav = (Mob) createVanilla(level, "ravager");
            if (rav != null) {
                rav.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() - 4.2, 0.0f, 0.0f);
                level.addFreshEntity(rav);
            }
            for (double x : new double[] {-2.6, 2.6}) {
                Mob h = spawnRaider(level, "husk", origin.getX() + 0.5 + x, origin.getY(), origin.getZ() - 6.0,
                    new ItemStack(Items.IRON_SWORD));
                if (h != null) h.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            }
        } else {
            // Balanced unbuffed-survival squad: vanguard Husks for Missing Step, mid-row for X-slash, iron-helmet Skeleton flankers for Drum & Barrier
            double[][] husks = { {-1.3, 0.0}, {0.0, -0.3}, {1.3, 0.0}, {-1.7, -3.5}, {1.7, -3.5} };
            for (double[] pt : husks) {
                Mob m = spawnRaider(level, "husk", origin.getX() + 0.5 + pt[0], origin.getY(), origin.getZ() + pt[1],
                    new ItemStack(Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 1, false, false));
                }
            }
            for (double x : new double[] {-2.5, 2.5}) {
                spawnRaider(level, "skeleton", origin.getX() + 0.5 + x, origin.getY(), origin.getZ() - 5.8,
                    new ItemStack(Items.BOW));
            }
        }
    }

    private static <T extends NihilityCreature> T spawnTempleMob(ServerLevel level, EntityType<T> type,
                                                                 double x, double y, double z) {
        T entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity != null) {
            entity.snapTo(x, y, z, 0.0f, 0.0f);
            entity.setPersistenceRequired();
            level.addFreshEntity(entity);
        }
        return entity;
    }

    private static Entity createVanilla(ServerLevel level, String vanillaId) {
        var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(vanillaId));
        if (opt.isEmpty()) {
            TempleNihilityMod.LOGGER.warn("Missing entity type for showcase: {}", vanillaId);
            return null;
        }
        return opt.get().create(level, EntitySpawnReason.COMMAND);
    }

    private static Mob spawnRaider(ServerLevel level, String vanillaId,
                                   double x, double y, double z, ItemStack weapon) {
        Entity created = createVanilla(level, vanillaId);
        if (created instanceof Mob mob) {
            mob.snapTo(x, y, z, 180.0f, 0.0f);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setPersistenceRequired();
            if ("skeleton".equals(vanillaId)) {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            }
            level.addFreshEntity(mob);
            return mob;
        }
        return null;
    }

    /** Server-tick scene director driving camera flight, wave spawns, and live combat choreography! */
    public static void levelTick(LevelTickEvent.Post event) {
        if (activeScene == SceneMode.NONE || sceneStartTick < 0L || scenePlayerId == null) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(scenePlayerId);
        if (player == null || player.level() != level) return;

        long elapsed = level.getGameTime() - sceneStartTick;
        // Authentic VoidPower flow during combat (no 1-second infinite refill crutch)

        switch (activeScene) {
            case STRUCTURE -> tickStructureFlyover(level, player, elapsed);
            case CREATURES -> tickCreaturesCombat(level, player, elapsed);
            case BOSS -> tickBossDuel(level, player, elapsed);
            case WEAPONS -> tickWeaponsGauntlet(level, player, elapsed);
            case CURIOS -> tickCuriosShowcase(level, player, elapsed);
            case ARMOR -> {
                if (elapsed > 360) activeScene = SceneMode.NONE;
            }
            default -> {}
        }
    }

    /** Smooth orbital helicopter flyover around the rebuilt Temple Complex (15s = 300 ticks). */
    private static void tickStructureFlyover(ServerLevel level, ServerPlayer player, long elapsed) {
        // Delay 65 ticks (~3.2s) for chat clear & recording start before sweeping orbit
        if (elapsed < 35) return;
        double t = (elapsed - 35) / 260.0;
        if (t > 1.05) {
            activeScene = SceneMode.NONE;
            return;
        }
        double clamped = Math.min(1.0, t);
        // Smooth 320-degree sweeping orbit from South Triumphal Gate -> East Energy Wing -> North Challenge Court -> West Vault Wing -> Central Astral Spire!
        double angle = -Math.PI * 0.72 + clamped * Math.PI * 1.85;
        double radius = 23.5 - Math.sin(clamped * Math.PI) * 3.5;
        double height = 9.5 + Math.sin(clamped * Math.PI) * 7.5;
        double camX = sceneOrigin.getX() + 0.5 + Math.cos(angle) * radius;
        double camZ = sceneOrigin.getZ() + 0.5 + Math.sin(angle) * radius;
        double camY = sceneOrigin.getY() + height;
        double lookX = sceneOrigin.getX() + 0.5;
        double lookY = sceneOrigin.getY() + 6.5;
        double lookZ = sceneOrigin.getZ() + 2.0;
        double dx = lookX - camX;
        double dy = lookY - camY;
        double dz = lookZ - camZ;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        player.teleportTo(level, camX, camY, camZ, Set.of(), yaw, pitch, false);
    }

    /** Live T1-T4 vs Raiders arena director (triggers Follower Intercept, Envoy Rift, Grand Envoy Domain+Shield+Recall!). */
    private static void tickCreaturesCombat(ServerLevel level, ServerPlayer player, long elapsed) {
        AABB arena = new AABB(sceneOrigin.getX() - 22, sceneOrigin.getY() - 4, sceneOrigin.getZ() - 22,
            sceneOrigin.getX() + 22, sceneOrigin.getY() + 16, sceneOrigin.getZ() + 22);

        // Keep enemies focused on Nihility creatures (not wandering away)
        if (elapsed % 15 == 0) {
            List<NihilityCreature> defenders = level.getEntitiesOfClass(NihilityCreature.class, arena, LivingEntity::isAlive);
            List<Mob> enemies = level.getEntitiesOfClass(Mob.class, arena, m -> !(m instanceof NihilityCreature) && m.isAlive());
            for (int i = 0; i < defenders.size() && !enemies.isEmpty(); i++) {
                NihilityCreature def = defenders.get(i);
                if (def.getTarget() == null || !def.getTarget().isAlive()) {
                    def.becomeHostileTo(enemies.get(i % enemies.size()));
                }
            }
            for (int i = 0; i < enemies.size() && !defenders.isEmpty(); i++) {
                Mob en = enemies.get(i);
                if (en.getTarget() == null || !en.getTarget().isAlive()) {
                    en.setTarget(defenders.get(i % defenders.size()));
                }
            }
        }

        // Stage 1 (elapsed 95 = ~2s into recording): Trigger T2 Follower Halberd Intercept + T3 Envoy Rift Direction!
        if (sceneStage == 0 && elapsed >= 95) {
            sceneStage = 1;
            List<Mob> enemies = level.getEntitiesOfClass(Mob.class, arena, m -> !(m instanceof NihilityCreature) && m.isAlive());
            if (!enemies.isEmpty()) {
                for (NihilityCreature c : level.getEntitiesOfClass(NihilityCreature.class, arena)) {
                    if (c instanceof NihilityFollower fl) {
                        fl.triggerShowcaseAction(ActionType.FOLLOWER_INTERCEPT, enemies.get(0));
                    } else if (c instanceof NihilityEnvoy en) {
                        en.triggerShowcaseAction(ActionType.ENVOY_RIFT_DIRECTION, enemies.get(Math.min(1, enemies.size() - 1)));
                    }
                }
            }
        }
        // Stage 2 (elapsed 195 = ~7s into recording): Spawn T4 Grand Envoy + fresh Ravager & Vindicators -> Domain + 4 Shields!
        else if (sceneStage == 1 && elapsed >= 195) {
            sceneStage = 2;
            NihilityGrandEnvoy ge = spawnTempleMob(level, ModEntities.NIHILITY_GRAND_ENVOY.get(),
                sceneOrigin.getX() - 1.5, sceneOrigin.getY(), sceneOrigin.getZ() - 2.5);
            ge.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 2, false, false));
            ge.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, false));

            Mob rav = (Mob) createVanilla(level, "ravager");
            if (rav != null) {
                rav.snapTo(sceneOrigin.getX() + 4.5, sceneOrigin.getY(), sceneOrigin.getZ() - 2.5, 90.0f, 0.0f);
                rav.setTarget(ge);
                level.addFreshEntity(rav);
                ge.becomeHostileTo(rav);
                ge.triggerShowcaseAction(ActionType.GRAND_ENVOY_DOMAIN, rav);
            }
            Mob rv1 = spawnRaider(level, "vindicator", sceneOrigin.getX() + 5.2, sceneOrigin.getY(), sceneOrigin.getZ() - 0.5,
                new ItemStack(Items.IRON_AXE));
            if (rv1 != null) rv1.setTarget(ge);
            Mob rv2 = spawnRaider(level, "skeleton", sceneOrigin.getX() + 6.0, sceneOrigin.getY(), sceneOrigin.getZ() - 4.2,
                new ItemStack(Items.BOW));
            if (rv2 != null) rv2.setTarget(ge);
        }
        // Stage 3 (elapsed 275 = ~11s into recording): Grand Envoy raises 4-Shield Ritual Barrier & clashes!
        else if (sceneStage == 2 && elapsed >= 275) {
            sceneStage = 3;
            List<Mob> enemies = level.getEntitiesOfClass(Mob.class, arena, m -> !(m instanceof NihilityCreature) && m.isAlive());
            for (NihilityGrandEnvoy ge : level.getEntitiesOfClass(NihilityGrandEnvoy.class, arena)) {
                if (!enemies.isEmpty()) {
                    ge.triggerShowcaseAction(ActionType.GRAND_ENVOY_SHIELD, enemies.get(0));
                }
            }
        }
        // End cleanly
        else if (elapsed >= 440) {
            activeScene = SceneMode.NONE;
        }
    }

    /** Live T5 Saint Boss Duel inside Challenge Court (alternates Erasure Line -> Missing Zones -> Void Terminus!). */
    private static void tickBossDuel(ServerLevel level, ServerPlayer player, long elapsed) {
        AABB court = new AABB(sceneOrigin.getX() - 16, sceneOrigin.getY() - 2, sceneOrigin.getZ() - 4,
            sceneOrigin.getX() + 16, sceneOrigin.getY() + 16, sceneOrigin.getZ() + 24);
        List<NihilitySaint> saints = level.getEntitiesOfClass(NihilitySaint.class, court, LivingEntity::isAlive);
        if (saints.isEmpty()) return;
        NihilitySaint saint = saints.get(0);

        if (elapsed % 20 == 0) {
            saint.setTarget(player);
            saint.setAggressive(true);
        }
        // Keep player inside the Challenge Court facing the Saint if Rift Dagger dash carries past him
        if (player.getZ() > sceneOrigin.getZ() + 11.8 || (elapsed == 312)) {
            double px = sceneOrigin.getX() + 0.5;
            double py = sceneOrigin.getY() + 1.05;
            double pz = sceneOrigin.getZ() + 7.6;
            double dx = saint.getX() - px;
            double dy = (saint.getY() + 1.4) - (py + 1.62);
            double dz = saint.getZ() - pz;
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
            player.teleportTo(level, px, py, pz, Set.of(), yaw, pitch, false);
        }

        // 1) Erasure Line Beam (elapsed 95 = ~1.8s into recording)
        if (sceneStage == 0 && elapsed >= 95) {
            sceneStage = 1;
            saint.triggerShowcaseAction(ActionType.SAINT_ERASURE_LINE, player);
        }
        // 2) Missing Zones Void Geysers (elapsed 185 = ~6.2s into recording)
        else if (sceneStage == 1 && elapsed >= 185) {
            sceneStage = 2;
            saint.triggerShowcaseAction(ActionType.SAINT_MISSING_ZONE, player);
        }
        // 3) Void Terminus 6-Pillar Ascension Ultimate (elapsed 270 = ~10.5s into recording)
        else if (sceneStage == 2 && elapsed >= 270) {
            sceneStage = 3;
            saint.triggerShowcaseAction(ActionType.SAINT_VOID_TERMINUS, player);
            NihilityVisualEffects.saintVoidTerminusActivate(saint, saint.position());
        }
        // 4) Second Erasure Line Finale (elapsed 355 = ~14.7s into recording)
        else if (sceneStage == 3 && elapsed >= 355) {
            sceneStage = 4;
            saint.triggerShowcaseAction(ActionType.SAINT_ERASURE_LINE, player);
        } else if (elapsed >= 450) {
            activeScene = SceneMode.NONE;
        }
    }

    /** Spawns Wave 2 & Wave 3 enemy squads seamlessly for Greatsword & Dagger/Drum live weapon tests! */
    private static void tickWeaponsGauntlet(ServerLevel level, ServerPlayer player, long elapsed) {
        AABB arena = new AABB(sceneOrigin.getX() - 24, sceneOrigin.getY() - 4, sceneOrigin.getZ() - 24,
            sceneOrigin.getX() + 24, sceneOrigin.getY() + 16, sceneOrigin.getZ() + 24);
        if (elapsed % 12 == 0) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena, LivingEntity::isAlive)) {
                if (m.getTarget() == null) m.setTarget(player);
            }
        }
        // Wave 2 spawns at elapsed 165 (~5.0s into recording) right before Greatsword Boundary Break!
        if (sceneStage == 0 && elapsed >= 165) {
            sceneStage = 1;
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena)) m.discard();
            player.teleportTo(level, sceneOrigin.getX() + 0.5, sceneOrigin.getY() + 0.05, sceneOrigin.getZ() + 4.4,
                Set.of(), 170.0f, 3.0f, false);
            spawnWeaponTargetWave(level, sceneOrigin, 2);
        }
        // Wave 3 spawns at elapsed 280 (~10.8s into recording) for Rift Dagger Backstab & Missing Mark Detonation
        else if (sceneStage == 1 && elapsed >= 280) {
            sceneStage = 2;
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena)) m.discard();
            player.teleportTo(level, sceneOrigin.getX() + 0.5, sceneOrigin.getY() + 0.05, sceneOrigin.getZ() + 4.0,
                Set.of(), 180.0f, 3.0f, false);
            spawnWeaponTargetWave(level, sceneOrigin, 3);
        }
        // Wave 4 spawns at elapsed 390 (~16.3s into recording) for Nihility Axe Shatter Cleave & 4pc Zero Counter!
        else if (sceneStage == 2 && elapsed >= 390) {
            sceneStage = 3;
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena)) m.discard();
            player.teleportTo(level, sceneOrigin.getX() + 0.5, sceneOrigin.getY() + 0.05, sceneOrigin.getZ() + 3.2,
                Set.of(), 180.0f, 3.0f, false);
            double[][] axeWave = { {-1.5, 0.2}, {0.0, -0.2}, {1.5, 0.2}, {0.0, -1.8} };
            for (int i = 0; i < axeWave.length; i++) {
                Mob m = spawnRaider(level, i == 0 ? "vindicator" : "husk",
                    sceneOrigin.getX() + 0.5 + axeWave[i][0], sceneOrigin.getY(), sceneOrigin.getZ() + axeWave[i][1],
                    new ItemStack(i == 0 ? Items.IRON_AXE : Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    m.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                }
            }
        } else if (elapsed >= 520) {
            activeScene = SceneMode.NONE;
        }
    }

    // ==================== SCENE 6: FOUR-ASPECT CURIOS BUILDS ====================
    private static void buildCuriosScene(ServerLevel level, ServerPlayer player, BlockPos origin) {
        buildPlateau(level, origin, 17);
        TempleGenerator.generateTemple(level, origin.offset(0, 0, -16), TempleType.SHRINE);
        AABB box = new AABB(origin.getX() - 32, origin.getY() - 4, origin.getZ() - 32,
            origin.getX() + 32, origin.getY() + 24, origin.getZ() + 32);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) mob.discard();

        player.setGameMode(GameType.SURVIVAL);
        CuriosCompat.equipShowcaseLoadout(player, "chaos");
        VoidPower.set(player, 120);
        // Start at 45% HP so Chaos Blood Ring (+40% dmg & <40% Chaos Splash) activates right in real combat!
        player.setHealth(Math.min(player.getMaxHealth(), 9.0F));
        player.teleportTo(level, origin.getX() + 0.5, origin.getY() + 0.05, origin.getZ() + 2.8,
            Set.of(), 180.0f, 3.0f, false);
        spawnCuriosStageWave(level, origin, 0);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) m.setTarget(player);
        player.sendOverlayMessage(Component.literal("§d【混沌构筑】血戒燃血 + 手甲五连 + 命定暴击 + 王冠免死过载"));
    }

    private static void spawnCuriosStageWave(ServerLevel level, BlockPos origin, int stage) {
        if (stage == 0) {
            double[][] pts = { {-1.3, 0.2}, {0.0, 0.0}, {1.3, 0.2}, {-1.8, -1.6}, {1.8, -1.6} };
            for (int i = 0; i < pts.length; i++) {
                Mob m = spawnRaider(level, i == 1 ? "vindicator" : "husk",
                    origin.getX() + 0.5 + pts[i][0], origin.getY(), origin.getZ() + pts[i][1],
                    new ItemStack(Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    m.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                }
            }
        } else if (stage == 1) {
            double[][] pts = { {-1.4, 0.3}, {0.0, 0.0}, {1.4, 0.3}, {-1.6, -1.4}, {1.6, -1.4} };
            for (int i = 0; i < pts.length; i++) {
                Mob m = spawnRaider(level, i == 0 ? "vindicator" : "husk",
                    origin.getX() + 0.5 + pts[i][0], origin.getY(), origin.getZ() + pts[i][1],
                    new ItemStack(Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
            }
        } else {
            double[][] melee = { {-1.2, 0.2}, {0.0, -0.2}, {1.2, 0.2} };
            for (double[] pt : melee) {
                Mob m = spawnRaider(level, "husk",
                    origin.getX() + 0.5 + pt[0], origin.getY(), origin.getZ() + pt[1],
                    new ItemStack(Items.IRON_SWORD));
                if (m != null) {
                    m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    m.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                }
            }
            for (double x : new double[] { -3.6, 0.0, 3.6 }) {
                spawnRaider(level, "skeleton",
                    origin.getX() + 0.5 + x, origin.getY(), origin.getZ() - 5.8,
                    new ItemStack(Items.BOW));
            }
        }
    }

    private static void tickCuriosShowcase(ServerLevel level, ServerPlayer player, long elapsed) {
        AABB arena = new AABB(sceneOrigin.getX() - 24, sceneOrigin.getY() - 4, sceneOrigin.getZ() - 24,
            sceneOrigin.getX() + 24, sceneOrigin.getY() + 16, sceneOrigin.getZ() + 24);
        if (elapsed % 12 == 0) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena, LivingEntity::isAlive)) {
                if (m.getTarget() == null) m.setTarget(player);
            }
        }
        // Stage 1: Abyss Immortal Build (elapsed 185 = ~6.5s into recording)
        if (sceneStage == 0 && elapsed >= 185) {
            sceneStage = 1;
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena)) m.discard();
            CuriosCompat.equipShowcaseLoadout(player, "abyss");
            player.setHealth(player.getMaxHealth());
            VoidPower.set(player, 120);
            player.teleportTo(level, sceneOrigin.getX() + 0.5, sceneOrigin.getY() + 0.05, sceneOrigin.getZ() + 2.8,
                Set.of(), 180.0f, 3.0f, false);
            spawnCuriosStageWave(level, sceneOrigin, 1);
            player.sendOverlayMessage(Component.literal("§c【深渊构筑】虹吸过量转魂盾 + 冥焰反震 + 空中重压 + 魂锚"));
        }
        // Stage 2: Void + Nihility Tactical Build (elapsed 320 = ~13.2s into recording)
        else if (sceneStage == 1 && elapsed >= 320) {
            sceneStage = 2;
            for (Mob m : level.getEntitiesOfClass(Mob.class, arena)) m.discard();
            CuriosCompat.equipShowcaseLoadout(player, "void_nihility");
            player.setHealth(player.getMaxHealth());
            VoidPower.set(player, 120);
            player.teleportTo(level, sceneOrigin.getX() + 0.5, sceneOrigin.getY() + 0.05, sceneOrigin.getZ() + 3.2,
                Set.of(), 180.0f, 3.0f, false);
            spawnCuriosStageWave(level, sceneOrigin, 2);
            player.sendOverlayMessage(Component.literal("§b【虚空·虚无构筑】哨兵偏折破甲 + 裂隙拉拽 + 归零薄膜 + 蚀影伏击"));
        } else if (elapsed >= 480) {
            activeScene = SceneMode.NONE;
        }
    }

    /** Preserved compatibility hook called by NihilitySaint.tick() */
    public static void tickShowcaseDirector(ServerLevel level, NihilitySaint saint) {
        // Driven centrally by TempleCommand.levelTick(LevelTickEvent.Post)
    }
}
