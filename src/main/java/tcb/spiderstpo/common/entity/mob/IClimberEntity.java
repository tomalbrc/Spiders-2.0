package tcb.spiderstpo.common.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import tcb.spiderstpo.common.entity.movement.IAdvancedPathFindingEntity;

import java.util.List;

public interface IClimberEntity extends IAdvancedPathFindingEntity {
    float getAttachmentOffset(Direction.Axis axis, float partialTicks);

    float getVerticalOffset(float partialTicks);

    Orientation getOrientation();

    Orientation calculateOrientation(float partialTicks);

    @Nullable
    Orientation getRenderOrientation();

    void setRenderOrientation(Orientation orientation);

    float getMovementSpeed();

    Pair<Direction, Vec3> getGroundDirection();

    boolean shouldTrackPathingTargets();

    @Nullable
    Vec3 getTrackedMovementTarget();

    @Nullable
    List<PathingTarget> getTrackedPathingTargets();

    boolean canClimbOnBlock(BlockState state, BlockPos pos);

    float getBlockSlipperiness(BlockPos pos);

    boolean canClimberTriggerWalking();

    boolean canClimbInWater();

    void setCanClimbInWater(boolean value);

    boolean canClimbInLava();

    void setCanClimbInLava(boolean value);

    float getCollisionsInclusionRange();

    void setCollisionsInclusionRange(float range);

    float getCollisionsSmoothingRange();

    void setCollisionsSmoothingRange(float range);

    void setJumpDirection(@Nullable Vec3 dir);
}
