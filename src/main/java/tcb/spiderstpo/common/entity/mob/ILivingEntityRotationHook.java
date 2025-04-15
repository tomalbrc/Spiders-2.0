package tcb.spiderstpo.common.entity.mob;

public interface ILivingEntityRotationHook {
    float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport);

    float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport);

    float getTargetHeadYaw(float yaw, int rotationIncrements);
}
