package tcb.spiderstpo.common.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IEntityMovementHook {
    boolean onMove(MoverType type, Vec3 pos, boolean pre);

    @Nullable
    BlockPos getAdjustedOnPosition(BlockPos onPosition);

    Entity.MovementEmission getAdjustedCanTriggerWalking(Entity.MovementEmission canTriggerWalking);
}
