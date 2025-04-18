package tcb.spiderstpo.polymer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tcb.spiderstpo.common.CollisionSmoothingUtil;
import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.ModTags;
import tcb.spiderstpo.common.SpiderMod;
import tcb.spiderstpo.common.entity.goal.BetterLeapAtTargetGoal;
import tcb.spiderstpo.common.entity.mob.*;
import tcb.spiderstpo.common.entity.movement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class BetterSpiderEntity extends Spider implements IClimberEntity, IAdvancedPathFindingEntity, IEntityReadWriteHook, IMobEntityTickHook, ILivingEntityLookAtHook, ILivingEntityRotationHook, ILivingEntityDataManagerHook, ILivingEntityJumpHook, ILivingEntityTravelHook, IEntityMovementHook {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spider");

    private static final AttributeModifier FOLLOW_RANGE_INCREASE = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spider_follow_range_increase"), 8.0D, AttributeModifier.Operation.ADD_VALUE);
    private boolean pathFinderDebugPreview;

    public BetterSpiderEntity(EntityType<? extends Spider> type, Level worldIn) {
        super(type, worldIn);

        Objects.requireNonNull(this.getAttribute(Attributes.STEP_HEIGHT)).setBaseValue(0.1);
        this.orientation = this.calculateOrientation(1);
        this.groundDirection = this.getGroundDirection();
        this.moveControl = new ClimberMoveController<>(this);
        this.lookControl = new ClimberLookController<>(this);
        this.jumpControl = new ClimberJumpController<>(this);

        this.lootTable = Optional.of(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("entities/spider")));

        var attr = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (attr != null)
            attr.addPermanentModifier(FOLLOW_RANGE_INCREASE);
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        this.pathFinderDebugPreview = Config.getConfig().isPathFinderDebugPreview();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Armadillo.class, 6.0F, 1.0F, 1.2, (livingEntity) -> !((Armadillo) livingEntity).isScared()));
        this.goalSelector.addGoal(3, new BetterLeapAtTargetGoal<>(this, 0.4f));
        this.goalSelector.addGoal(4, new SpiderAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setUnseenMemoryTicks(200));
        this.targetSelector.addGoal(2, new SpiderTargetGoal<>(this, Player.class).setUnseenMemoryTicks(200));
        this.targetSelector.addGoal(3, new SpiderTargetGoal<>(this, IronGolem.class).setUnseenMemoryTicks(200));
    }


    @Override
    public boolean shouldTrackPathingTargets() {
        return this.pathFinderDebugPreview;
    }

    @Override
    public boolean canClimbOnBlock(BlockState state, BlockPos pos) {
        return !state.is(ModTags.NON_CLIMBABLE);
    }

    @Override
    public float getBlockSlipperiness(BlockPos pos) {
        BlockState offsetState = this.level().getBlockState(pos);

        float slipperiness = offsetState.getBlock().getFriction() * 0.91f;

        if (offsetState.is(ModTags.NON_CLIMBABLE)) {
            slipperiness = 1 - (1 - slipperiness) * 0.25f;
        }

        return slipperiness;
    }

    @Override
    public float getPathingMalus(BlockGetter cache, Mob entity, PathType nodeType, BlockPos pos, Vec3i direction, Predicate<Direction> sides) {
        if (direction.getY() != 0) {
            boolean hasClimbableNeigbor = false;

            BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();

            for (Direction offset : Direction.values()) {
                if (sides.test(offset)) {
                    offsetPos.set(pos.getX() + offset.getStepX(), pos.getY() + offset.getStepY(), pos.getZ() + offset.getStepZ());

                    BlockState state = cache.getBlockState(offsetPos);

                    if (this.canClimbOnBlock(state, offsetPos)) {
                        hasClimbableNeigbor = true;
                    }
                }
            }

            if (!hasClimbableNeigbor) {
                return -1.0f;
            }
        }

        return entity.getPathfindingMalus(nodeType);
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(Level level) {
        BetterSpiderPathNavigator<BetterSpiderEntity> navigate = new BetterSpiderPathNavigator<>(this, level, false);
        navigate.setCanFloat(true);
        return navigate;
    }

    private static float MOVEMENT_TARGET_X;
    private static float MOVEMENT_TARGET_Y;
    private static float MOVEMENT_TARGET_Z;
    private static final List<Optional<BlockPos>> PATHING_TARGETS = new ObjectArrayList<>(8);
    private static final List<Direction> PATHING_SIDES = new ObjectArrayList<>(8);

    private double prevAttachmentOffsetX, prevAttachmentOffsetY, prevAttachmentOffsetZ;
    private double attachmentOffsetX, attachmentOffsetY, attachmentOffsetZ;

    private Vec3 attachmentNormal = new Vec3(0, 1, 0);
    private Vec3 prevAttachmentNormal = new Vec3(0, 1, 0);

    private double lastAttachmentOffsetX, lastAttachmentOffsetY, lastAttachmentOffsetZ;

    private Vec3 lastAttachmentOrientationNormal = new Vec3(0, 1, 0);

    private int attachedTicks = 5;

    private Vec3 attachedSides = new Vec3(0, 0, 0);

    private boolean canClimbInWater = false;
    private boolean canClimbInLava = false;

    private boolean isTravelingInFluid = false;

    private float collisionsInclusionRange = 2.0f;
    private float collisionsSmoothingRange = 1.25f;

    private Orientation orientation;
    private Pair<Direction, Vec3> groundDirection = Pair.of(Direction.DOWN, new Vec3(0, -1, 0));

    private Orientation renderOrientation;

    private float nextStepDistance;
    private Vec3 preWalkingPosition;

    private double preMoveY;

    private Vec3 jumpDir;


    @Override
    public void onWrite(CompoundTag nbt) {
        nbt.putDouble("nyfsspiders.AttachmentNormalX", this.attachmentNormal.x);
        nbt.putDouble("nyfsspiders.AttachmentNormalY", this.attachmentNormal.y);
        nbt.putDouble("nyfsspiders.AttachmentNormalZ", this.attachmentNormal.z);

        nbt.putInt("nyfsspiders.AttachedTicks", this.attachedTicks);
    }

    @Override
    public void onRead(CompoundTag nbt) {
        this.prevAttachmentNormal = this.attachmentNormal = new Vec3(
                nbt.getDouble("nyfsspiders.AttachmentNormalX").orElseThrow(),
                nbt.getDouble("nyfsspiders.AttachmentNormalY").orElseThrow(),
                nbt.getDouble("nyfsspiders.AttachmentNormalZ").orElseThrow()
        );

        this.attachedTicks = nbt.getInt("nyfsspiders.AttachedTicks").orElseThrow();

        this.orientation = this.calculateOrientation(1);
    }

    @Override
    public boolean canClimbInWater() {
        return this.canClimbInWater;
    }

    @Override
    public void setCanClimbInWater(boolean value) {
        this.canClimbInWater = value;
    }

    @Override
    public boolean canClimbInLava() {
        return this.canClimbInLava;
    }

    @Override
    public void setCanClimbInLava(boolean value) {
        this.canClimbInLava = value;
    }

    @Override
    public float getCollisionsInclusionRange() {
        return this.collisionsInclusionRange;
    }

    @Override
    public void setCollisionsInclusionRange(float range) {
        this.collisionsInclusionRange = range;
    }

    @Override
    public float getCollisionsSmoothingRange() {
        return this.collisionsSmoothingRange;
    }

    @Override
    public void setCollisionsSmoothingRange(float range) {
        this.collisionsSmoothingRange = range;
    }

    @Override
    public float getBridgePathingMalus(Mob entity, BlockPos pos, Node fallPathPoint) {
        return -1.0f;
    }

    @Override
    public void onPathingObstructed(Direction facing) {

    }

    @Override
    public int getMaxFallDistance() {
        return 0;
    }

    @Override
    public float getMovementSpeed() {
        AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED); //MOVEMENT_SPEED
        return attribute != null ? (float) attribute.getValue() : 1.0f;
    }

    private static double calculateXOffset(AABB aabb, AABB other, double offsetX) {
        if (other.maxY > aabb.minY && other.minY < aabb.maxY && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
            if (offsetX > 0.0D && other.maxX <= aabb.minX) {
                double dx = aabb.minX - other.maxX;

                if (dx < offsetX) {
                    offsetX = dx;
                }
            } else if (offsetX < 0.0D && other.minX >= aabb.maxX) {
                double dx = aabb.maxX - other.minX;

                if (dx > offsetX) {
                    offsetX = dx;
                }
            }

            return offsetX;
        } else {
            return offsetX;
        }
    }

    private static double calculateYOffset(AABB aabb, AABB other, double offsetY) {
        if (other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
            if (offsetY > 0.0D && other.maxY <= aabb.minY) {
                double dy = aabb.minY - other.maxY;

                if (dy < offsetY) {
                    offsetY = dy;
                }
            } else if (offsetY < 0.0D && other.minY >= aabb.maxY) {
                double dy = aabb.maxY - other.minY;

                if (dy > offsetY) {
                    offsetY = dy;
                }
            }

            return offsetY;
        } else {
            return offsetY;
        }
    }

    private static double calculateZOffset(AABB aabb, AABB other, double offsetZ) {
        if (other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxY > aabb.minY && other.minY < aabb.maxY) {
            if (offsetZ > 0.0D && other.maxZ <= aabb.minZ) {
                double dz = aabb.minZ - other.maxZ;

                if (dz < offsetZ) {
                    offsetZ = dz;
                }
            } else if (offsetZ < 0.0D && other.minZ >= aabb.maxZ) {
                double dz = aabb.maxZ - other.minZ;

                if (dz > offsetZ) {
                    offsetZ = dz;
                }
            }
        }
        return offsetZ;
    }

    private void updateWalkingSide() {
        //Direction avoidPathingFacing = null;

        AABB entityBox = this.getBoundingBox();

        double closestFacingDst = Double.MAX_VALUE;
        Direction closestFacing = null;

        Vec3 weighting = new Vec3(0, 0, 0);

        float stickingDistance = this.zza != 0 ? 1.5f : 0.1f;

        for (Direction facing : Direction.values()) {
//            if (avoidPathingFacing == facing) {
//                continue;
//            }

            List<AABB> collisionBoxes = this.getCollisionBoxes(entityBox.inflate(0.2f).expandTowards(facing.getStepX() * stickingDistance, facing.getStepY() * stickingDistance, facing.getStepZ() * stickingDistance));

            double closestDst = Double.MAX_VALUE;

            for (AABB collisionBox : collisionBoxes) {
                closestDst = switch (facing) {
                    case EAST, WEST ->
                            Math.min(closestDst, Math.abs(calculateXOffset(entityBox, collisionBox, -facing.getStepX() * stickingDistance)));
                    case UP, DOWN ->
                            Math.min(closestDst, Math.abs(calculateYOffset(entityBox, collisionBox, -facing.getStepY() * stickingDistance)));
                    case NORTH, SOUTH ->
                            Math.min(closestDst, Math.abs(calculateZOffset(entityBox, collisionBox, -facing.getStepZ() * stickingDistance)));
                };
            }

            if (closestDst < closestFacingDst) {
                closestFacingDst = closestDst;
                closestFacing = facing;
            }

            if (closestDst < Double.MAX_VALUE) {
                weighting = weighting.add(new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(1 - Math.min(closestDst, stickingDistance) / stickingDistance));
            }
        }

        if (closestFacing == null) {
            this.groundDirection = Pair.of(Direction.DOWN, new Vec3(0, -1, 0));
        } else {
            this.groundDirection = Pair.of(closestFacing, weighting.normalize().add(0, -0.001f, 0).normalize());
        }
    }

    @Override
    public Pair<Direction, Vec3> getGroundDirection() {
        return this.groundDirection;
    }

    @Override
    public Direction getGroundSide() {
        return this.groundDirection.getKey();
    }

    @Override
    public Orientation getOrientation() {
        return this.orientation;
    }

    @Override
    public void setRenderOrientation(Orientation orientation) {
        this.renderOrientation = orientation;
    }

    @Override
    public Orientation getRenderOrientation() {
        return this.renderOrientation;
    }

    @Override
    public float getAttachmentOffset(Direction.Axis axis, float partialTicks) {
        return switch (axis) {
            case Y ->
                    (float) (this.prevAttachmentOffsetY + (this.attachmentOffsetY - this.prevAttachmentOffsetY) * partialTicks);
            case Z ->
                    (float) (this.prevAttachmentOffsetZ + (this.attachmentOffsetZ - this.prevAttachmentOffsetZ) * partialTicks);
            default ->
                    (float) (this.prevAttachmentOffsetX + (this.attachmentOffsetX - this.prevAttachmentOffsetX) * partialTicks);
        };
    }

    @Override
    public Vec3 onLookAt(EntityAnchorArgument.Anchor anchor, Vec3 vec) {
        Vec3 dir = vec.subtract(this.position());
        dir = this.getOrientation().getLocal(dir);
        return dir;
    }

    @Override
    public void onTick() {


        if (!this.level().isClientSide && this.level() instanceof ServerLevel) {
            ChunkMap.TrackedEntity entityTracker = ((ServerLevel) this.level()).getChunkSource().chunkMap.entityMap.get(this.getId());

            //Prevent premature syncing of position causing overly smoothed movement
            if (entityTracker != null && entityTracker.serverEntity.tickCount % entityTracker.serverEntity.updateInterval == 0) {
                Orientation orientation = this.getOrientation();
                if (this.shouldTrackPathingTargets()) {
                    if (this.xxa != 0) {
                        Vec3 forwardVector = orientation.getGlobal(this.getYRot(), 0);
                        Vec3 strafeVector = orientation.getGlobal(this.getYRot() + 90.0f, 0);

                        Vec3 offset = forwardVector.scale(this.zza).add(strafeVector.scale(this.xxa)).normalize();

                        MOVEMENT_TARGET_X = (float) (this.getX() + offset.x);
                        MOVEMENT_TARGET_Y = (float) (this.getY() + this.getBbHeight() * 0.5f + offset.y);
                        MOVEMENT_TARGET_Z = (float) (this.getZ() + offset.z);
                    } else {
                        MOVEMENT_TARGET_X = (float) this.getMoveControl().getWantedX();
                        MOVEMENT_TARGET_Y = (float) this.getMoveControl().getWantedY();
                        MOVEMENT_TARGET_Z = (float) this.getMoveControl().getWantedZ();
                    }

                    Path path = this.getNavigation().getPath();
                    if (path != null) {
                        for (int i = 0; i < PATHING_TARGETS.size(); i++) {
                            Optional<BlockPos> pathingTarget;
                            Direction pathingSide = PATHING_SIDES.get(i);

                            if (path.getNextNodeIndex() + i < path.getNodeCount()) {
                                Node point = path.getNode(path.getNextNodeIndex() + i);

                                pathingTarget = Optional.of(new BlockPos(point.x, point.y, point.z));

                                if (point instanceof DirectionalPathPoint) {
                                    Direction dir = ((DirectionalPathPoint) point).getPathSide();

                                    pathingSide = Objects.requireNonNullElse(dir, Direction.DOWN);
                                }

                            } else {
                                pathingTarget = Optional.empty();
                                pathingSide = Direction.DOWN;
                            }

                            PATHING_TARGETS.set(i, pathingTarget);
                            PATHING_SIDES.set(i, pathingSide);
                        }
                    } else {
                        for (int i = 0; i < PATHING_TARGETS.size(); i++) {
                            PATHING_TARGETS.set(i, Optional.empty());
                        }
                        for (int i = 0; i < PATHING_SIDES.size(); i++) {
                            PATHING_SIDES.set(i, Direction.DOWN);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateWalkingSide();
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    @Nullable
    public Vec3 getTrackedMovementTarget() {
        if (this.shouldTrackPathingTargets()) {
            return new Vec3(MOVEMENT_TARGET_X, MOVEMENT_TARGET_Y, MOVEMENT_TARGET_Z);
        }

        return null;
    }

    @Override
    @Nullable
    public List<PathingTarget> getTrackedPathingTargets() {
        if (this.shouldTrackPathingTargets()) {
            List<PathingTarget> pathingTargets = new ArrayList<>(PATHING_TARGETS.size());

            int i = 0;
            for (Optional<BlockPos> key : PATHING_TARGETS) {
                BlockPos pos = key.orElse(null);

                if (pos != null) {
                    pathingTargets.add(new PathingTarget(pos, PATHING_SIDES.get(i)));
                }

                i++;
            }

            return pathingTargets;
        }

        return null;
    }

    @Override
    public float getVerticalOffset(float partialTicks) {
        return 0.075f;
    }

    private void forEachCollisonBox(AABB aabb, Shapes.DoubleLineConsumer action) {
        int minChunkX = ((Mth.floor(aabb.minX - 1.0E-7D) - 1) >> 4);
        int maxChunkX = ((Mth.floor(aabb.maxX + 1.0E-7D) + 1) >> 4);
        int minChunkZ = ((Mth.floor(aabb.minZ - 1.0E-7D) - 1) >> 4);
        int maxChunkZ = ((Mth.floor(aabb.maxZ + 1.0E-7D) + 1) >> 4);

        int width = maxChunkX - minChunkX + 1;
        int depth = maxChunkZ - minChunkZ + 1;

        BlockGetter[] blockReaderCache = new BlockGetter[width * depth];

        CollisionGetter collisionReader = this.level();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                blockReaderCache[(cx - minChunkX) + (cz - minChunkZ) * width] = collisionReader.getChunkForCollisions(cx, cz);
            }
        }

        CollisionGetter cachedCollisionReader = new CollisionGetter() {
            @Override
            public int getHeight() {
                return level().getHeight();
            }

            @Override
            public int getMinY() {
                return level().getMinY();
            }

            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {
                return collisionReader.getBlockEntity(pos);
            }

            @Override
            @NotNull
            public BlockState getBlockState(BlockPos pos) {
                return collisionReader.getBlockState(pos);
            }

            @Override
            @NotNull
            public FluidState getFluidState(BlockPos pos) {
                return collisionReader.getFluidState(pos);
            }

            @Override
            @NotNull
            public WorldBorder getWorldBorder() {
                return collisionReader.getWorldBorder();
            }

            @Override
            @NotNull
            public List<VoxelShape> getEntityCollisions(Entity entity, AABB aabb) {
                return collisionReader.getEntityCollisions(entity, aabb);
            }

            @Override
            public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
                return blockReaderCache[(chunkX - minChunkX) + (chunkZ - minChunkZ) * width];
            }
        };

        //todo:figure out;
        Iterable<VoxelShape> shapes = cachedCollisionReader.getBlockCollisions(this, aabb);
//		StreamSupport.stream(new CollisionSpliterator(cachedCollisionReader, this, aabb, this::canClimbOnBlock), false);

        shapes.forEach(shape -> shape.forAllBoxes(action));
    }

    private List<AABB> getCollisionBoxes(AABB aabb) {
        List<AABB> boxes = new ArrayList<>();
        this.forEachCollisonBox(aabb, (minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ)));
        return boxes;
    }

    private void updateOffsetsAndOrientation() {
        Vec3 direction = this.getOrientation().getGlobal(this.getYRot(), this.getXRot());

        boolean isAttached = false;

        double baseStickingOffsetX = 0.0f;
        double baseStickingOffsetY = this.getVerticalOffset(1);
        double baseStickingOffsetZ = 0.0f;
        Vec3 baseOrientationNormal = new Vec3(0, 1, 0);

        if (!this.isTravelingInFluid && this.onGround() && this.getVehicle() == null) {
            Vec3 p = this.position();

            Vec3 s = p.add(0, this.getBbHeight() * 0.5f, 0);
            AABB inclusionBox = new AABB(s.x, s.y, s.z, s.x, s.y, s.z).inflate(this.collisionsInclusionRange);

            Pair<Vec3, Vec3> attachmentPoint = CollisionSmoothingUtil.findClosestPoint(consumer -> this.forEachCollisonBox(inclusionBox, consumer), s, this.attachmentNormal.scale(-1), this.collisionsSmoothingRange, 1.0f, 0.001f, 20, 0.05f, s);

            AABB entityBox = this.getBoundingBox();

            if (attachmentPoint != null) {
                Vec3 attachmentPos = attachmentPoint.getLeft();

                double dx = Math.max(entityBox.minX - attachmentPos.x, attachmentPos.x - entityBox.maxX);
                double dy = Math.max(entityBox.minY - attachmentPos.y, attachmentPos.y - entityBox.maxY);
                double dz = Math.max(entityBox.minZ - attachmentPos.z, attachmentPos.z - entityBox.maxZ);

                if (Math.max(dx, Math.max(dy, dz)) < 0.5f) {
                    isAttached = true;

                    this.lastAttachmentOffsetX = Mth.clamp(attachmentPos.x - p.x, -this.getBbWidth() / 2, this.getBbWidth() / 2);
                    this.lastAttachmentOffsetY = Mth.clamp(attachmentPos.y - p.y, 0, this.getBbHeight());
                    this.lastAttachmentOffsetZ = Mth.clamp(attachmentPos.z - p.z, -this.getBbWidth() / 2, this.getBbWidth() / 2);
                    this.lastAttachmentOrientationNormal = attachmentPoint.getRight();
                }
            }
        }

        this.prevAttachmentOffsetX = this.attachmentOffsetX;
        this.prevAttachmentOffsetY = this.attachmentOffsetY;
        this.prevAttachmentOffsetZ = this.attachmentOffsetZ;
        this.prevAttachmentNormal = this.attachmentNormal;

        float attachmentBlend = this.attachedTicks * 0.2f;

        this.attachmentOffsetX = baseStickingOffsetX + (this.lastAttachmentOffsetX - baseStickingOffsetX) * attachmentBlend;
        this.attachmentOffsetY = baseStickingOffsetY + (this.lastAttachmentOffsetY - baseStickingOffsetY) * attachmentBlend;
        this.attachmentOffsetZ = baseStickingOffsetZ + (this.lastAttachmentOffsetZ - baseStickingOffsetZ) * attachmentBlend;
        this.attachmentNormal = baseOrientationNormal.add(this.lastAttachmentOrientationNormal.subtract(baseOrientationNormal).scale(attachmentBlend)).normalize();

        if (!isAttached) {
            this.attachedTicks = Math.max(0, this.attachedTicks - 1);
        } else {
            this.attachedTicks = Math.min(5, this.attachedTicks + 1);
        }

        this.orientation = this.calculateOrientation(1);

        Pair<Float, Float> newRotations = this.getOrientation().getLocalRotation(direction);

        float yawDelta = newRotations.getLeft() - this.getYRot();
        float pitchDelta = newRotations.getRight() - this.getXRot();

        this.setYRot(Mth.wrapDegrees(this.getYRot() + yawDelta));
        this.yRotO = this.wrapAngleInRange(this.yRotO/* + yawDelta*/, this.getYRot());

        this.yBodyRot = Mth.wrapDegrees(this.yBodyRot + yawDelta);
        this.yBodyRotO = this.wrapAngleInRange(this.yBodyRotO/* + yawDelta*/, this.yBodyRot);

        this.yHeadRot = Mth.wrapDegrees(this.yHeadRot + yawDelta);
        this.yHeadRotO = this.wrapAngleInRange(this.yHeadRotO/* + yawDelta*/, this.yHeadRot);
        this.lerpYHeadRot = Mth.wrapDegrees(this.lerpYHeadRot + yawDelta);

        this.setXRot(Mth.wrapDegrees(this.getXRot() + pitchDelta));
        this.xRotO = this.wrapAngleInRange(this.xRotO/* + pitchDelta*/, this.getXRot());
    }

    private float wrapAngleInRange(float angle, float target) {
        while (target - angle < -180.0F) {
            angle -= 360.0F;
        }

        while (target - angle >= 180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    @Override
    public Orientation calculateOrientation(float partialTicks) {
        Vec3 attachmentNormal = this.prevAttachmentNormal.add(this.attachmentNormal.subtract(this.prevAttachmentNormal).scale(partialTicks));

        Vec3 localZ = new Vec3(0, 0, 1);
        Vec3 localY = new Vec3(0, 1, 0);
        Vec3 localX = new Vec3(1, 0, 0);

        float componentZ = (float) localZ.dot(attachmentNormal);
        float componentY;
        float componentX = (float) localX.dot(attachmentNormal);

        float yaw = (float) Math.toDegrees(Mth.atan2(componentX, componentZ));

        localZ = new Vec3(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        localY = new Vec3(0, 1, 0);
        localX = new Vec3(Math.sin(Math.toRadians(yaw - 90)), 0, Math.cos(Math.toRadians(yaw - 90)));

        componentZ = (float) localZ.dot(attachmentNormal);
        componentY = (float) localY.dot(attachmentNormal);
        componentX = (float) localX.dot(attachmentNormal);

        float pitch = (float) Math.toDegrees(Mth.atan2(Mth.sqrt(componentX * componentX + componentZ * componentZ), componentY));
        pitch = (pitch + 360) % 360;

        Matrix4f m = new Matrix4f();
        m.rotate(Mth.DEG_TO_RAD * yaw, 0, 1, 0);
        m.rotate(Mth.DEG_TO_RAD * pitch, 1, 0, 0);
        m.rotate(Mth.DEG_TO_RAD * (Math.signum(0.5f - componentY - componentZ - componentX) * yaw), 0, 1, 0);

        localZ = new Vec3(m.transformDirection(new Vector3f(0, 0, -1)));
        localY = new Vec3(m.transformDirection(new Vector3f(0, 1, 0)));
        localX = new Vec3(m.transformDirection(new Vector3f(1, 0, 0)));

        return new Orientation(attachmentNormal, localZ, localY, localX, componentZ, componentY, componentX, yaw, pitch);
    }


    @Override
    public float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        return yaw;
    }

    @Override
    public float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        return pitch;
    }

    @Override
    public float getTargetHeadYaw(float yaw, int rotationIncrements) {
        return (float) this.lerpYHeadRot;
    }

    @Override
    public void onNotifyDataManagerChange(EntityDataAccessor<?> key) {

    }

    @Override
    public double getDefaultGravity() {
        double gravity = 0.08d;

        boolean isFalling = this.getDeltaMovement().y <= 0.0D;

        if (isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
            gravity = 0.1D;
        }

        return gravity;
    }

    private Vec3 getStickingForce(Pair<Direction, Vec3> walkingSide) {
        double uprightness = Math.max(this.attachmentNormal.y, 0);
        double gravity = this.getGravity();
        double stickingForce = gravity * uprightness + 0.08D * (1 - uprightness);
        return walkingSide.getRight().scale(stickingForce);
    }

    @Override
    public void setJumpDirection(Vec3 dir) {
        this.jumpDir = dir != null ? dir.normalize() : null;
    }

    @Override
    public boolean onJump() {
        if (this.jumpDir != null) {
            float jumpStrength = this.getJumpPower();
            if (this.hasEffect(MobEffects.JUMP_BOOST)) {
                jumpStrength += 0.1f * (Objects.requireNonNull(this.getEffect(MobEffects.JUMP_BOOST)).getAmplifier() + 1.f);
            }

            Vec3 motion = this.getDeltaMovement();

            Vec3 orthogonalMotion = this.jumpDir.scale(this.jumpDir.dot(motion));
            Vec3 tangentialMotion = motion.subtract(orthogonalMotion);

            this.setDeltaMovement(tangentialMotion.x + this.jumpDir.x * jumpStrength, tangentialMotion.y + this.jumpDir.y * jumpStrength, tangentialMotion.z + this.jumpDir.z * jumpStrength);

            if (this.isSprinting()) {
                Vec3 boost = this.getOrientation().getGlobal(this.getYRot(), 0).scale(0.2f);
                this.setDeltaMovement(this.getDeltaMovement().add(boost));
            }

            this.hasImpulse = true;

            return true;
        }

        return false;
    }

    @Override
    public boolean onTravel(Vec3 relative, boolean pre) {
        if (pre) {
            boolean canTravel = this.isEffectiveAi() || this.hasControllingPassenger();

            this.isTravelingInFluid = false;

            FluidState fluidState = this.level().getFluidState(this.blockPosition());

            if (!this.canClimbInWater && this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                this.isTravelingInFluid = true;

                if (canTravel) {
                    return false;
                }
            } else if (!this.canClimbInLava && this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
                this.isTravelingInFluid = true;

                if (canTravel) {
                    return false;
                }
            } else if (canTravel) {
                this.travelOnGround(relative);
            }

            if (!canTravel) {
                this.calculateEntityAnimation(true);
            }

            this.updateOffsetsAndOrientation();
            return true;
        } else {
            this.updateOffsetsAndOrientation();
            return false;
        }
    }

    private float getRelevantMoveFactor(float slipperiness) {
        return this.onGround() ? this.getSpeed() * (0.16277136F / (slipperiness * slipperiness * slipperiness)) : this.getFlyingSpeed();
    }

    private void travelOnGround(Vec3 relative) {
        Orientation orientation = this.getOrientation();

        Vec3 forwardVector = orientation.getGlobal(this.getYRot(), 0);
        Vec3 strafeVector = orientation.getGlobal(this.getYRot() + 90.0f, 0);
        Vec3 upVector = orientation.getGlobal(this.getYRot(), -90.0f);

        Pair<Direction, Vec3> groundDirection = this.getGroundDirection();

        Vec3 stickingForce = this.getStickingForce(groundDirection);

        boolean isFalling = this.getDeltaMovement().y <= 0.0D;

        if (isFalling && this.hasEffect(MobEffects.SLOW_FALLING)) {
            this.fallDistance = 0;
        }

        float forward = (float) relative.z;
        float strafe = (float) relative.x;

        if (forward != 0 || strafe != 0) {
            float slipperiness = 0.91f;

            if (this.onGround()) {
                BlockPos offsetPos = new BlockPos(this.blockPosition()).relative(groundDirection.getLeft());
                slipperiness = this.getBlockSlipperiness(offsetPos);
            }

            float f = forward * forward + strafe * strafe;
            if (f >= 1.0E-4F) {
                f = Math.max(Mth.sqrt(f), 1.0f);
                f = this.getRelevantMoveFactor(slipperiness) / f;
                forward *= f;
                strafe *= f;

                Vec3 movementOffset = new Vec3(forwardVector.x * forward + strafeVector.x * strafe, forwardVector.y * forward + strafeVector.y * strafe, forwardVector.z * forward + strafeVector.z * strafe);

                double px = this.getX();
                double py = this.getY();
                double pz = this.getZ();
                Vec3 motion = this.getDeltaMovement();
                AABB aabb = this.getBoundingBox();

                //Probe actual movement vector
                this.move(MoverType.SELF, movementOffset);

                Vec3 movementDir = new Vec3(this.getX() - px, this.getY() - py, this.getZ() - pz).normalize();

                this.setBoundingBox(aabb);
                this.setLocationFromBoundingbox();
                this.setDeltaMovement(motion);

                //Probe collision normal
                Vec3 probeVector = new Vec3(Math.abs(movementDir.x) < 0.001D ? -Math.signum(upVector.x) : 0, Math.abs(movementDir.y) < 0.001D ? -Math.signum(upVector.y) : 0, Math.abs(movementDir.z) < 0.001D ? -Math.signum(upVector.z) : 0).normalize().scale(0.0001D);
                this.move(MoverType.SELF, probeVector);

                Vec3 collisionNormal = new Vec3(Math.abs(this.getX() - px - probeVector.x) > 0.000001D ? Math.signum(-probeVector.x) : 0, Math.abs(this.getY() - py - probeVector.y) > 0.000001D ? Math.signum(-probeVector.y) : 0, Math.abs(this.getZ() - pz - probeVector.z) > 0.000001D ? Math.signum(-probeVector.z) : 0).normalize();

                this.setBoundingBox(aabb);
                this.setLocationFromBoundingbox();
                this.setDeltaMovement(motion);

                //Movement vector projected to surface
                Vec3 surfaceMovementDir = movementDir.subtract(collisionNormal.scale(collisionNormal.dot(movementDir))).normalize();

                boolean isInnerCorner = Math.abs(collisionNormal.x) + Math.abs(collisionNormal.y) + Math.abs(collisionNormal.z) > 1.0001f;

                //Only project movement vector to surface if not moving across inner corner, otherwise it'd get stuck in the corner
                if (!isInnerCorner) {
                    movementDir = surfaceMovementDir;
                }

                //Nullify sticking force along movement vector projected to surface
                stickingForce = stickingForce.subtract(surfaceMovementDir.scale(surfaceMovementDir.normalize().dot(stickingForce)));

                float moveSpeed = Mth.sqrt(forward * forward + strafe * strafe);
                this.setDeltaMovement(this.getDeltaMovement().add(movementDir.scale(moveSpeed)));
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().add(stickingForce));

        double px = this.getX();
        double py = this.getY();
        double pz = this.getZ();
        Vec3 motion = this.getDeltaMovement();

        this.move(MoverType.SELF, motion);

        Vec3 prevAttachedSides = this.attachedSides;
        this.attachedSides = new Vec3(Math.abs(this.getX() - px - motion.x) > 0.001D ? -Math.signum(motion.x) : 0, Math.abs(this.getY() - py - motion.y) > 0.001D ? -Math.signum(motion.y) : 0, Math.abs(this.getZ() - pz - motion.z) > 0.001D ? -Math.signum(motion.z) : 0);

        float slipperiness = 0.91f;

        if (this.onGround()) {
            this.fallDistance = 0;

            BlockPos offsetPos = new BlockPos(blockPosition()).relative(groundDirection.getLeft());
            slipperiness = this.getBlockSlipperiness(offsetPos);
        }

        motion = this.getDeltaMovement();
        Vec3 orthogonalMotion = upVector.scale(upVector.dot(motion));
        Vec3 tangentialMotion = motion.subtract(orthogonalMotion);

        this.setDeltaMovement(tangentialMotion.x * slipperiness + orthogonalMotion.x * 0.98f, tangentialMotion.y * slipperiness + orthogonalMotion.y * 0.98f, tangentialMotion.z * slipperiness + orthogonalMotion.z * 0.98f);

        boolean detachedX = this.attachedSides.x != prevAttachedSides.x && Math.abs(this.attachedSides.x) < 0.001D;
        boolean detachedY = this.attachedSides.y != prevAttachedSides.y && Math.abs(this.attachedSides.y) < 0.001D;
        boolean detachedZ = this.attachedSides.z != prevAttachedSides.z && Math.abs(this.attachedSides.z) < 0.001D;

        if (detachedX || detachedY || detachedZ) {
            float stepHeight = this.maxUpStep();
            Objects.requireNonNull(this.getAttribute(Attributes.STEP_HEIGHT)).setBaseValue(0);
            boolean prevOnGround = this.onGround();
            boolean prevCollidedHorizontally = this.horizontalCollision;
            boolean prevCollidedVertically = this.verticalCollision;

            //Offset so that AABB is moved above the new surface
            this.move(MoverType.SELF, new Vec3(detachedX ? -prevAttachedSides.x * 0.25f : 0, detachedY ? -prevAttachedSides.y * 0.25f : 0, detachedZ ? -prevAttachedSides.z * 0.25f : 0));

            Vec3 axis = prevAttachedSides.normalize();
            Vec3 attachVector = upVector.scale(-1);
            attachVector = attachVector.subtract(axis.scale(axis.dot(attachVector)));

            if (Math.abs(attachVector.x) > Math.abs(attachVector.y) && Math.abs(attachVector.x) > Math.abs(attachVector.z)) {
                attachVector = new Vec3(Math.signum(attachVector.x), 0, 0);
            } else if (Math.abs(attachVector.y) > Math.abs(attachVector.z)) {
                attachVector = new Vec3(0, Math.signum(attachVector.y), 0);
            } else {
                attachVector = new Vec3(0, 0, Math.signum(attachVector.z));
            }

            double attachDst = motion.length() + 0.1f;

            AABB aabb = this.getBoundingBox();
            motion = this.getDeltaMovement();

            //Offset AABB towards new surface until it touches
            for (int i = 0; i < 2 && !this.onGround(); i++) {
                this.move(MoverType.SELF, attachVector.scale(attachDst));
            }

            Objects.requireNonNull(this.getAttribute(Attributes.STEP_HEIGHT)).setBaseValue(stepHeight);

            //Attaching failed, fall back to previous position
            if (!this.onGround()) {
                this.setBoundingBox(aabb);
                this.setLocationFromBoundingbox();
                this.setDeltaMovement(motion);
                this.setOnGround(prevOnGround);
                this.horizontalCollision = prevCollidedHorizontally;
                this.verticalCollision = prevCollidedVertically;
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }

        this.calculateEntityAnimation(true);
    }

    @Override
    public boolean onMove(MoverType type, Vec3 pos, boolean pre) {
        if (pre) {
            this.preWalkingPosition = this.position();
            this.preMoveY = this.getY();
        } else {
            if (Math.abs(this.getY() - this.preMoveY - pos.y) > 0.000001D) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
            }

            this.setOnGround(this.horizontalCollision || this.verticalCollision);
        }

        return false;
    }

    @Override
    public BlockPos getAdjustedOnPosition(BlockPos onPosition) {
        float verticalOffset = this.getVerticalOffset(1);

        int x = Mth.floor(this.getX() + this.attachmentOffsetX - (float) this.attachmentNormal.x * (verticalOffset + 0.2f));
        int y = Mth.floor(this.getY() + this.attachmentOffsetY - (float) this.attachmentNormal.y * (verticalOffset + 0.2f));
        int z = Mth.floor(this.getZ() + this.attachmentOffsetZ - (float) this.attachmentNormal.z * (verticalOffset + 0.2f));
        BlockPos pos = new BlockPos(x, y, z);

        if (this.level().isEmptyBlock(pos) && this.attachmentNormal.y < 0.0f) {
            BlockPos posDown = pos.below();
            BlockState stateDown = this.level().getBlockState(posDown);

            if (stateDown.is(BlockTags.FENCES) || stateDown.is(BlockTags.WALLS) || stateDown.getBlock() instanceof FenceGateBlock) {
                return posDown;
            }
        }

        return pos;
    }

    @Override
    public boolean getAdjustedCanTriggerWalking(boolean canTriggerWalking) {
        if (this.preWalkingPosition != null && this.canClimberTriggerWalking() && !this.isPassenger()) {
            Vec3 moved = this.position().subtract(this.preWalkingPosition);
            this.preWalkingPosition = null;

            BlockPos pos = this.getOnPos();
            BlockState state = this.level().getBlockState(pos);

            double dx = moved.x;
            double dy = moved.y;
            double dz = moved.z;

            Vec3 tangentialMovement = moved.subtract(this.attachmentNormal.scale(this.attachmentNormal.dot(moved)));

            // TODO: fix
            this.nextStepDistance = (float) ((double) this.nextStepDistance + tangentialMovement.length() * 0.6D);

            this.moveDist = (float) ((double) this.moveDist + Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.6D);

            if (this.moveDist > this.nextStepDistance && !state.isAir()) {
                this.nextStepDistance = this.nextStep();

                if (this.isInWater()) {
                    Entity controller = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;

                    float multiplier = controller == this ? 0.35F : 0.4F;

                    Vec3 motion = controller.getDeltaMovement();

                    float swimStrength = (float) Math.sqrt(motion.x * motion.x * (double) 0.2F + motion.y * motion.y + motion.z * motion.z * 0.2F) * multiplier;
                    if (swimStrength > 1.0F) {
                        swimStrength = 1.0F;
                    }

                    this.playSwimSound(swimStrength);
                } else {
                    this.playStepSound(pos, state);
                }
            } else if (state.isAir()) {
                this.processFlappingMovement();
            }
        }

        return false;
    }

    @Override
    public boolean canClimberTriggerWalking() {
        return true;
    }

    public void setLocationFromBoundingbox() {
        AABB axisalignedbb = this.getBoundingBox();
        this.setPosRaw((axisalignedbb.minX + axisalignedbb.maxX) / 2.0D, axisalignedbb.minY, (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D);
    }


}
