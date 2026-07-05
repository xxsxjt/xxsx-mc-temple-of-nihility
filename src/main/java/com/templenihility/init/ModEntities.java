package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import com.templenihility.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TempleNihilityMod.MOD_ID);

    // 1级 - 信徒
    public static final DeferredHolder<EntityType<?>, EntityType<NihilityBeliever>> NIHILITY_BELIEVER =
        ENTITIES.registerEntityType("nihility_believer", NihilityBeliever::new, MobCategory.CREATURE,
            builder -> builder.sized(0.6f, 1.8f));

    // 2级 - 从者
    public static final DeferredHolder<EntityType<?>, EntityType<NihilityFollower>> NIHILITY_FOLLOWER =
        ENTITIES.registerEntityType("nihility_follower", NihilityFollower::new, MobCategory.CREATURE,
            builder -> builder.sized(0.6f, 1.9f));

    // 3级 - 令使
    public static final DeferredHolder<EntityType<?>, EntityType<NihilityEnvoy>> NIHILITY_ENVOY =
        ENTITIES.registerEntityType("nihility_envoy", NihilityEnvoy::new, MobCategory.CREATURE,
            builder -> builder.sized(0.7f, 2.0f));

    // 4级 - 大令使
    public static final DeferredHolder<EntityType<?>, EntityType<NihilityGrandEnvoy>> NIHILITY_GRAND_ENVOY =
        ENTITIES.registerEntityType("nihility_grand_envoy", NihilityGrandEnvoy::new, MobCategory.CREATURE,
            builder -> builder.sized(0.8f, 2.2f));

    // 5级 - 圣灵
    public static final DeferredHolder<EntityType<?>, EntityType<NihilitySaint>> NIHILITY_SAINT =
        ENTITIES.registerEntityType("nihility_saint", NihilitySaint::new, MobCategory.CREATURE,
            builder -> builder.sized(1.0f, 2.5f));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
