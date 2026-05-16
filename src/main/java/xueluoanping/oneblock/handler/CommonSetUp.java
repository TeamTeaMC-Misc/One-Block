package xueluoanping.oneblock.handler;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.UnknownNullability;
import xueluoanping.oneblock.ModContents;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.api.StageProgress;
import xueluoanping.oneblock.util.ClientUtils;
import xueluoanping.oneblock.worldgen.OneBlockChunkGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

// @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class CommonSetUp {
    public static CommonSetUp instance = new CommonSetUp();

    @SubscribeEvent
    public void onAddReloadListener(AddServerReloadListenersEvent event) {
        // event.addListener(network.instance);
        event.addListener(OneBlock.rl("oneblock"), StageManager.instance2);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .then(Commands.literal("skip_to")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("stage", IdentifierArgument.id())
                                        .suggests((context, builder) -> {
                                            String pre = "";
                                            try {
                                                pre = context.getArgument("stage", Identifier.class).getPath();
                                            } catch (IllegalArgumentException e) {
                                                // e.printStackTrace();
                                            }
                                            String finalPre = pre;
                                            StageManager.STAGE_DATA_LIST
                                                    .stream()
                                                    .map(stageData -> stageData.getIdentifier() + "")
                                                    .filter(s -> s.contains(finalPre)).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .suggests((context, builder) -> {
                                                    var save = Levelhandler.getSaveData(context.getSource().getLevel());
                                                    save.getBlockPos().stream().map(pos -> String.format("%s %s %s", pos.getX(), pos.getY(), pos.getZ())).forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes((stackCommandContext) ->
                                                        skip_to_stage(stackCommandContext.getSource(), IdentifierArgument.getId(stackCommandContext, "stage"), BlockPosArgument.getLoadedBlockPos(stackCommandContext, "pos"))))))
        );
        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .then(Commands.literal("set")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((stackCommandContext) ->
                                        set_stage(stackCommandContext.getSource(), BlockPosArgument.getLoadedBlockPos(stackCommandContext, "pos")))))
        );
        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)

                        .then(Commands.literal("remove")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .suggests((context, builder) -> {
                                            var save = Levelhandler.getSaveData(context.getSource().getLevel());
                                            save.getBlockPos().stream().map(pos -> String.format("%s %s %s", pos.getX(), pos.getY(), pos.getZ())).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes((stackCommandContext) ->
                                                remove_stage(stackCommandContext.getSource(), BlockPosArgument.getLoadedBlockPos(stackCommandContext, "pos")))))


        );
        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .then(Commands.literal("list_stage_info")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes((stackCommandContext) ->
                                        list_stage(stackCommandContext.getSource()))));

        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)

                        .then(Commands.literal("export")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("all").executes((stackCommandContext) ->
                                        export_all(stackCommandContext.getSource())))
                        ));
        // dispatcher.register(
        //         Commands.literal(OneBlock.MOD_ID)
        //                 .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
        //                 .then(Commands.literal("list")
        //                         .then(Commands.literal("block").executes(context -> 1))
        //                         .then(Commands.literal("item").executes(context -> 1))
        //                         .then(Commands.literal("mob").executes(context -> 1))
        //                         .then(Commands.literal("template").executes(context -> 1))
        //                         .then(Commands.literal("structure").executes(context -> 1))
        //                         .then(Commands.literal("feature").executes(context -> 1))
        //                         .then(Commands.literal("sound").executes(context -> 1))
        //                 )
        //
        // );
        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .then(Commands.literal("team")
                                .then(Commands.literal("create")
                                        .executes(ctx -> createTeam(ctx.getSource()))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> createTeam(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getBlockPos(ctx, "pos")
                                                ))
                                        ))
                                .then(Commands.literal("join")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> joinTeam(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player")
                                                ))))
                                .then(Commands.literal("inherit")
                                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .suggests((context, builder) -> {
                                                    var save = Levelhandler.getSaveData(context.getSource().getLevel());
                                                    save.getBlockPos().stream().map(pos -> String.format("%s %s %s", pos.getX(), pos.getY(), pos.getZ())).forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> inheritTeam(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos")
                                                ))
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(ctx -> inheritTeam(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")
                                                        ))
                                                )
                                        )
                                )
                                .then(Commands.literal("leave")
                                        .executes(ctx -> leaveTeam(ctx.getSource()))
                                )
                                .then(Commands.literal("remove")
                                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> removeTeam(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos")
                                                ))
                                        )
                                )
                        )
        );
    }

    public static final int MIN_TEAM_DISTANCE = 512;
    private static final int MIN_TEAM_DISTANCE_SQR = MIN_TEAM_DISTANCE * MIN_TEAM_DISTANCE;

    public static boolean isTooCloseToOtherTeams(BlockPos pos, GlobalDataManager manager) {
        for (var otherPos : manager.getBlockPos()) {
            int dx = pos.getX() - otherPos.getX();
            int dz = pos.getZ() - otherPos.getZ();

            if (dx * dx + dz * dz < MIN_TEAM_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private static int createTeam(@UnknownNullability CommandSourceStack context) throws CommandSyntaxException {
        ServerPlayer player = context.getPlayerOrException();
        ServerLevel level = player.level();
        GlobalDataManager data = GlobalDataManager.get(level);
        return createTeam(context, allocateNewOneBlockPos(data));
    }

    static boolean isOneBlockWorld(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof OneBlockChunkGenerator;
    }

    private static int createTeam(@UnknownNullability CommandSourceStack context, BlockPos oneBlockPos) throws CommandSyntaxException {
        return createTeam(context.getPlayerOrException(), oneBlockPos);
    }

    public static int createTeam(ServerPlayer player, BlockPos oneBlockPos) {
        ServerLevel level = player.level();

        GlobalDataManager data = GlobalDataManager.get(level);

        if (data.hasTeam(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.already_in_team"));
            return 0;
        }

        if (isTooCloseToOtherTeams(oneBlockPos, data)) {
            player.sendSystemMessage(Component.translatable(
                    "commands.oneblock.create.too_close",
                    MIN_TEAM_DISTANCE
            ));
            return 0;
        }

        StageProgress progress = data.createTeam(player.getUUID(), oneBlockPos);

        if (progress == null) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.create_failed"));
            return 0;
        }


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

        player.sendSystemMessage(Component.translatable("message.oneblock.team.created"));
        return 1;
    }

    private static int joinTeam(CommandSourceStack commandSourceStack, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = commandSourceStack.getPlayerOrException();

        GlobalDataManager data = GlobalDataManager.get(player.level());

        if (!data.joinTeam(player.getUUID(), target.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.join_failed"));
            return 0;
        }

        BlockPos spawnPos = data.getByPlayer(player.getUUID()).getSpawnPos();
        player.teleportTo(player.level(), spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, new HashSet<>(), player.getYRot(), player.getXRot(), true);

        player.sendSystemMessage(Component.translatable("message.oneblock.team.joined", target.getDisplayName()));
        return 1;
    }

    private static int inheritTeam(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return inheritTeam(source, player, pos);
    }

    private static int inheritTeam(CommandSourceStack source, ServerPlayer player, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = player.level();

        GlobalDataManager data = GlobalDataManager.get(level);

        if (data.hasTeam(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.already_in_team"));
            return 0;
        }

        StageProgress progress = data.get(pos);

        if (progress == null) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.not_found"));
            return 0;
        }

        progress.setOwner(player.getUUID());
        progress.addMember(player.getUUID());
        progress.setSpawnPos(pos);

        data.update(pos, progress);

        player.sendSystemMessage(Component.translatable("message.oneblock.team.inherited"));
        return 1;
    }

    static BlockPos allocateNewOneBlockPos(GlobalDataManager data) {
        int spacing = 2048;
        int index = data.getBlockPos().size();
        return new BlockPos(index * spacing, 64, 0);
    }

    private static int leaveTeam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();

        GlobalDataManager data = GlobalDataManager.get(level);
        StageProgress progress = data.getByPlayer(player.getUUID());

        if (progress == null) {
            player.sendSystemMessage(Component.translatable("message.oneblock.team.not_in_team"));
            return 0;
        }

        progress.removeMember(player.getUUID());

        if (player.getUUID().equals(progress.getOwner())) {
            progress.setOwner(null);
            data.remove(progress.getSpawnPos());
        }

        data.setDirty();

        player.sendSystemMessage(Component.translatable("message.oneblock.team.left"));
        return 1;
    }

    private static int removeTeam(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        GlobalDataManager data = GlobalDataManager.get(level);

        StageProgress progress = data.get(pos);

        if (progress == null) {
            source.sendFailure(Component.translatable("message.oneblock.team.not_found"));
            return 0;
        }

        data.remove(pos);
        source.sendSuccess(() -> Component.translatable("message.oneblock.team.removed"), true);
        return 1;
    }

    private int export_all(CommandSourceStack source) {
        for (ResourceKey resourceKey : List.of(Registries.BLOCK, Registries.ITEM, Registries.ENTITY_TYPE)) {
            for (Object entry : ((Registry) BuiltInRegistries.REGISTRY.getOrThrow(resourceKey)).entrySet()) {
                if (entry instanceof Map.Entry mapEntry) {
                    OneBlock.logger(mapEntry.getKey());
                }
            }
        }
        return 1;
    }

    private int list_stage(CommandSourceStack source) {
        for (StageData stageData : StageManager.STAGE_DATA_LIST) {
            var stringBuilder = Component.empty().append("Click to Copy " + stageData.getIdentifier())
                    .withStyle((style) -> style
                            .withColor(TextColor.parseColor("#7FFF00").getOrThrow())
                            .withHoverEvent(new HoverEvent.ShowText(Component.empty().append("Click it to copy")))
                            .withClickEvent(new ClickEvent.CopyToClipboard(stageData.toString())));
            ClientUtils.informPlayer(source.getServer(), stringBuilder);
        }
        return 1;
    }

    private int skip_to_stage(CommandSourceStack source, Identifier id, BlockPos pos) {
        int startPos = StageManager.getStageStartPos(id);
        OneBlock.logger(id, pos, startPos, source.getLevel());
        var save = Levelhandler.getSaveData(source.getLevel());
        var progress = save.get(pos);
        if (progress != null) {
            progress.counter = startPos;
            save.update(pos, progress);
            source.getLevel().removeBlock(pos, false);
        } else {
            return 0;
        }
        return 1;
    }

    private int set_stage(CommandSourceStack source, BlockPos pos) {
        OneBlock.logger("Set", pos, source.getLevel());
        var save = Levelhandler.getSaveData(source.getLevel());
        var progress = save.get(pos);
        if (progress == null) {
            save.update(pos, save.getOrDefault(pos));
            source.getLevel().removeBlock(pos, false);
        } else {
            return 0;
        }
        return 1;
    }

    private int remove_stage(CommandSourceStack source, BlockPos pos) {
        OneBlock.logger("Remove", pos, source.getLevel());
        var save = Levelhandler.getSaveData(source.getLevel());
        var progress = save.get(pos);
        if (progress != null) {
            save.remove(pos);
        } else {
            return 0;
        }
        return 1;
    }
}
