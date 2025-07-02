package tcb.spiderstpo.polymer;

import com.mojang.math.Axis;
import de.tomalbrc.bil.core.element.PerPlayerItemDisplayElement;
import de.tomalbrc.bil.core.holder.entity.living.LivingEntityHolder;
import de.tomalbrc.bil.core.holder.wrapper.Bone;
import de.tomalbrc.bil.core.holder.wrapper.DisplayWrapper;
import de.tomalbrc.bil.core.holder.wrapper.ModelBone;
import de.tomalbrc.bil.core.model.Model;
import de.tomalbrc.bil.core.model.Node;
import de.tomalbrc.bil.core.model.Pose;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tcb.spiderstpo.common.SpiderMod;
import tcb.spiderstpo.common.entity.mob.IClimberEntity;
import tcb.spiderstpo.common.entity.mob.Orientation;

import java.util.List;

public class SpiderHolder<T extends PolyBetterSpiderEntity> extends LivingEntityHolder<T> {
    public static Model MODEL = new CustomBbModelLoader().loadResource(ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spider"));

    ModelBone rightFront;
    ModelBone leftFront;
    ModelBone middleRightFront;
    ModelBone middleLeftFront;
    ModelBone middleRightBack;
    ModelBone middleLeftBack;
    ModelBone rightBack;
    ModelBone leftBack;

    public static void load() {
    }

    public SpiderHolder(T parent) {
        super(parent, MODEL);
    }

    protected void onDataLoaded() {
        super.onDataLoaded();
        for (Bone<?> bone : this.bones) {
            if (bone.name().equals("leg1")) rightBack = (ModelBone) bone;
            if (bone.name().equals("leg2")) leftBack = (ModelBone) bone;
            if (bone.name().equals("leg3")) middleRightBack = (ModelBone) bone;
            if (bone.name().equals("leg4")) middleLeftBack = (ModelBone) bone;
            if (bone.name().equals("leg5")) middleRightFront = (ModelBone) bone;
            if (bone.name().equals("leg6")) middleLeftFront = (ModelBone) bone;
            if (bone.name().equals("leg7")) rightFront = (ModelBone) bone;
            if (bone.name().equals("leg8")) leftFront = (ModelBone) bone;
        }
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

    @Override
    public void updateElement(ServerPlayer serverPlayer, DisplayWrapper<?> display, @Nullable Pose pose) {
        display.element().setYaw(0);
        display.element().setInterpolationDuration(serverPlayer, 2);
        if (pose == null) {
            this.applyPose(serverPlayer, display.getLastPose(serverPlayer), display);
        } else {
            this.applyPose(serverPlayer, pose, display);
        }
    }

    @Override
    protected void applyPose(ServerPlayer serverPlayer, Pose pose, DisplayWrapper<?> display) {
        Node node = getRotationParent(display.node());
        boolean isHead = node != null;
        boolean isDead = this.parent.deathTime > 0;

        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translate(pose.readOnlyTranslation().sub(0f, parent.getBbHeight() / entityScale, 0.f, new Vector3f()));
        matrix4f.rotate(pose.readOnlyLeftRotation());
        matrix4f.scale(new Vector3f(1.f));
        matrix4f.rotate(pose.readOnlyRightRotation());

        var rot = getRot(display);
        if (rot != null) {
            if (display == leftBack || display == leftFront || display == middleLeftFront || display == middleLeftBack) {
                matrix4f.rotateY(-rot.y);
                matrix4f.rotateZ(-rot.x);
            } else {
                matrix4f.rotateY(rot.y);
                matrix4f.rotateZ(rot.x);
            }
            display.element().setInterpolationDuration(serverPlayer, 2);
        }

        var yy = 0.f;
        if (isHead && !isDead) {
            yy = (Mth.DEG_TO_RAD * -this.parent.yHeadRotO);
            var x = (Mth.DEG_TO_RAD * this.parent.xRotO);

            Vector3f pivotOffset = node.transform().origin().get(new Vector3f()).mul(1, 0, 1);
            matrix4f.translateLocal(pivotOffset);

            matrix4f.rotateX(-x);
        } else {
            yy = -this.parent.yBodyRotO * Mth.DEG_TO_RAD;
        }
        matrix4f.rotateLocalY(yy);


        if (isDead) {
            matrix4f.translateLocal(0, this.parent.getBbHeight(), 0);
            matrix4f.rotateLocalZ(-this.deathAngle * Mth.HALF_PI);
            matrix4f.translateLocal(0, -this.parent.getBbHeight(), 0);
        }

        Matrix4f orientationMat = calculateRotationMatrix(.1f);
        matrix4f = orientationMat.mul(matrix4f);

        matrix4f.scale(pose.readOnlyScale());

        display.element().setTransformation(serverPlayer, matrix4f);
        display.element().startInterpolationIfDirty(serverPlayer);
    }

    @Override
    protected void setupElements(List<Bone<?>> bones) {
        ObjectIterator<Node> iterator = this.model.nodeMap().values().iterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            Pose defaultPose = this.model.defaultPose().get(node.uuid());
            if (node.type() == Node.NodeType.BONE) {
                var bone = this.createBoneDisplay(node.modelData());
                if (bone != null) {
                    if (node.name().equals("head2")) {
                        bones.add(headEmissiveBone(bone, node, defaultPose));
                        bone.setBrightness(Brightness.FULL_BRIGHT);
                        this.addElement(bone);
                    } else {
                        bones.add(ModelBone.of(bone, node, defaultPose));
                        this.addElement(bone);
                    }
                }
            }
        }
    }

    public static Bone<?> headEmissiveBone(PerPlayerItemDisplayElement element, @NotNull Node node, Pose defaultPose) {
        Node current = node;

        boolean head;
        for(head = false; current != null; current = current.parent()) {
            if (current.headTag()) {
                head = true;
                break;
            }
        }

        return new ModelBone(element, node, defaultPose, head) {
            public void setInvisible(boolean invisible) {
                // noop
            }
        };
    }

    @Override
    public void onDimensionsUpdated(EntityDimensions dimensions) {
        dimensions = dimensions.scale(1.f,0.925f);
        super.onDimensionsUpdated(dimensions);
    }

    private Matrix4f calculateRotationMatrix(float partialTicks) {
        IClimberEntity climber = this.parent;

        Orientation orientation = climber.getOrientation();
        Orientation renderOrientation = climber.calculateOrientation(partialTicks);
        climber.setRenderOrientation(renderOrientation);

        float verticalOffset = climber.getVerticalOffset(partialTicks);
        float x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks) - (float) renderOrientation.normal.x * verticalOffset;
        float y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal.y * verticalOffset;
        float z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal.z * verticalOffset;

        Matrix4f matrix = new Matrix4f();
        matrix.translate(-x, -y, -z);

        matrix.rotate(Axis.YP.rotationDegrees(renderOrientation.yaw));
        matrix.rotate(Axis.XP.rotationDegrees(renderOrientation.pitch));
        matrix.rotate(Axis.YP.rotationDegrees(Math.signum(0.5f - orientation.componentY - orientation.componentZ - orientation.componentX) * renderOrientation.yaw));

        matrix.translate(0, 0.4f,0);

        return matrix;
    }

