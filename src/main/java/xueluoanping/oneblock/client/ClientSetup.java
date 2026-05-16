package xueluoanping.oneblock.client;


import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import xueluoanping.oneblock.ModContents;
import xueluoanping.oneblock.OneBlock;

import java.util.Optional;

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

    private static boolean selectedSkyBlockPreset = false;
    private static final ResourceKey<WorldPreset> SKY_BLOCK = ResourceKey.create(
            Registries.WORLD_PRESET,
            OneBlock.rl("sky_block")
    );

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }

        // if (selectedSkyBlockPreset) {
        //     return;
        // }

        WorldCreationUiState uiState = screen.getUiState();

        Registry<WorldPreset> presetRegistry = uiState
                .getSettings()
                .worldgenLoadContext()
                .lookupOrThrow(Registries.WORLD_PRESET);


        Optional<Holder.Reference<WorldPreset>> skyBlock = presetRegistry.get(SKY_BLOCK);

        skyBlock.ifPresent(holder -> {
            uiState.setWorldType(new WorldCreationUiState.WorldTypeEntry(holder));
            selectedSkyBlockPreset = true;
        });
    }
}
