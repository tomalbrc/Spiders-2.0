package tcb.spiderstpo.common.entity.mob;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IEntityReadWriteHook {
    void onRead(ValueInput nbt);

    void onWrite(ValueOutput nbt);
}
