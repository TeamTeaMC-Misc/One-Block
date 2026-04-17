package xueluoanping.oneblock;

import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforgespi.locating.IModFile;
import xueluoanping.oneblock.Item.FantasyBraceletItem;
import xueluoanping.oneblock.api.ModFilePackResources;
import xueluoanping.oneblock.block.BlockOne;
import xueluoanping.oneblock.config.General;
import xueluoanping.oneblock.loot.modifier.AddLootTableModifier;
import xueluoanping.oneblock.util.Platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModContents {
    public static final DeferredRegister<Item> ITEM_DEFERRED_REGISTER = DeferredRegister.create(Registries.ITEM, OneBlock.MOD_ID);
    public static final DeferredRegister<Block> BLOCK_DEFERRED_REGISTER = DeferredRegister.create(Registries.BLOCK, OneBlock.MOD_ID);
    // public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPE_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OneBlock.MOD_ID);

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OneBlock.MOD_ID);

    public static final DeferredHolder<Block, BlockOne> one_stone = BLOCK_DEFERRED_REGISTER.register("one_stone", () -> new BlockOne(BlockBehaviour.Properties.of()
            .strength(-1.0F, 3600000.8F).noLootTable().noTerrainParticles().pushReaction(PushReaction.BLOCK)
            .sound(SoundType.AMETHYST).noOcclusion().setId(ResourceKey.create(Registries.BLOCK,OneBlock.rl("fantasy_bracelet")))));
    public static final DeferredHolder<Item, FantasyBraceletItem> fantasy_bracelet = ITEM_DEFERRED_REGISTER.register("fantasy_bracelet",
            () -> new FantasyBraceletItem(new Item.Properties().durability(1).setNoCombineRepair().setId(ResourceKey.create(Registries.ITEM,OneBlock.rl("fantasy_bracelet")))));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddLootTableModifier>> ADD_LOOT_TABLE = LOOT_MODIFIERS.register("add_loot_table", AddLootTableModifier.CODEC::get);

    private static final PackSelectionConfig FEATURE_SELECTION_CONFIG = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);

    public static KnownPack knowPack(String pName) {
        return new KnownPack(OneBlock.MOD_ID, pName, SharedConstants.getCurrentVersion().dataVersion().toString());
    }

    public static void onAddPackFindersEvent(AddPackFindersEvent event) {

        if (event.getPackType() == PackType.SERVER_DATA) {
            IModFile modFile = Platform.getModFile(OneBlock.MOD_ID);

            General.enableList.forEach(
                    (pack, booleanValue) -> {
                        if (booleanValue.get())
                        {

                            String packID = "oneblock-extra-" + pack;
                            var packLocationInfo = new PackLocationInfo(
                                    packID, Component.translatable(pack), PackSource.BUILT_IN, Optional.of(knowPack(packID))
                            );
                            // event.addPackFinders(OneBlock.rl(packID),PackType.SERVER_DATA, (Component) Component.translatable(packID),
                            //     new ModFilePackResources(packLocationInfo,modFile,"datapacks/" + packID),true, Pack.Position.BOTTOM);

                            event.addRepositorySource(consumer -> consumer.accept(
                                    Pack.readMetaAndCreate(packLocationInfo,
                                            new ModFilePackResources.PathResourcesSupplier(modFile,Path.of("datapacks/" + packID)),
                                            PackType.SERVER_DATA,
                                            FEATURE_SELECTION_CONFIG)));
                        }
                    }
            );

            for (String packID : List.of("ija-one-block", OneBlock.MOD_ID)) {
                var packLocationInfo = new PackLocationInfo(
                        packID, Component.translatable(packID), PackSource.BUILT_IN, Optional.of(knowPack(packID)
                ));
                // event.addPackFinders(OneBlock.rl(packID),PackType.SERVER_DATA, (Component) Component.translatable(packID),
                //         new ModFilePackResources(packLocationInfo,modFile,"datapacks/" + packID),true, Pack.Position.BOTTOM);
                event.addRepositorySource(consumer -> consumer.accept(
                        Pack.readMetaAndCreate(packLocationInfo,
                                new ModFilePackResources.PathResourcesSupplier(modFile,Path.of("datapacks/" + packID)),
                                PackType.SERVER_DATA,
                                FEATURE_SELECTION_CONFIG)));
            }

        }
    }
}
