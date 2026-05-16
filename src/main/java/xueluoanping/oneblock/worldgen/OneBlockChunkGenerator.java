package xueluoanping.oneblock.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class OneBlockChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<OneBlockChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(OneBlockChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(OneBlockChunkGenerator::generatorSettings)
            ).apply(instance, OneBlockChunkGenerator::new)
    );

    public OneBlockChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }


    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess) {

    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {
        if (!worldGenRegion.getLevel().dimension().equals(Level.OVERWORLD))
            return;

        ChunkPos chunkPos = chunkAccess.getPos();
        BlockPos oneBlockPos = worldGenRegion.getLevel().getRespawnData().pos();
        if (chunkPos.contains(oneBlockPos) && chunkAccess.getBlockState(oneBlockPos).isAir()) {
            // chunkAccess.setBlockState(
            //         oneBlockPos,
            //         ModContents.one_stone.get().defaultBlockState(),
            //         Block.UPDATE_ALL
            // );
            // worldGenRegion.getBlockTicks()
            //         .schedule(new ScheduledTick<>(
            //                 ModContents.one_stone.get(), oneBlockPos,
            //                 0, 0
            //         ));
            chunkAccess.setBlockState(oneBlockPos, Blocks.DIRT.defaultBlockState());
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {

    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long legacyLevelSeed) {
        return ChunkGeneratorStructureState.createForFlat(randomState, legacyLevelSeed, biomeSource, Stream.of());
    }

    // @Override
    // public int getGenDepth() {
    //     return 0;
    // }


    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        return CompletableFuture.completedFuture(chunk);
    }

    // @Override
    // public int getSeaLevel() {
    //     return 0;
    // }
    //
    // @Override
    // public int getMinY() {
    //     return -64;
    // }
    //
    // @Override
    // public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
    //     return 0;
    // }
    //
    // @Override
    // public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
    //     return new NoiseColumn(0, new BlockState[0]);
    // }
    //
    // @Override
    // public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
    //
    // }

    // 下面这些生成阶段都尽量空实现：
    // createBiomes / fillFromNoise / applyCarvers / buildSurface / spawnOriginalMobs
}
