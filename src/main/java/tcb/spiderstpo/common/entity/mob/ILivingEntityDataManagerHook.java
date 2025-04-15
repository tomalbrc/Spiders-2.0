package tcb.spiderstpo.common.entity.mob;

import net.minecraft.network.syncher.EntityDataAccessor;

public interface ILivingEntityDataManagerHook {
    void onNotifyDataManagerChange(EntityDataAccessor<?> key);
}