    @Nullable
    public Vec2 getRot(DisplayWrapper<?> wrapper) {
        if (wrapper == rightFront || wrapper == leftFront) {
            return front(this.parent.walkAnimation);
        }

        if (wrapper == middleRightFront || wrapper == middleLeftFront) {
            return middleFront(this.parent.walkAnimation);
        }

        if (wrapper == middleRightBack || wrapper == middleLeftBack) {
            return middleHind(this.parent.walkAnimation);
        }

        if (wrapper == rightBack || wrapper == leftBack) {
            return hind(this.parent.walkAnimation);
        }

        return null;
    }

    public Vec2 hind(WalkAnimationState walkAnimationState) {
        float pos = walkAnimationState.position() * 0.6662F;
        float speed = walkAnimationState.speed();
        float y = -(Mth.cos(pos * 2.0F + 0.0F) * 0.4F) * speed;
        float z = Math.abs(Mth.sin(pos + 0.0F) * 0.4F) * speed;
        return new Vec2(z, y);
    }

    public Vec2 middleHind(WalkAnimationState walkAnimationState) {
        float pos = walkAnimationState.position() * 0.6662F;
        float speed = walkAnimationState.speed();
        float y = -(Mth.cos(pos * 2.0F + Mth.PI) * 0.4F) * speed;
        float z = Math.abs(Mth.sin(pos + Mth.PI) * 0.4F) * speed;
        return new Vec2(z, y);
    }

    public Vec2 front(WalkAnimationState walkAnimationState) {
        float pos = walkAnimationState.position() * 0.6662F;
        float speed = walkAnimationState.speed();
        float y = -(Mth.cos(pos * 2.0F + (Mth.PI * 1.5F)) * 0.4F) * speed;
        float z = Math.abs(Mth.sin(pos + (Mth.PI * 1.5F)) * 0.4F) * speed;
        return new Vec2(z, y);
    }

    public Vec2 middleFront(WalkAnimationState walkAnimationState) {
        float pos = walkAnimationState.position() * 0.6662F;
        float speed = walkAnimationState.speed();
        float y = -(Mth.cos(pos * 2.0F + (Mth.PI / 2F)) * 0.4F) * speed;
        float z = Math.abs(Mth.sin(pos + (Mth.PI / 2F)) * 0.4F) * speed;
        return new Vec2(z, y);
    }
}

