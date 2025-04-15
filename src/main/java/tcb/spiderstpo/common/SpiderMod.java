package tcb.spiderstpo.common;

import net.fabricmc.api.ModInitializer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class SpiderMod implements ModInitializer {

    @Override
    public void onInitialize() {
        //Bind tag
        final TagKey<Block> nonClimbable = ModTags.NON_CLIMBABLE;
    }
}
