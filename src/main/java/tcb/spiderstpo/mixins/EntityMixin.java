package tcb.spiderstpo.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tcb.spiderstpo.common.CommonEventHandlers;
import tcb.spiderstpo.common.entity.mob.IEntityMovementHook;
import tcb.spiderstpo.common.entity.mob.IEntityReadWriteHook;
import tcb.spiderstpo.common.entity.mob.IEntityRegisterDataHook;

import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityMovementHook, IEntityReadWriteHook, IEntityRegisterDataHook {

    @Shadow private EntityDimensions dimensions;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setDimensions(EntityType entityType, Level level, CallbackInfo ci) {
        final Optional<EntityDimensions> entityDimensions = CommonEventHandlers.onEntitySize((Entity) (Object) this);
        entityDimensions.ifPresent(value -> this.dimensions = value);
    }

    @Inject(method = "refreshDimensions", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;"))
    private void setDimensionsNew(CallbackInfo ci) {
        final Optional<EntityDimensions> entityDimensions = CommonEventHandlers.onEntitySize((Entity) (Object) this);
        entityDimensions.ifPresent(value -> this.dimensions = value);
    }
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMovePre(MoverType type, Vec3 pos, CallbackInfo ci) {
        if(this.onMove(type, pos, true)) {
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void onMovePost(MoverType type, Vec3 pos, CallbackInfo ci) {
        this.onMove(type, pos, false);
    }

    @Override
    public boolean onMove(MoverType type, Vec3 pos, boolean pre) {
        return false;
    }

    @Inject(method = "getOnPos", at = @At("RETURN"), cancellable = true)
    private void onGetOnPosition(CallbackInfoReturnable<BlockPos> ci) {
        BlockPos adjusted = this.getAdjustedOnPosition(ci.getReturnValue());
        if(adjusted != null) {
            ci.setReturnValue(adjusted);
        }
    }

    @Override
    public BlockPos getAdjustedOnPosition(BlockPos onPosition) {
        return null;
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity$MovementEmission;emitsAnything()Z"))
    public boolean bop(Entity.MovementEmission instance, Operation<Boolean> original){
        return this.getAdjustedCanTriggerWalking(instance.emitsAnything());
    }

    @Override
    public boolean getAdjustedCanTriggerWalking(boolean canTriggerWalking) {
        return canTriggerWalking;
    }

    @Inject(method = "load", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
            shift = At.Shift.AFTER
    ))
    private void onRead(ValueInput valueInput, CallbackInfo ci) {
        this.onRead(valueInput);
    }

    @Override
    public void onRead(ValueInput nbt) { }

    @Inject(method = "saveWithoutId", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            shift = At.Shift.AFTER
    ))
    private void onWrite(ValueOutput valueOutput, CallbackInfo ci) {
        this.onWrite(valueOutput);
    }

    @Override
    public void onWrite(ValueOutput nbt) { }
}
