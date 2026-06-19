package xueluoanping.oneblock.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.api.StageProgress;
import xueluoanping.oneblock.config.General;
import xueluoanping.oneblock.util.ClientUtils;
import xueluoanping.oneblock.util.NewEntryUtils;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

// @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class Levelhandler {
    public static Levelhandler instance = new Levelhandler();

    // public static OneBlockSave oneBlockSave;
    public static final Map<ServerLevel, GlobalDataManager> oneBlockSaveHolder = new HashMap<>();


    public static GlobalDataManager getSaveData(ServerLevel level) {
        GlobalDataManager globalDataManager = oneBlockSaveHolder.get(level);
        if (globalDataManager == null) {
            OneBlock.LOGGER.error("Null One Block Level Data from Level : {}", level);
        }
        return globalDataManager != null ?
                globalDataManager : oneBlockSaveHolder.put(level, GlobalDataManager.get(level));
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel)
            oneBlockSaveHolder.putIfAbsent(serverLevel, GlobalDataManager.get(serverLevel));
        // for (ServerLevel allLevel : event.getLevel().getServer().getAllLevels()) {
        //     oneBlockSaveHolder.putIfAbsent(allLevel, GlobalDataManager.get(allLevel));
        // }
    }


    @SubscribeEvent
    public void onLevelSave(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()
                && event.getLevel() instanceof ServerLevel serverLevel)
        // oneBlockSaveHolder.clear();
        {
            oneBlockSaveHolder.remove(serverLevel);
        }        // oneBlockSave = null;
    }

    @SubscribeEvent
    public void onTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            var oneBlockSave = getSaveData(serverLevel);
            for (BlockPos pos : oneBlockSave.getBlockPos()) {
                if (serverLevel.isLoaded(pos)
                        && serverLevel.isEmptyBlock(pos)) {
                    serverLevel.setBlock(pos, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_NONE);
                }
            }
        }
    }

    // 只需要保持item位置即可
    @SubscribeEvent
    public void onTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel)
        // OneBlock.logger(System.currentTimeMillis());
        {
            if (StageManager.isNeedCheck()) {
                var old = System.currentTimeMillis();
                // event.getLevel().getServer().overworld()
                StageManager.onCheck(serverLevel.getLevel());
                OneBlock.logger("Check Cost ", System.currentTimeMillis() - old);
            }

            var oneBlockSave = getSaveData(serverLevel);
            for (BlockPos pos : oneBlockSave.getBlockPos()) {
                if (serverLevel.isLoaded(pos))
                    generateBlock(serverLevel.getServer(), oneBlockSave, serverLevel, pos);
            }
            // var server = event.getLevel().getServer();
            // for (ServerLevel level : server.getAllLevels()) {
            //     var oneBlockSave = getSaveData(level);
            //     for (BlockPos pos : oneBlockSave.getBlockPos()) {
            //         if (level.isLoaded(pos))
            //             generateBlock(server, oneBlockSave, level, pos);
            //     }
            // }
        }
        // ServerLevel level = server.overworld();

    }

    private boolean add = false;

    private void generateBlock(MinecraftServer server, GlobalDataManager globalDataManager, ServerLevel level, BlockPos pos) {

        ClientUtils.playASHParticles(level, pos);
        // Player always dig it and we not get
        // Todo:Clean Water in future or clean tick
        if (level.isEmptyBlock(pos) || level.getBlockState(pos).is(Blocks.BARRIER)) {
            if (General.debug.get())
                OneBlock.logger("Start Set at ", System.currentTimeMillis());
            var nowProgress = globalDataManager.getOrDefault(pos);
            var stageHolder = StageManager.getStage(nowProgress.counter);
            var stage = stageHolder.data();
            nowProgress.name = stage.getResName();
            boolean has_gift = stage.getEnd_gift() != null;
            boolean set_gift = has_gift && stageHolder.stageRemainCount() == 1;
            if (stageHolder.isEnd()) {
                if (stage.getBedrock_time() >= 0) {
                    StageManager.setBedrock(level, pos);
                    int lastTime = stage.getBedrock_time() > 0 ? stage.getBedrock_time() :
                            (stage.getCount() > 0 ? stage.getCount() : 50 * 60);
                    nowProgress.setBedrockLastTime(lastTime);
                }
            } else if (set_gift) {
                StageManager.setGif(level, pos, stage.getEnd_gift());
            } else {
                if (stageHolder.isBegin()) {
                    if (!stage.isDisable_message()) {
                        ClientUtils.tittlePlayerClean(server, s -> nowProgress.isMember(s.getUUID()));
                        NewEntryUtils.informNewStage(server, nowProgress, stage);
                    }
                    // reset the record when a new stage
                    nowProgress.cleanLocalCounter();
                    for (StageData.BlockEntry blockEntry : stage.getList()) {
                        if (blockEntry.getMin_times() > 0) {
                            nowProgress.addRemain(blockEntry.getType(), blockEntry.getGlobalId(), blockEntry.getMin_times());
                        }
                        if (blockEntry.getMax_times() > 0) {
                            nowProgress.addQuota(blockEntry.getType(), blockEntry.getGlobalId(), blockEntry.getMin_times());
                        }
                        if (blockEntry.getTimes() > 0) {
                            nowProgress.addRemain(blockEntry.getType(), blockEntry.getGlobalId(), blockEntry.getTimes());
                            nowProgress.addQuota(blockEntry.getType(), blockEntry.getGlobalId(), blockEntry.getTimes());
                        }
                        if (blockEntry.getPrecedence_start() != 0 || blockEntry.getPrecedence_end() != 0) {
                            int start = blockEntry.getPrecedence_start() >= 0 ? blockEntry.getPrecedence_start() : stage.getCount() + blockEntry.getPrecedence_start() + 1;
                            int end = blockEntry.getPrecedence_end() >= 0 ? blockEntry.getPrecedence_end() : stage.getCount() + blockEntry.getPrecedence_end() + 1;
                            nowProgress.addPrecedence(blockEntry.getType(), blockEntry.getGlobalId(), start, end);
                        }
                        if (blockEntry.getPrecedence() != 0) {
                            int precedence = blockEntry.getPrecedence() > 0 ? blockEntry.getPrecedence() : stage.getCount() + blockEntry.getPrecedence() + 1;
                            nowProgress.addPrecedence(blockEntry.getType(), blockEntry.getGlobalId(), precedence, precedence);
                        }
                    }
                }
                // the last is a gift,so we need to prepare
                int remainCount = has_gift ? stageHolder.stageRemainCount() - 1 : stageHolder.stageRemainCount();
                var select = StageManager.setNewBlock(level, pos, stage, remainCount, nowProgress);

                nowProgress.updateLocalCounter(select.getType(), select.getGlobalId());
            }
            nowProgress.updateCounter();
            globalDataManager.update(pos, nowProgress);
            // for (ServerPlayer player : level.players()) {
            //     level.sendParticles(player, ParticleTypes.CLOUD, false, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.5, 0.7, 0.5, 0.025);
            // }

            // for debug
            if (General.debug.get())
                ClientUtils.informPlayerProgress(server, nowProgress.name, nowProgress.counter);

            if (General.debug.get())
                OneBlock.logger("End Set at ", System.currentTimeMillis());
        } else if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
            var oneBlockSaveInstance = globalDataManager.getOrDefault(pos);
            if (oneBlockSaveInstance.bedrockLastTime <= 0) {
                level.removeBlock(pos, false);
            } else {
                int i = oneBlockSaveInstance.bedrockLastTime / 50;
                if (oneBlockSaveInstance.bedrockLastTime % 50 == 0)
                    ClientUtils.tittlePlayer(server, String.valueOf(i), s -> oneBlockSaveInstance.isMember(s.getUUID()));
                oneBlockSaveInstance.updateBedrockLastTime();
                globalDataManager.update(pos, oneBlockSaveInstance);
            }
        }

        if (General.collectItemNearby.get()) {
            try {
                double x = pos.getX() + 0.5;
                double y = level.isWaterAt(pos) ? pos.getY() + 1 + 0.875 : pos.getY() + 1;
                double z = pos.getZ() + 0.5;
                var aabb = new AABB(new Vec3(x - 1.5, y - 1, z - 1.5), new Vec3(x + 1.5, y + 1.25, z + 1.5));
                level.getEntities().get(EntityTypes.ITEM, aabb, itemEntity -> {
                    // if (entity instanceof ItemEntity itemEntity)
                    {
                        itemEntity.syncPacketPositionCodec(x, y, z);
                        itemEntity.moveOrInterpolateTo(new Vec3(x, y, z));
                    }
                    return AbortableIterationConsumer.Continuation.CONTINUE;
                });
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }

    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.level();
        if (player.getRespawnConfig() != null) {
            return;
        }

        var save = Levelhandler.getSaveData(level);

        StageProgress progress = save.getByPlayer(player.getUUID());
        if (progress == null) {
            return;
        }
        BlockPos oneBlockPos = progress.getSpawnPos();
        // int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, oneBlockPos);
        // if (height != oneBlockPos.getY()) {
        //     oneBlockPos = oneBlockPos.mutable().setY(height);
        // }
        player.teleportTo(
                level,
                oneBlockPos.getX() + 0.5,
                oneBlockPos.getY() + 1,
                oneBlockPos.getZ() + 0.5,
                new HashSet<>(),
                player.getYRot(),
                player.getXRot(),
                true
        );
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (!server.isSingleplayer()) {
            return;
        }
        ServerLevel level = player.level();
        if (level.dimension() != Level.OVERWORLD || !CommonSetUp.isOneBlockWorld(level)
                || level.getGameTime() > 100) {
            return;
        }
        CommonSetUp.createTeam(player, level.getRespawnData().pos());
    }

    @SubscribeEvent
    public void inherit(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (!server.isSingleplayer()) {
            return;
        }

        ServerLevel level = player.level();
        GlobalDataManager data = GlobalDataManager.get(level);
        if (data.hasTeam(player.getUUID())) {
            return;
        }
        StageProgress nearestLegacyProgress = data.findNearestUnownedProgress(player.blockPosition());
        if (nearestLegacyProgress == null) {
            return;
        }

        nearestLegacyProgress.setOwner(player.getUUID());
        nearestLegacyProgress.addMember(player.getUUID());

        data.setDirty();

        player.sendSystemMessage(Component.translatable("message.oneblock.team.inherited"));
    }
}
