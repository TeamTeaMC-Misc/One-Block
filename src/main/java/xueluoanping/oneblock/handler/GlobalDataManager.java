package xueluoanping.oneblock.handler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageProgress;

import java.util.*;


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
            BlockPos chunkPos = new BlockPos(manaTag.getIntOr("x", 0), manaTag.getIntOr("y", 0), manaTag.getIntOr("z", 0));
            var p = new StageProgress
                    (manaTag.getStringOr("name", ""), manaTag.getIntOr("counter", 0));
            if (manaTag.contains("bedrockLastTime"))
                p.counter = manaTag.getIntOr("bedrockLastTime", 0);
            if (manaTag.contains("remainCounter"))
                p.remainCounter = manaTag.getListOrEmpty("remainCounter");
            if (manaTag.contains("quotaCounter"))
                p.quotaCounter = manaTag.getListOrEmpty("quotaCounter");
            if (manaTag.contains("precedenceCounter"))
                p.precedenceCounter = manaTag.getListOrEmpty("precedenceCounter");

            CompoundTag oldEntriesTag = manaTag.getCompoundOrEmpty("old_entries");
            for (Map.Entry<String, Tag> tagEntry : oldEntriesTag.entrySet()) {
                if (tagEntry.getValue() instanceof ListTag listTag) {
                    Set<String> set = p.oldEntries.computeIfAbsent(tagEntry.getKey(), s -> new LinkedHashSet<>());
                    for (Tag tag1 : listTag) {
                        set.add(tag1.asString().orElse(""));
                    }
                }
            }

            if (manaTag.contains("teamId")) {
                p.setTeamId(UUID.fromString(manaTag.getStringOr("teamId", "")));
            }
            if (manaTag.contains("owner")) {
                p.setOwner(UUID.fromString(manaTag.getStringOr("owner", "")));
            }
            if (manaTag.contains("spawnPos")) {
                p.setSpawnPos(BlockPos.of(manaTag.getLongOr("spawnPos", 0)));
            }

            ListTag membersTag = manaTag.getListOrEmpty("members");
            for (Tag raw : membersTag) {
                CompoundTag memberTag = (CompoundTag) raw;
                p.addMember(UUID.fromString(memberTag.getStringOr("id", "")));
            }
            chunkPosData.put(chunkPos, p);
        }


        hashStageVersion = tag.getStringOr("oneblockVersion", "");

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
            CompoundTag oldEntriesTag = new CompoundTag();
            for (Map.Entry<String, Set<String>> setEntry : mana.oldEntries.entrySet()) {
                ListTag listTag = new ListTag(setEntry.getValue().size());
                oldEntriesTag.put(setEntry.getKey(), listTag);
                for (String s : setEntry.getValue()) {
                    listTag.add(StringTag.valueOf(s));
                }
            }
            manaTag.put("old_entries", oldEntriesTag);

            if (mana.getTeamId() != null) {
                manaTag.putString("teamId", mana.getTeamId().toString());
            }
            if (mana.getOwner() != null) {
                manaTag.putString("owner", mana.getOwner().toString());
            }
            if (mana.getSpawnPos() != null) {
                manaTag.putLong("spawnPos", mana.getSpawnPos().asLong());
            }

            ListTag membersTag = new ListTag();
            for (UUID uuid : mana.getMembers()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putString("id", uuid.toString());
                membersTag.add(memberTag);
            }
            manaTag.put("members", membersTag);
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

    public boolean hasTeam(UUID playerId) {
        return getByPlayer(playerId) != null;
    }

    public StageProgress getByPlayer(UUID playerId) {
        for (StageProgress progress : chunkPosData.values()) {
            if (progress.isMember(playerId)) {
                return progress;
            }
        }
        return null;
    }

    public BlockPos getOneBlockPosByPlayer(UUID playerId) {
        for (Map.Entry<BlockPos, StageProgress> entry : chunkPosData.entrySet()) {
            if (entry.getValue().isMember(playerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public StageProgress createTeam(UUID owner, BlockPos oneBlockPos) {
        if (hasTeam(owner)) {
            return null;
        }

        StageProgress progress = getOrDefault(oneBlockPos);

        progress.setTeamId(UUID.randomUUID());
        progress.setOwner(owner);
        progress.setSpawnPos(oneBlockPos);
        progress.addMember(owner);

        update(oneBlockPos, progress);
        return progress;
    }

    public boolean joinTeam(UUID playerId, UUID targetPlayerId) {
        if (hasTeam(playerId)) {
            return false;
        }

        StageProgress targetTeam = getByPlayer(targetPlayerId);
        if (targetTeam == null) {
            return false;
        }

        targetTeam.addMember(playerId);
        setDirty();
        return true;
    }

    @Nullable
    public StageProgress findNearestUnownedProgress(BlockPos playerPos) {
        StageProgress nearest = null;
        BlockPos npos = null;
        double nearestDistance = Double.MAX_VALUE;

        for (var progress : this.chunkPosData.entrySet()) {
            if (progress.getValue().getOwner() != null) {
                continue;
            }

            BlockPos pos = progress.getKey();
            double distance = pos.distSqr(playerPos);
            if (distance < nearestDistance) {
                npos = pos;
                nearestDistance = distance;
                nearest = progress.getValue();
            }
        }
        if (nearest != null) {
            nearest.setSpawnPos(npos);
        }

        return nearest;
    }
}
