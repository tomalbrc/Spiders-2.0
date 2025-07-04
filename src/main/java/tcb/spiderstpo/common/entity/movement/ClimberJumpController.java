package tcb.spiderstpo.common.entity.movement;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tcb.spiderstpo.common.entity.mob.IClimberEntity;

public class ClimberJumpController<T extends Mob & IClimberEntity> extends JumpControl {
    protected final T climber;

    @Nullable
    protected Vec3 dir;

    public ClimberJumpController(T mob) {
        super(mob);
        this.climber = mob;
    }

    @Override
    public void jump() {
        this.setJumping(null);
    }

    public void setJumping(Vec3 dir) {
        super.jump();
        this.dir = dir;
    }

    @Override
    public void tick() {
        this.climber.setJumping(this.jump);
        if (this.jump) {
            this.climber.setJumpDirection(this.dir);
        } else if (this.dir == null) {
            this.climber.setJumpDirection(null);
        }
        this.jump = false;
    }
}
