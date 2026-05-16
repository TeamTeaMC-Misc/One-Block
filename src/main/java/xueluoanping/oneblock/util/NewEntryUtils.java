package xueluoanping.oneblock.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootParams;
import xueluoanping.oneblock.ModConstants;
import xueluoanping.oneblock.api.StageData;
import xueluoanping.oneblock.api.StageProgress;

import java.util.*;

// import java.util.*;

public class NewEntryUtils {
    public static void informNewStage(MinecraftServer server, StageProgress globalDataManager, StageData stage) {
        Map<String, List<Component>> entries = collectNewStageEntries(server, globalDataManager, stage);

        String stageId = stage.getIdentifier().toLanguageKey().replace("/", ".");
        Component title = Component.translatable("tip.oneblock.stage." + stageId + ".title");
        Component description = Component.translatable("tip.oneblock.stage." + stageId + ".description");

        Component tip = buildNewStageTip(title, description, entries);
        ClientUtils.informPlayer(server, tip, serverPlayer ->
                globalDataManager.isMember(serverPlayer.getUUID()));
    }

    private static Map<String, List<Component>> collectNewStageEntries(
            MinecraftServer server,
            StageProgress globalDataManager,
            StageData stage
    ) {
        Map<String, List<Component>> result = new LinkedHashMap<>();
        Map<String, Set<String>> newStrings = new LinkedHashMap<>();
        for (StageData.BlockEntry entry : stage.getList()) {
            String type = entry.getType();
            String id = entry.getId(); // 如果你的字段不是 getId()，这里改一下

            if (!Objects.equals(type, ModConstants.TYPE_BLOCK)
                    && !Objects.equals(type, ModConstants.TYPE_MOB)
                    && !Objects.equals(type, ModConstants.TYPE_GIFT)) {
                continue;
            }

            Set<String> oldEntries = globalDataManager.getOldEntries(type);
            if (oldEntries.contains(id)) {
                continue;
            }
            // Set<String> set = newStrings.computeIfAbsent(type, (s) -> new LinkedHashSet<>());

            List<Component> names = getLocalizedEntryNames(server, type,
                    Objects.equals(type, ModConstants.TYPE_GIFT) ? entry.getLoot_table() : id);
            if (!names.isEmpty()) {
                List<Component> newCompo = new ArrayList<>();
                for (Component name : names) {
                    if (name.getContents() instanceof TranslatableContents translatableContents) {
                        if (oldEntries.add(translatableContents.getKey())) {
                            newCompo.add(name);
                        }
                    } else {
                        if (oldEntries.add(name.getString())) {
                            newCompo.add(name);
                        }
                    }
                }
                result.computeIfAbsent(type, k -> new ArrayList<>())
                        .addAll(newCompo);
            }
        }

        newStrings.forEach((type, strings) -> {
            Set<String> oldEntries = globalDataManager.getOldEntries(type);
            oldEntries.addAll(strings);
        });

        return result;
    }


    private static Component buildNewStageTip(
            Component title,
            Component description,
            Map<String, List<Component>> entries
    ) {
        MutableComponent message = Component.empty();

        message.append(Component.translatable(
                "tip.oneblock.new_stage.header",
                title,
                description
        ));

        appendGroup(
                message,
                "tip.oneblock.new_stage.blocks",
                entries.get(ModConstants.TYPE_BLOCK)
        );

        appendGroup(
                message,
                "tip.oneblock.new_stage.items",
                entries.get(ModConstants.TYPE_GIFT)
        );

        appendGroup(
                message,
                "tip.oneblock.new_stage.mobs",
                entries.get(ModConstants.TYPE_MOB)
        );

        message.append(Component.translatable("tip.oneblock.new_stage.footer"));

        return message;
    }

    private static void appendGroup(MutableComponent message, String key, List<Component> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        message.append(Component.translatable(key, joinLocalizedList(list)).getString());
    }

    private static Component joinLocalizedList(List<Component> list) {
        MutableComponent result = Component.empty();
        Component separator = Component.translatable("tip.oneblock.new_stage.separator");
        boolean isTooBig = list.size() > 24;
        if (isTooBig) {
            list = list.subList(0, 24);
        }
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                result.append(separator);
            }
            result.append(list.get(i));
        }
        if (isTooBig)
            result.append(" ...");
        return result;
    }

    private static List<Component> getLocalizedEntryNames(MinecraftServer server, String type, String id) {
        Identifier location;

        try {
            location = Identifier.tryParse(id);
        } catch (Exception e) {
            return List.of(Component.literal(id));
        }

        if (Objects.equals(type, ModConstants.TYPE_BLOCK)) {
            return BuiltInRegistries.BLOCK.getOptional(location)
                    .map(block -> List.<Component>of(block.getName()))
                    .orElse(List.of(Component.literal(id)));
        }

        if (Objects.equals(type, ModConstants.TYPE_MOB)) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(location)
                    .map(entityType -> List.<Component>of(entityType.getDescription()))
                    .orElse(List.of(Component.literal(id)));
        }

        if (Objects.equals(type, ModConstants.TYPE_GIFT)) {
            return sampleLootTableItems(server, location);
        }

        return List.of(Component.literal(id));
    }

    private static List<Component> sampleLootTableItems(MinecraftServer server, Identifier location) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, location);
        LootTable lootTable = server.reloadableRegistries().getLootTable(key);

        Set<Component> seenItemIds = new LinkedHashSet<>();
        List<Component> result = new ArrayList<>();

        LootParams params = new LootParams.Builder(server.overworld())
                .create(LootContextParamSets.EMPTY);

        xx:
        for (int i = 0; i < 32; i++) {
            for (ItemStack stack : lootTable.getRandomItems(params)) {
                if (stack.isEmpty()) {
                    continue;
                }

                // String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                Item item = stack.getItem();
                if ((item instanceof SpawnEggItem))
                    continue;
                if (seenItemIds.add(stack.getHoverName())) {
                    result.add(stack.getHoverName());
                }
            }
        }

        return result;
    }
}
