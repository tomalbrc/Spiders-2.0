package tcb.spiderstpo.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;

import java.util.Optional;

public class CommonEventHandlers {
    public static Optional<EntityDimensions> onEntitySize(Entity entity) {

        if (entity instanceof CaveSpider) {
            return Optional.of(EntityDimensions.scalable(0.7f, 0.5f));
        } else if (entity instanceof Spider) {
            return Optional.of(EntityDimensions.scalable(0.95f, 0.85f));
        }
        return Optional.empty();
    }
}
