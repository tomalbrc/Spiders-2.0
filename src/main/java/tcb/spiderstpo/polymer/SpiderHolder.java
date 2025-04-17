package tcb.spiderstpo.polymer;

import com.mojang.math.Axis;
import de.tomalbrc.bil.core.holder.entity.living.LivingEntityHolder;
import de.tomalbrc.bil.core.holder.wrapper.DisplayWrapper;
import de.tomalbrc.bil.core.model.Model;
import de.tomalbrc.bil.core.model.Node;
import de.tomalbrc.bil.core.model.Pose;
import de.tomalbrc.bil.file.loader.BbModelLoader;
import de.tomalbrc.bil.util.Utils;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tcb.spiderstpo.common.SpiderMod;
import tcb.spiderstpo.common.entity.mob.IClimberEntity;
import tcb.spiderstpo.common.entity.mob.Orientation;

public class SpiderHolder<T extends PolyBetterSpiderEntity> extends LivingEntityHolder<T> {
    public static Model MODEL = BbModelLoader.load(ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spider"));

    public static void load() {
    }

    public SpiderHolder(T parent) {
        super(parent, MODEL);
    }

    @Nullable
    private Node getRotationParent(Node node) {
        var currentNode = node;
        while (currentNode != null) {
            if (currentNode.headTag())
                return currentNode;

            currentNode = currentNode.parent();
        }

        return null;
    }

    public void updateElement(DisplayWrapper<?> display, @Nullable Pose pose) {
        display.element().setYaw(0);
        if (pose == null) {
            this.applyPose(display.getLastPose(), display);
        } else {
            this.applyPose(pose, display);
        }
    }

    @Override
    protected void applyPose(Pose pose, DisplayWrapper<?> display) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translate(pose.readOnlyTranslation().sub(0f, parent.getBbHeight() / entityScale, 0f, new Vector3f()));
        matrix4f.rotate(pose.readOnlyLeftRotation());
        matrix4f.scale(new Vector3f(1.f));
        matrix4f.rotate(pose.readOnlyRightRotation());

        var node = getRotationParent(display.node());

        boolean isHead = node != null;
        boolean isDead = this.parent.deathTime > 0;


//        if (isHead) {
//            var y = (Mth.DEG_TO_RAD * Mth.rotLerp(0.5F, this.parent.yHeadRotO, this.parent.yHeadRot));
//            var x = (Mth.DEG_TO_RAD * Mth.lerp(0.5F, this.parent.xRotO, this.parent.getXRot()));
//            matrix4f.rotateLocalX(x);
//            matrix4f.rotateLocalY(y);
//        } else {

//        }
        matrix4f.rotateLocalY(a180(-this.parent.yBodyRotO) * Mth.DEG_TO_RAD);

        if (isDead) {
            matrix4f.translateLocal(0, this.parent.getBbHeight(), 0);
            matrix4f.rotateLocalZ(-this.deathAngle * ((float) Math.PI / 2F));
            matrix4f.translateLocal(0, -this.parent.getBbHeight(), 0);
        } else {
            Matrix4f p = new Matrix4f();
            calc(1.0f, p);
            matrix4f = p.mul(matrix4f);
        }

        matrix4f.scale(pose.readOnlyScale());

        display.element().setTransformation(matrix4f);
        display.element().startInterpolationIfDirty();
    }

    public static float a180(float angle) {
        angle = ((angle + 180) % 360);
        return angle - 180;
    }

    private float a360(float f) {
        return (f + 360) % 360;
    }

    @Override
    public void onDimensionsUpdated(EntityDimensions dimensions) {
        dimensions = dimensions.scale(0.95f);

        this.collisionElement.setSize(Math.max(1, Utils.toSlimeSize(Math.min(dimensions.width(), dimensions.height()))));
        this.sendPacket(new ClientboundBundlePacket(Utils.updateClientInteraction(this.hitboxInteraction, dimensions)));
    }

    void calc(float partialTicks, Matrix4f matrixStack) {
        IClimberEntity climber = this.parent;

        Orientation orientation = climber.getOrientation();
        Orientation renderOrientation = climber.calculateOrientation(partialTicks);
        climber.setRenderOrientation(renderOrientation);

        float verticalOffset = climber.getVerticalOffset(partialTicks);

        float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
        float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
        float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

        matrixStack.translate(-x, -y, -z);

        matrixStack.rotate(Axis.YP.rotationDegrees(renderOrientation.yaw));
        matrixStack.rotate(Axis.XP.rotationDegrees(renderOrientation.pitch));
        matrixStack.rotate(Axis.YP.rotationDegrees(Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) * renderOrientation.yaw));
    }
}

