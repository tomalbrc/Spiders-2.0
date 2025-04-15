package tcb.spiderstpo.common.entity.mob;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.Vec3;

public interface ILivingEntityLookAtHook {
    Vec3 onLookAt(EntityAnchorArgument.Anchor anchor, Vec3 vec);
}
