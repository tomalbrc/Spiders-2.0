package tcb.spiderstpo.common;

import net.fabricmc.api.ModInitializer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import tcb.spiderstpo.polymer.MobRegistry;
import tcb.spiderstpo.polymer.SpiderHolder;

public class SpiderMod implements ModInitializer {

    public static final String MODID = "spiderstpo";

    @Override
    public void onInitialize() {
        MobRegistry.register();
        SpiderHolder.load();
        final TagKey<Block> nonClimbable = ModTags.NON_CLIMBABLE;
    }
}
