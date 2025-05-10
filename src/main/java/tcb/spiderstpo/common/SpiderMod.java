package tcb.spiderstpo.common;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
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

        PolymerResourcePackUtils.addModAssets(MODID);
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(resourcePackBuilder -> {
            // i dont want to include a copy of the vanilla spider texture in the mods assets soooo:
            var spiderTexture = resourcePackBuilder.getDataOrSource("assets/minecraft/textures/entity/spider/spider.png");
            if (spiderTexture != null) resourcePackBuilder.addData("assets/bil/textures/item/spider/spider.png", spiderTexture);
        });
    }
}
