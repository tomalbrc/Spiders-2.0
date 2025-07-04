package tcb.spiderstpo.polymer;

import de.tomalbrc.bil.api.AnimatedEntity;
import de.tomalbrc.bil.api.AnimatedEntityHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PolyBetterSpiderEntity extends BetterSpiderEntity implements AnimatedEntity {
    SpiderHolder<PolyBetterSpiderEntity> holder;

    public PolyBetterSpiderEntity(EntityType<? extends Spider> type, Level worldIn) {
        super(type, worldIn);

        this.holder = new SpiderHolder<>(this);
        EntityAttachment.ofTicking(holder, this);
    }

    @Override
    public AnimatedEntityHolder getHolder() {
        return this.holder;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.hurtTime > 0 || this.deathTime > 0)
            holder.setColor(0xff7e7e);
        else
            holder.clearColor();
    }

    @Override
    public @NotNull Component getName() {
        return EntityType.SPIDER.getDescription();
    }
}
