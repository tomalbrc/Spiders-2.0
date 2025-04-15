package tcb.spiderstpo.common.entity.mob;

import net.minecraft.nbt.CompoundTag;

public interface IEntityReadWriteHook {
    void onRead(CompoundTag nbt);

    void onWrite(CompoundTag nbt);
}
