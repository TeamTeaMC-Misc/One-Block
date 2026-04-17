package xueluoanping.oneblock.data.blockstate;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import xueluoanping.oneblock.ModContents;
import xueluoanping.oneblock.OneBlock;

import java.util.Optional;
import java.util.stream.Stream;

public class OurModelProvider extends ModelProvider {
    public OurModelProvider(PackOutput output) {
        this(output, OneBlock.MOD_ID);
    }

    public OurModelProvider(PackOutput output, String modId) {
        super(output, modId);
    }

    @Override
    protected @NonNull Stream<? extends Holder<Item>> getKnownItems() {
        return Stream.of();
    }

    @Override
    protected @NonNull Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.of();
    }


    public static final ModelTemplate AIR = ModelTemplates.create("air", TextureSlot.ALL);

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        MultiVariant model = BlockModelGenerators.plainVariant(
                Identifier.withDefaultNamespace("block/air"));
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModContents.one_stone.value(), model));


        ItemModel.Unbaked unbaked = ItemModelUtils.plainModel(itemModels.createFlatItemModel(ModContents.fantasy_bracelet.get(), "", ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked unbaked1 = ItemModelUtils.plainModel(itemModels.createFlatItemModel(ModContents.fantasy_bracelet.get(), "_1", ModelTemplates.FLAT_ITEM));
        itemModels.itemModelOutput.accept(ModContents.fantasy_bracelet.get(),
                new ConditionalItemModel.Unbaked(Optional.empty(), new Damaged(), unbaked1, unbaked));
    }


}
