package tcb.spiderstpo.common.entity.mob;

import net.minecraft.world.phys.Vec3;

public interface ILivingEntityTravelHook {
    boolean onTravel(Vec3 relative, boolean pre);
}
