// package xueluoanping.oneblock.data.blockstate;
//
// import net.minecraft.data.PackOutput;
// import net.minecraft.resources.Identifier;
// import net.minecraft.world.item.Item;
//
// import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
// import net.neoforged.neoforge.client.model.generators.ModelFile;
// import net.neoforged.neoforge.common.data.ExistingFileHelper;
// import xueluoanping.oneblock.ModContents;
// import xueluoanping.oneblock.OneBlock;
// import xueluoanping.oneblock.util.RegisterFinderUtil;
//
// public class CItemModelProvider extends ItemModelProvider {
//
//
// 	public static final String GENERATED = "item/generated";
// 	public static final String HANDHELD = "item/handheld";
//
// 	public CItemModelProvider(PackOutput generator, ExistingFileHelper existingFileHelper) {
// 		super(generator, OneBlock.MOD_ID, existingFileHelper);
// 	}
//
//
// 	@Override
// 	protected void registerModels() {
//
// 		basicItem(OneBlock.rl("fantasy_bracelet_1"));
// 		basicItem(ModContents.fantasy_bracelet.get())
// 				.override().predicate(Identifier.withDefaultNamespace("damage"),1).model(new ModelFile.UncheckedModelFile(OneBlock.rl("item/fantasy_bracelet_1")))
// 		;
// 	}
//
//
// 	private String itemName(Item item) {
// 		return RegisterFinderUtil.getItemKey(item).getPath();
// 	}
//
// 	public Identifier resourceItem(String path) {
// 		return OneBlock.rl("item/" + path);
// 	}
//
//
//
// }
