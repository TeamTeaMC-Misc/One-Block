package xueluoanping.oneblock.handler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.util.ClientUtils;

import java.util.List;
import java.util.Map;

// @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class ReloadHandler {
    public static ReloadHandler instance = new ReloadHandler();

    @SubscribeEvent
    public void onAddReloadListener(AddServerReloadListenersEvent event) {
        // event.addListener(network.instance);
        event.addListener(OneBlock.rl("oneblock"),StageManager.instance2);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("skip_to")
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
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("set")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((stackCommandContext) ->
                                        set_stage(stackCommandContext.getSource(), BlockPosArgument.getLoadedBlockPos(stackCommandContext, "pos")))))
        );
        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("remove")
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
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("list_stage_info")
                                .executes((stackCommandContext) ->
                                        list_stage(stackCommandContext.getSource()))));

        dispatcher.register(
                Commands.literal(OneBlock.MOD_ID)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("export")
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
    }

    private int export_all(CommandSourceStack source) {
        for (ResourceKey resourceKey : List.of(Registries.BLOCK, Registries.ITEM, Registries.ENTITY_TYPE)) {
            for (Object entry : ((Registry)BuiltInRegistries.REGISTRY.getOrThrow(resourceKey)).entrySet()) {
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
                            .withHoverEvent(new HoverEvent.ShowText( Component.empty().append("Click it to copy")))
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
