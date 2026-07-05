package com.templenihility.init;

import com.templenihility.TempleNihilityMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    public static final class Items {
        public static final TagKey<Item> NIHILITY_REPAIR_MATERIALS = tag("nihility_repair_materials");

        private Items() {
        }

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, name));
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> INCORRECT_FOR_NIHILITY_TOOL = tag("incorrect_for_nihility_tool");

        private Blocks() {
        }

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TempleNihilityMod.MOD_ID, name));
        }
    }
}
