package xueluoanping.oneblock.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.loot.modifier.AddLootTableModifier;
import xueluoanping.oneblock.util.RegisterFinderUtil;

import java.util.concurrent.CompletableFuture;

public class GLMProvider extends GlobalLootModifierProvider {
    public GLMProvider(PackOutput gen, CompletableFuture<HolderLookup.Provider> registries, String modid) {
        super(gen, registries, modid);
    }

    //  can not work together
    @Override
    protected void start() {
        String variety = "-variety";

        var cond = new LootItemCondition[7];
        for (int i = 4; i < 11; i++) {
            cond[i - 4] = LootTableIdCondition.builder(Identifier.fromNamespaceAndPath("ija-one-block", num(i) + variety)).build();
        }


        cond = new LootItemCondition[2];
        for (int i = 9; i < 11; i++) {
            cond[i - 9] = LootTableIdCondition.builder(Identifier.fromNamespaceAndPath("ija-one-block", num(i) + variety)).build();
        }
        this.add("add_loot_from_10", new AddLootTableModifier(cond,
                1000,RegisterFinderUtil.getLootTable(OneBlock.MOD_ID, "10")));


        this.add("add_loot_from_12", new AddLootTableModifier(new LootItemCondition[]{
                LootTableIdCondition.builder(Identifier.fromNamespaceAndPath("ija-one-block", num(10) + variety)).build()
        }, 1000,RegisterFinderUtil.getLootTable(OneBlock.MOD_ID, "12")));


        cond = new LootItemCondition[7];
        for (int i = 4; i < 11; i++) {
            cond[i - 4] = LootTableIdCondition.builder(Identifier.fromNamespaceAndPath("ija-one-block", num(i) + variety)).build();
        }
        this.add("add_loot_from_copper", new AddLootTableModifier(cond,
                1000,RegisterFinderUtil.getLootTable(OneBlock.MOD_ID, "copper")));
    }


    public String num(int num) {
        return num > 9 ? num + "" : "0" + num;
    }


}
