package tcb.spiderstpo.common.entity.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface IAdvancedPathFindingEntity {
    /**
     * The side on which the entity is currently walking
     *
     * @return
     */
    default Direction getGroundSide() {
        return Direction.DOWN;
    }

    /**
     * Called when the mob tries to move along the path but is obstructed
     */
    void onPathingObstructed(Direction facing);

    /**
     * Returns how many ticks the mob can be stuck before the path is considered to be obstructed
     *
     * @return
     */
    default int getMaxStuckCheckTicks() {
        return 40;
    }

    /**
     * Returns the pathing malus for building a bridge
     *
     * @param entity
     * @param pos
     * @param fallPathPoint
     * @return
     */
    default float getBridgePathingMalus(Mob entity, BlockPos pos, @Nullable Node fallPathPoint) {
        return -1.0f;
    }

    /**
     * Returns the pathing malus for the given {@link PathType} and block position.
     * Nodes with negative values are avoided at all cost. Nodes with value 0.0 have the highest priority, i.e.
     * are preferred over all other nodes. Nodes with a positive value incur an additional travel cost of the same magnitude
     * and the higher their value the less they are preferred. Note that the additional travel cost increases the path's "length" (i.e. cost)
     * and thus decreases the actual maximum path length in blocks.
     *
     * @param cache
     * @param type
     * @param pos
     * @param direction
     * @param sides
     * @return
     */
    default float getPathingMalus(BlockGetter cache, Mob entity, PathType type, BlockPos pos, Vec3i direction, Predicate<Direction> sides) {
        return entity.getPathfindingMalus(type);
    }

    /**
     * Called after the path finder has finished finding a path.
     * Can e.g. be used to clear caches.
     */
    default void pathFinderCleanup() {

    }
}
