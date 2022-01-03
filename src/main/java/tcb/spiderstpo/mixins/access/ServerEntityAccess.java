package tcb.spiderstpo.mixins.access;

import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface ServerEntityAccess {

    @Accessor
    int getTickCount();

    @Accessor
    int getUpdateInterval();
}
