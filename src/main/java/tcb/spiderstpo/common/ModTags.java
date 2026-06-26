package tcb.spiderstpo.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static final TagKey<Block> NON_CLIMBABLE = TagKey.create(Registries.BLOCK, Identifier.tryParse("spiderstpo:non_climbable"));
}
