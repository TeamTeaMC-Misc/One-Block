package xueluoanping.oneblock.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.client.OneBlockTranslator;

public class ClientUtils {


    public static void playSound(ServerLevel level, BlockPos pos, SoundEvent soundEvents) {
        level.playSound(null, pos, soundEvents, SoundSource.MASTER, 0.6f, 0.5f);
    }

    public static void playSpawnSound(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.CHAIN_HIT);
        playSound(level, pos, SoundEvents.SHULKER_TELEPORT);
    }

    public static void playCloudParticles(ServerLevel level, BlockPos pos) {
        var particleOptions = ParticleTypes.CLOUD; // 选择粒子效果类型
        double xSpeed = 0.5; // X 方向速度
        double ySpeed = 0.7; // Y 方向速度
        double zSpeed = 0.5; // Z 方向速度
        double speedMultiplier = 0.01; // 速度倍率

        int count = 60; // 粒子数量
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particleOptions, false, false, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, count, xSpeed, ySpeed, zSpeed, speedMultiplier);
        }
    }

    public static void informPlayer(MinecraftServer server, Component component) {
        try {
            for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
                serverplayer.sendSystemMessage(ComponentUtils.resolve(ResolutionContext.builder()
                                .withSource(server.createCommandSourceStack())
                                .withEntityOverride(serverplayer)
                                .build(),
                        component, 0), false);
            }
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void informPlayerProgress(MinecraftServer server, String name, int progress) {
        informPlayer(server, Component.translatable("Stage " + name + ", Has dug " + (progress)));
    }

    public static void informNewStage(MinecraftServer server, String stageName) {
        Component component = ChatUtils.parseJson(OneBlockTranslator.getStageTip(stageName));
        informPlayer(server, component);
    }

    public static void tittlePlayer(MinecraftServer server, String component) {
        // ClientboundSetTitleTextPacket clientboundSetTitleTextPacket = new ClientboundSetTitleTextPacket(Component.translatable(component));
        ClientboundSetTitleTextPacket clientboundSetTitleTextPacket = new ClientboundSetTitleTextPacket(Component.translatable(component));

        for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
            serverplayer.connection.send(clientboundSetTitleTextPacket);
        }
    }

    public static void tittlePlayerClean(MinecraftServer server) {
        ClientboundClearTitlesPacket clientboundSetTitleTextPacket = new ClientboundClearTitlesPacket(false);
        for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
            serverplayer.connection.send(clientboundSetTitleTextPacket);
        }
    }

    public static void playHEARTParticles(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, ParticleTypes.HEART, false, false, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, 30, 0.5, 0.7, 0.5, 0.05);
        }
    }

    public static void playASHParticles(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, ParticleTypes.ASH, false, false, pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0.01);
        }
    }

    public static void playFireWorkParticles(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, ParticleTypes.FIREWORK, false, false, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, 120, 0.25, 0.05, 0.25, 0.05);
        }
    }

    public static void placeSound(ServerLevel level, BlockPos basePos, String select) {
        float volume = 1.0f;
        float pitch = 1.0f;
        float minVolume = 0.0f;
        Vec3 vec3_base = basePos.getCenter();
        Holder<SoundEvent> holder = Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(select)));
        SoundSource soundSource = SoundSource.BLOCKS;
        try {
            var values = SoundSource.values();
            boolean found = false;
            for (SoundSource value : values) {
                if (value.getName().equals(Identifier.parse(select).getPath().split("\\.")[0])) {
                    soundSource = value;
                    found = true;
                    break;
                }
            }
            if (!found)
                throw new IllegalArgumentException("Not Found Category for " + select);
        } catch (IllegalArgumentException e) {
            OneBlock.error(e.getMessage());
        }
        // Stop other music
        if (soundSource == SoundSource.MUSIC) {
            ClientboundStopSoundPacket clientboundstopsoundpacket = new ClientboundStopSoundPacket(null, soundSource);
            for (ServerPlayer serverplayer : level.players()) {
                serverplayer.connection.send(clientboundstopsoundpacket);
            }
        }
        double d0 = (double) Mth.square(holder.value().getRange(volume));
        long j = level.getRandom().nextLong();
        for (ServerPlayer serverplayer : level.players()) {
            double d1 = vec3_base.x() - serverplayer.getX();
            double d2 = vec3_base.y() - serverplayer.getY();
            double d3 = vec3_base.z() - serverplayer.getZ();
            double d4 = d1 * d1 + d2 * d2 + d3 * d3;
            Vec3 vec3 = basePos.getCenter();
            float f = volume;
            if (d4 > d0) {
                if (minVolume <= 0.0F) {
                    continue;
                }

                double d5 = Math.sqrt(d4);
                vec3 = new Vec3(serverplayer.getX() + d1 / d5 * 2.0D, serverplayer.getY() + d2 / d5 * 2.0D, serverplayer.getZ() + d3 / d5 * 2.0D);
                f = minVolume;
            }

            serverplayer.connection.send(new ClientboundSoundPacket(holder, soundSource, vec3.x(), vec3.y(), vec3.z(), f, pitch, j));
        }
    }
}
