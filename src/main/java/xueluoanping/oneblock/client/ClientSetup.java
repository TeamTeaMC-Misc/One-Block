package xueluoanping.oneblock.client;



import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import xueluoanping.oneblock.ModContents;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientSetup {


    @SubscribeEvent
    public static void onBuildCreativeModeTabContentsEvent(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModContents.fantasy_bracelet.get());
            ItemStack stack = ModContents.fantasy_bracelet.get().getDefaultInstance();
            stack.setDamageValue(1);
            event.accept(stack);
        }
    }
}
