package xueluoanping.oneblock;


import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xueluoanping.oneblock.config.General;
import xueluoanping.oneblock.data.start;
import xueluoanping.oneblock.handler.Levelhandler;
import xueluoanping.oneblock.handler.ReloadHandler;

import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OneBlock.MOD_ID)
public class OneBlock {
    public static final String MOD_ID = "oneblock";
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static final boolean useLogger = Objects.equals(System.getProperty("forgegradle.runs.dev"), "true");

    public OneBlock(IEventBus modEventBus, ModContainer modContainer) {

        // modEventBus.register(ControllerFluidCapabilityHandler.instance);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(ModContents::onAddPackFindersEvent);

        // Register the Deferred Register to the mod event bus so blocks get registered
        ModContents.BLOCK_DEFERRED_REGISTER.register(modEventBus);
        ModContents.ITEM_DEFERRED_REGISTER.register(modEventBus);
        ModContents.LOOT_MODIFIERS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.

        NeoForge.EVENT_BUS.register(Levelhandler.instance);
        NeoForge.EVENT_BUS.register(ReloadHandler.instance);

        // Register the item to a creative tab
        // modContainer.addListener(this::gatherData);
        // modContainer.addListener(this::FMLCommonSetup);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, General.COMMON_CONFIG);
        if (FMLLoader.getCurrentOrNull().getDist() == Dist.CLIENT)
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }


    public void gatherData(final GatherDataEvent.Client event) {
        start.dataGen(event);
    }

    public static void logger(Object... x) {
        if (useLogger|| !FMLEnvironment.isProduction()) {
            LOGGER.info(getStr(x));
        }
    }

    public static String getStr(Object... x) {
        StringBuilder output = new StringBuilder();
        for (Object i : x) {
            output.append(i).append(" ");
        }
        return output.toString();
    }

    public static void error(Object... x) {
        LOGGER.error(getStr(x));
    }

    public static Identifier rl(String name) {
        return Identifier.fromNamespaceAndPath(OneBlock.MOD_ID, name);
    }
}
