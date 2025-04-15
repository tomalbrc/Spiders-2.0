package tcb.spiderstpo.mixins;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tcb.spiderstpo.common.entity.mob.ILivingEntityRotationHook;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin implements ILivingEntityRotationHook {
    @Override
    public float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        return yaw;
    }

    @Override
    public float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        return pitch;
    }

    @ModifyVariable(method = "lerpHeadTo(FI)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float onSetHeadRotation(float yaw, float yaw2, int rotationIncrements) {
        return this.getTargetHeadYaw(yaw, rotationIncrements);
    }

    @Override
    public float getTargetHeadYaw(float yaw, int rotationIncrements) {
        return yaw;
    }
}
