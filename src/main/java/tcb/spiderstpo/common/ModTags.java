package tcb.spiderstpo.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import tcb.spiderstpo.mixins.access.BlockTagsAccess;

public class ModTags {
	public static final Tag<Block> NON_CLIMBABLE = BlockTagsAccess.invokeBind("spiderstpo:non_climbable");
}
