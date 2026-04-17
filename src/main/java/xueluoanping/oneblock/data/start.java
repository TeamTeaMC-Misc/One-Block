package xueluoanping.oneblock.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.data.blockstate.OurModelProvider;


public class start {
    public final static String MODID = OneBlock.MOD_ID;

    public static void dataGen(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(true, new OurModelProvider(packOutput));

    }
}
