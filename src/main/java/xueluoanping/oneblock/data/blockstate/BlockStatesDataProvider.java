// package xueluoanping.oneblock.data.blockstate;
//
//
// import net.minecraft.data.PackOutput;
// import net.minecraft.resources.Identifier;
// import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
// import net.neoforged.neoforge.client.model.generators.ModelFile;
// import net.neoforged.neoforge.common.data.ExistingFileHelper;
// import xueluoanping.oneblock.ModContents;
// import xueluoanping.oneblock.OneBlock;
//
// public class BlockStatesDataProvider extends BlockStateProvider {
//
//
//     private final ExistingFileHelper existingFileHelper;
//
//     public BlockStatesDataProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
//         super(output, OneBlock.MOD_ID, existingFileHelper);
//         this.existingFileHelper = existingFileHelper;
//     }
//
//     @Override
//     protected void registerStatesAndModels() {
//         simpleBlock(ModContents.one_stone.value(),
//                 new ModelFile.ExistingModelFile(Identifier.withDefaultNamespace("block/air"),existingFileHelper));
//     }
//
//
// }
