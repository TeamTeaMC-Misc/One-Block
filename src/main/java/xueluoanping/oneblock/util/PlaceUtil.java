package xueluoanping.oneblock.util;

import net.minecraft.IdentifierException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import xueluoanping.oneblock.ModConstants;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.config.General;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class PlaceUtil {

    private static boolean checkLoaded(ServerLevel serverLevel, ChunkPos chunkPos2, ChunkPos chunkPos1) {
        // throw BlockPosArgument.ERROR_NOT_LOADED.create();
        return ChunkPos.rangeClosed(chunkPos2, chunkPos1).allMatch((chunkPos) -> serverLevel.isLoaded(chunkPos.getWorldPosition()));
    }

    public static void placeStructure(ServerLevel level, BlockPos base, Identifier resourceLocation) {
        Structure structure = RegisterFinderUtil.getStructure(level, resourceLocation.toString());
        ChunkGenerator chunkgenerator = level.getChunkSource().getGenerator();
        if (structure == null) {
            OneBlock.error("Failed found " + resourceLocation);
            return;
        }
        StructureStart structurestart = structure.generate(
                level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(resourceLocation).get(),
                level.dimension(),
                level.structureManager().registryAccess(),
                chunkgenerator, chunkgenerator.getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                ChunkPos.containing(base), 0, level, (b) -> true);
        if (!structurestart.isValid()) {
            OneBlock.error("Failed place" + "");
        } else {
            BoundingBox boundingbox = structurestart.getBoundingBox();
            ChunkPos chunkpos = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.minX()), SectionPos.blockToSectionCoord(boundingbox.minZ()));
            ChunkPos chunkpos1 = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.maxX()), SectionPos.blockToSectionCoord(boundingbox.maxZ()));
            if (checkLoaded(level, chunkpos, chunkpos1))
                ChunkPos.rangeClosed(chunkpos, chunkpos1).forEach((chunkPos) -> {
                    structurestart.placeInChunk(level, level.structureManager(), chunkgenerator, level.getRandom(), new BoundingBox(chunkPos.getMinBlockX(), level.getMinY(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), level.getMaxY(), chunkPos.getMaxBlockZ()), chunkPos);
                });
        }
    }

    public static void placeTemplate(ServerLevel level, BlockPos base, Identifier resourceLocation) {
        placeTemplate(level, base, resourceLocation, Rotation.NONE, Mirror.NONE, 1f);
    }

    public static void placeTemplate(ServerLevel level, BlockPos startPos, Identifier resourceLocation, Rotation rotation, Mirror mirror, float integrity) {

        StructureTemplateManager structuretemplatemanager = level.getStructureManager();
        Optional<StructureTemplate> optional;

        try {
            // if (General.debug.get()) {
            //     level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).entrySet();
            // }
            optional = structuretemplatemanager.get(resourceLocation);
        } catch (IdentifierException resourcelocationexception) {
            OneBlock.error("Failed get" + resourceLocation.toString());
            return;
        }

        if (optional.isEmpty()) {
            OneBlock.error("Failed place" + resourceLocation.toString());
        } else {

            StructureTemplate structuretemplate = optional.get();
            // var size = structuretemplate.getSize();
            // startPos=startPos.offset(size.);
            Vec3i center = new Vec3i(startPos.getX(), startPos.getY(), startPos.getZ());


            checkLoaded(level, ChunkPos.containing(startPos), ChunkPos.containing(startPos.offset(structuretemplate.getSize())));
            StructurePlaceSettings structureplacesettings = (new StructurePlaceSettings()).setMirror(mirror).setRotation(rotation);
            // degree of intactness
            if (integrity < 1.0F) {
                structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(integrity)).setRandom(level.getRandom());
            }
            // above to start
            // startPos = startPos.above();
            boolean flag = structuretemplate.placeInWorld(level, startPos, startPos, structureplacesettings, level.getRandom(), Block.UPDATE_CLIENTS);
            OneBlock.logger("Place " + flag + resourceLocation.toString());

            // Clear tick counter? not sure if We need it and it may cause other problem if there are blocks exist
            level.getBlockTicks().clearArea(BoundingBox.fromCorners(center, center.offset(structuretemplate.getSize())));

        }
    }

    public static void placeFeature(ServerLevel level, BlockPos pos, Identifier resourceLocation) {
        ConfiguredFeature<?, ?> configuredfeature = RegisterFinderUtil.getConfiguredFeature(level, resourceLocation.toString());

        if (configuredfeature != null) {
            ChunkPos chunkpos = ChunkPos.containing(pos);
            if (checkLoaded(level, new ChunkPos(chunkpos.x() - 1, chunkpos.z() - 1), new ChunkPos(chunkpos.x() + 1, chunkpos.z() + 1)))
                // above to start
                if (!configuredfeature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos)) {
                    OneBlock.error("Failed place" + resourceLocation.toString());
                }
        }
    }

    //
    public static void placeSelect(ServerLevel level, BlockPos basePos, StageData.BlockEntry select) {
        if (select.getPreprocessing() != null) {
            for (StageData.BlockEntry blockEntry : select.getPreprocessing()) {

                if (level.getRandom().nextFloat() > 1 - blockEntry.getRealChance()) {
                    var backPos = new BlockPos(basePos.getX(), basePos.getY(), basePos.getZ());
                    placeSelect(level, backPos, blockEntry);
                }

            }
        }

        var offsetPos = new BlockPos(basePos.getX() + select.getOffset_x(), basePos.getY() + select.getOffset_y(), basePos.getZ() + select.getOffset_z());


        if (Objects.equals(select.getType(), ModConstants.TYPE_BLOCK)) {
            var block = RegisterFinderUtil.getBlock(select.getId());
            level.setBlockAndUpdate(offsetPos, block.defaultBlockState());
            BoundingBox.encapsulatingPositions(List.of(offsetPos)).ifPresent(
                    boundingBox -> level.getBlockTicks().clearArea(boundingBox)
            );
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_GIFT)) {
            // to avoid some problem the gift use id as res
            var block = RegisterFinderUtil.getBlock(select.getId());
            var state = block.defaultBlockState();
            state = state.hasProperty(BlockStateProperties.FACING) ?
                    state.setValue(BlockStateProperties.FACING, Direction.EAST) : state;
            level.setBlockAndUpdate(offsetPos, state);
            if (level.getBlockEntity(offsetPos) instanceof RandomizableContainerBlockEntity containerBlockEntity)
                containerBlockEntity.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(select.getLoot_table())), level.getRandom().nextLong());
            // containerBlockEntity.setLootTable(Identifier.parse(select.getId()), level.getSeed());
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_ARCHAEOLOGY)) {
            var block = RegisterFinderUtil.getBlock(select.getId());
            level.setBlockAndUpdate(offsetPos, block.defaultBlockState());
            if (level.getBlockEntity(offsetPos) instanceof BrushableBlockEntity brushableBlockEntity)
                brushableBlockEntity.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(select.getLoot_table())), level.getRandom().nextLong());
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_MOB)) {
            var mob = RegisterFinderUtil.getEntity(select.getId());
            if ((General.allowHostileMobs.getAsBoolean() || mob.getCategory().isFriendly())
                    && General.disableMobs.get().stream().noneMatch(s -> {
                try {
                    Pattern pattern = Pattern.compile(s);
                    return pattern.matcher(select.getId()).matches();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            })) {
                for (int i = 0; i < select.getCount(); i++) {
                    var entity = mob.spawn(level, offsetPos.above(2), EntitySpawnReason.NATURAL);
                    if (entity instanceof SkeletonHorse skeletonHorse) {
                        if (level.getRandom().nextFloat() <= General.tamedSkeletonHorseChance.get())
                            skeletonHorse.setTamed(true);
                    }
                    if (entity instanceof AbstractHorse abstractHorse) {
                        if (level.getRandom().nextFloat() <= General.horseWithSaddleChance.get()) {
                            abstractHorse.equipItemIfPossible(level,Items.SADDLE.getDefaultInstance());
                        }
                    }
                    if (entity != null) {
                        entity.moveOrInterpolateTo(new Vec3(offsetPos.getX() + 0.5 + 0.05 * i, offsetPos.getY() + 1.6, offsetPos.getZ() + 0.5 + 0.05 * i));
                        if (General.addMobName.getAsBoolean()) {
                            entity.setCustomName(Component.translatable(General.mobName.get()));
                        }
                    }
                }
                ClientUtils.playSpawnSound(level, offsetPos);
                ClientUtils.playCloudParticles(level, offsetPos);
            }
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_TEMPLATE)) {
            if (General.allowStructure.getAsBoolean()) {
                placeTemplate(level, offsetPos, Identifier.parse(select.getId()));
            }
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_STRUCTURE)) {
            if (General.allowStructure.getAsBoolean()) {
                placeStructure(level, offsetPos, Identifier.parse(select.getId()));
            }
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_CONFIGURED_FEATURE)) {
            if (General.allowFeature.getAsBoolean()) {
                placeFeature(level, offsetPos, Identifier.parse(select.getId()));
            }
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_SOUND)) {
            if (General.allowSound.getAsBoolean()) {
                ClientUtils.placeSound(level, offsetPos, select.getId());
            }
        } else if (Objects.equals(select.getType(), ModConstants.TYPE_COMMAND)) {
            if (General.allowCommand.getAsBoolean()) {
                CommandUtils.performCommand(level.getServer(), offsetPos, select.getId());
            }
        }
    }
}
