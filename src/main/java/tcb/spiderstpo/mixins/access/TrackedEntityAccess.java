package tcb.spiderstpo.mixins.access;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.TrackedEntity.class)
public interface TrackedEntityAccess {

    @Accessor
    ServerEntity getServerEntity();
}
