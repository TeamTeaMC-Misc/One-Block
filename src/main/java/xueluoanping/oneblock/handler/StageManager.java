package xueluoanping.oneblock.handler;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.*;


import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.OneBlockConfig;
import xueluoanping.oneblock.api.OneBlockSubConfig;
import xueluoanping.oneblock.api.StageProgress;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.util.ClientUtils;
import xueluoanping.oneblock.util.PlaceUtil;
import xueluoanping.oneblock.util.Platform;


// https://github.com/teaconmc/SignMeUp/blob/1.18-forge/src/main/java/org/teacon/signin/data/GuideMapManager.java
public class StageManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setLenient()
            // .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
            .create();

    // public static final network instance = new network(GSON, "stages.config");
    public static final StageManager instance2 = new StageManager(GSON, OneBlock.MOD_ID);
    public static final List<StageData> STAGE_DATA_LIST = new ArrayList<>();
    public static boolean needCheck = false;
    public static OneBlockConfig oneBlockConfigHolder = new OneBlockConfig();


    record StageHolder(StageData data, boolean isBegin, boolean isEnd, int stageRemainCount) {
    }

    public static int getStageStartPos(Identifier resourceLocation) {
        // it means we set the first block
        int pos = 0;
        int size = STAGE_DATA_LIST.size();
        for (int i = 0; i < size; i++) {
            StageData stageData = STAGE_DATA_LIST.get(i);
            if (i == size - 1 || stageData.getIdentifier().compareTo(resourceLocation) == 0) {

                break;
            } else {
                pos += stageData.getCount();
                // because a bedrock cost a counter, so we need to +1 if we have set the bedrock
                pos++;
            }
        }
        return pos;
    }

    public static StageHolder getStage(int lastProgress) {
        StageData stageData = null;
        boolean is_begin = false;
        boolean is_end = false;
        int stageRemainCount = 0;

        int progress = lastProgress;
        int size = STAGE_DATA_LIST.size();
        for (int i = 0; i < size; i++) {
            StageData data = STAGE_DATA_LIST.get(i);
            if (i < size - 1 && data.getCount() > 0) {
                int count = data.getCount();
                count = Math.max(count, 0);
                progress -= data.getCount();
                if (progress < 0) {
                    stageData = data;
                    is_begin = (count + progress == 0 || lastProgress == 0) && count != 0;
                    is_end = lastProgress > 1 && count + progress + 1 == 0;
                    stageRemainCount = -progress;
                    break;
                }
                // because a bedrock cost a counter, so we need to -1 if we have set the bedrock
                progress--;

            } else {
                // a bedrock is set before the stage so we need
                if (progress == -1) {
                    is_end = true;
                } else if (progress == 0) {
                    is_begin = true;
                }
                stageData = data;
                if (data.getCount() < 0) {
                    // due to it was set -1 to go endless so we need update the remain to get the local progress
                    stageRemainCount = -(progress - data.getCount()) - 1;
                } else {
                    progress -= data.getCount();
                    stageRemainCount = -progress;
                }
                break;
            }
        }
        return new StageHolder(stageData, is_begin, is_end, stageRemainCount);
    }

    public static void setBedrock(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
    }

    public static void setGif(ServerLevel level, BlockPos pos, String lootTable) {
        var block = Blocks.CHEST;
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity entity)
            entity.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(lootTable)), level.getSeed());
        ClientUtils.playHEARTParticles(level, pos);
    }

    public static StageData.BlockEntry setNewBlock(ServerLevel level, BlockPos basePos, StageData stage, int remain, StageProgress nowProgress) {
        // remain less than 0 if endless stage
        var select = stage.selectRandomByWeight(level.getRandom(), nowProgress, nowProgress.getRemainAmount() >= remain && remain > 0, stage.getCount() - remain);
        PlaceUtil.placeSelect(level, basePos, select);
        level.getBlockTicks().clearArea(BoundingBox.fromCorners(basePos, basePos));
        return select;
    }

    public StageManager(Gson json, String s) {
        super();
        OneBlock.logger("data init");
    }


    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        HashMap<Identifier, JsonElement> objectObjectHashMap = new HashMap<>();
        scanDirectory(manager, FileToIdConverter.json("oneblock"), objectObjectHashMap);
        return objectObjectHashMap;
    }

    public static <T> void scanDirectory(
            ResourceManager manager, FileToIdConverter lister, Map<Identifier, JsonElement> result
    ) {
        for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                result.put(id, jsonElement);
            } catch (IllegalArgumentException | IOException | JsonParseException var14) {
                OneBlock.LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, var14);
            }
        }
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        OneBlock.logger("Hello Profile");
        STAGE_DATA_LIST.clear();
        Gson gson = new GsonBuilder().create();

        var new_list = new ArrayList<StageData>();
        var additionalStageDataList = new ArrayList<StageData>();
        var subStageConfigList = new ArrayList<OneBlockSubConfig.Sub>();

        objects.forEach((res, json) -> {
            OneBlock.logger(json.toString(), res);
            if (res.getPath().contains("phases")) {
                StageData stageData = gson.fromJson(json, StageData.class);
                // Check mods
                if (stageData.getMods() != null) {
                    if (!Platform.isModsLoaded(stageData.getMods()))
                        return;
                }
                stageData.setIdentifier(res);
                if (stageData.getTarget() == null)
                    new_list.add(stageData);
                else additionalStageDataList.add(stageData);
                OneBlock.logger("Go on", res);
            } else if (res.toString().equals("oneblock:common/config")) {
                oneBlockConfigHolder = gson.fromJson(json, OneBlockConfig.class);
            } else if (res.getPath().equals("common/sub_config")) {
                subStageConfigList.addAll(gson.fromJson(json, OneBlockSubConfig.class).getList());
            }
        });


        // var sub_Stage=new_list.stream()
        //         .filter(stageData1 -> stageData1.getIdentifier().toString().equals(sub.getTarget()))
        //         .findFirst();
        // add sub to STAGE_DATA_LIST
        var subStageMap = new LinkedHashMap<String, List<OneBlockSubConfig.Sub>>();
        for (String stageID : oneBlockConfigHolder.getOrder()) {
            for (OneBlockSubConfig.Sub sub : subStageConfigList) {
                if (stageID.equals(sub.getTarget())) {
                    if (subStageMap.get(stageID) == null) {
                        var varTemp = new ArrayList<OneBlockSubConfig.Sub>();
                        varTemp.add(sub);
                        subStageMap.put(stageID, varTemp);
                    } else {
                        var varTemp = subStageMap.get(stageID);
                        varTemp.add(sub);
                        subStageMap.put(stageID, varTemp);
                    }
                }
            }
        }

        // add to config list
        oneBlockConfigHolder.setOrder(new ArrayList<>(oneBlockConfigHolder.getOrder()));
        oneBlockConfigHolder.setStageLink(new LinkedHashMap<>());
        for (Map.Entry<String, List<OneBlockSubConfig.Sub>> subEntry : subStageMap.entrySet()) {
            var varTemp = subEntry.getValue();
            varTemp.sort(Comparator.comparing(
                    sub -> -sub.getPriority()
            ));
            oneBlockConfigHolder.getOrder()
                    .addAll(1 + oneBlockConfigHolder.getOrder().indexOf(subEntry.getKey()),
                            varTemp.stream().map(OneBlockSubConfig.Sub::getId).toList());
            varTemp.forEach(sub ->
                    oneBlockConfigHolder.getStageLink().put(sub.getId(), sub.getTarget()));
        }


        STAGE_DATA_LIST.addAll(new_list.stream()
                .filter(stageData -> oneBlockConfigHolder.getOrder().contains(stageData.getIdentifier().toString()))
                .toList());
        // sort
        STAGE_DATA_LIST.sort(Comparator.comparing(
                e -> oneBlockConfigHolder.getOrder().indexOf(e.getIdentifier().toString())
        ));


        // add additional target
        for (StageData additionalStage : additionalStageDataList) {
            for (StageData stage : STAGE_DATA_LIST) {
                if (!stage.isDisable_addition()) {
                    if (stage.getIdentifier().toString().equals(additionalStage.getTarget())
                            || oneBlockConfigHolder.matchSubWithAddition(stage.getIdentifier().toString(), additionalStage.getTarget())
                    ) {
                        for (StageData.BlockEntry subEntry : additionalStage.getList()) {
                            subEntry.setFrom(additionalStage.getIdentifier());
                        }
                        stage.setCount(stage.getCount() + additionalStage.getAdd_count());
                        stage.getList().addAll(additionalStage.getList());
                        break;
                    }
                }
            }
        }

        setNeedCheck(true);

    }

    public static void onCheck(ServerLevel level) {

        for (StageData data : StageManager.STAGE_DATA_LIST) {
            data.setList(data.getList().stream().filter(blockEntry -> {
                boolean isValid = blockEntry.isValid(level);
                if (!isValid) {
                    OneBlock.error("Skip error id found in ",
                            blockEntry.getFrom() == null ? data.getIdentifier() : blockEntry.getFrom()
                            , blockEntry);
                }
                return isValid;
            }).collect(Collectors.toList()));

        }
        setNeedCheck(false);
    }

    public static boolean isNeedCheck() {
        return needCheck;
    }

    public static void setNeedCheck(boolean needCheck) {
        StageManager.needCheck = needCheck;
    }
}
