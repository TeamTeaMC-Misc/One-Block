package xueluoanping.oneblock.handler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageProgress;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


// Todo: clean remain counter in future
public class GlobalDataManager extends SavedData {

    public static final SavedDataType<GlobalDataManager> TYPE = new SavedDataType<>(
            OneBlock.rl("glbal_data"), _ -> new GlobalDataManager(), GlobalDataManager::makeCodec, DataFixTypes.LEVEL
    );

    private static Codec<GlobalDataManager> makeCodec(@Nullable ServerLevel level) {
        return CompoundTag.CODEC.flatXmap(
                tag -> DataResult.success(load(level, tag)),
                data -> DataResult.success(data.save(new CompoundTag())));
    }

    private final Map<BlockPos, StageProgress> chunkPosData = new HashMap<>();

    private String hashStageVersion = "";

    public GlobalDataManager() {
    }

    public GlobalDataManager(CompoundTag tag) {

        ListTag list = tag.getListOrEmpty(OneBlock.MOD_ID);
        for (Tag t : list) {
            CompoundTag manaTag = (CompoundTag) t;
            BlockPos chunkPos = new BlockPos(manaTag.getIntOr("x",0), manaTag.getIntOr("y",0), manaTag.getIntOr("z",0));
            var p = new StageProgress
                    (manaTag.getStringOr("name",""), manaTag.getIntOr("counter",0));
            if (manaTag.contains("bedrockLastTime"))
                p.counter = manaTag.getIntOr("bedrockLastTime",0);
            if (manaTag.contains("remainCounter"))
                p.remainCounter = manaTag.getListOrEmpty("remainCounter");
            if (manaTag.contains("quotaCounter"))
                p.quotaCounter = manaTag.getListOrEmpty("quotaCounter");
            if (manaTag.contains("precedenceCounter"))
                p.precedenceCounter = manaTag.getListOrEmpty("precedenceCounter");
            chunkPosData.put(chunkPos, p);
        }

        if (tag.contains("oneblockVersion")) {
            hashStageVersion = tag.getStringOr("oneblockVersion","");
        }
    }

    // public void update(BlockPos blockPos) {
    //     // if(fluidDrawerControllerSave!=null){
    //     var p = get(blockPos);
    //     p.counter++;
    //     chunkPosData.put(blockPos, p);
    //     setDirty();
    //     // }else fluidDrawerControllerSave=new GlobalDataManager();
    // }

    public void update(BlockPos blockPos, StageProgress progress) {
        chunkPosData.put(blockPos, progress);
        setDirty();
    }

    public StageProgress getOrDefault(BlockPos pos) {
        return chunkPosData.getOrDefault(pos, new StageProgress());
    }

    public StageProgress get(BlockPos pos) {
        return chunkPosData.get(pos);
    }

    public BlockPos remove(BlockPos blockPos) {
        var remove = chunkPosData.remove(blockPos);
        setDirty();
        return remove == null ? null : blockPos;
    }

    public Set<BlockPos> getBlockPos() {
        return chunkPosData.keySet();
    }

    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        chunkPosData.forEach((chunkPos, mana) -> {
            CompoundTag manaTag = new CompoundTag();
            manaTag.putInt("x", chunkPos.getX());
            manaTag.putInt("y", chunkPos.getY());
            manaTag.putInt("z", chunkPos.getZ());
            manaTag.putString("name", mana.name);
            manaTag.putInt("counter", mana.counter);
            manaTag.put("remainCounter", mana.remainCounter);
            manaTag.put("quotaCounter", mana.quotaCounter);
            manaTag.put("precedenceCounter", mana.precedenceCounter);
            list.add(manaTag);
        });
        tag.put(OneBlock.MOD_ID, list);
        tag.putString("hashStageVersion", "");
        return tag;
    }

    public static GlobalDataManager get(ServerLevel serverLevel) {
        var storage = serverLevel.getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    private static GlobalDataManager load(ServerLevel serverLevel, CompoundTag compoundTag) {
        return new GlobalDataManager(compoundTag);
    }

    private static GlobalDataManager create(ServerLevel serverLevel) {
        return new GlobalDataManager();
    }

}
