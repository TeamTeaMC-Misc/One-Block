package xueluoanping.oneblock.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

public class RegisterFinderUtil {

    // BuiltInRegistries.REGISTRY.getOrThrow(Registries.BLOCK)

    public static EntityType<?> getEntity(String s) {
        return getEntity(Identifier.parse(s));
    }

    public static EntityType<?> getEntity(Identifier rs) {
        return BuiltInRegistries.ENTITY_TYPE.get(rs).map(Holder.Reference::value).orElse(null);
    }

    public static Block getBlock(String s) {
        return getBlock(Identifier.parse(s));
    }

    // BuiltInRegistries
    public static Block getBlock(Identifier rs) {
        return BuiltInRegistries.BLOCK.get(rs).map(Holder.Reference::value).orElse(null);
    }

    public static Item getItem(String s) {
        return getItem(Identifier.parse(s));
    }

    public static Item getItem(Identifier rs) {
        return BuiltInRegistries.ITEM.get(rs).map(Holder.Reference::value).orElse(null);
    }

    public static Item getItem(String s, String s2) {
        return getItem(Identifier.fromNamespaceAndPath(s, s2));
    }


    public static Identifier getItemKey(Item s) {
        return BuiltInRegistries.ITEM.getKey(s);
    }

    public static Identifier getBlockKey(Block s) {
        return BuiltInRegistries.BLOCK.getKey(s);
    }

    public static Identifier getEntityKey(EntityType<?> entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity);
    }

    public static SoundEvent getSound(String id) {
        return BuiltInRegistries.SOUND_EVENT.get(Identifier.parse(id)).map(Holder.Reference::value).orElse(null);
    }

    public static Identifier getSoundKey(SoundEvent soundEvent) {
        return BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
    }

    public static Identifier getStructureKey(Identifier rs) {
        var structureType = BuiltInRegistries.STRUCTURE_TYPE.get(rs).map(Holder.Reference::value).orElse(null);
        return structureType == null ? null : BuiltInRegistries.STRUCTURE_TYPE.getKey(structureType);
    }

    public static Structure getStructure(ServerLevel serverLevel, String s) {
        var res = getStructureKey(Identifier.parse(s));
        return res == null ? null : serverLevel.structureManager().registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .get(res).map(Holder.Reference::value).orElse(null);
    }


    // Feature is a type of terrain feature.
    // ConfiguredFeature is a specific terrain feature.
    // PlacedFeature is a terrain feature with specific placement conditions set.
    public static ConfiguredFeature<?, ?> getConfiguredFeature(ServerLevel serverLevel, String s) {
        var res = serverLevel.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(Identifier.parse(s)).map(Holder.Reference::value).orElse(null);
        return res != null ? res : serverLevel.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(Identifier.fromNamespaceAndPath("minecraft", "oak_bees_005")).map(Holder.Reference::value).orElse(null);
    }

    // public static Feature<?> getFeature(String s) {
    //     return BuiltInRegistries.FEATURE.get(Identifier.parse(s));
    // }

    public static boolean checkTemplateKey(ServerLevel serverLevel, String s) {
        StructureTemplateManager structuretemplatemanager = serverLevel.getStructureManager();
        return structuretemplatemanager.listTemplates().anyMatch(ss -> ss.toString().equals(s));
    }


    public static ResourceKey<LootTable> getLootTable(Identifier resourceLocation) {
        return ResourceKey.create(Registries.LOOT_TABLE, resourceLocation);
    }

    public static ResourceKey<LootTable> getLootTable(String s, String s2) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(s, s2));
    }

    public static ResourceKey<LootTable> getLootTable(String s) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(s));
    }
}
