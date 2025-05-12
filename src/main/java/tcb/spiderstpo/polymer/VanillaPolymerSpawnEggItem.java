package tcb.spiderstpo.polymer;

import eu.pb4.polymer.core.api.item.PolymerSpawnEggItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Objects;

public class VanillaPolymerSpawnEggItem extends PolymerSpawnEggItem {
    public VanillaPolymerSpawnEggItem(EntityType<? extends Mob> type, Item visualItem, Properties settings) {
        super(type, visualItem, settings);
    }

    @Override
    public ResourceLocation getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.getPolymerItem(itemStack, context).getDefaultInstance().get(DataComponents.ITEM_MODEL);
    }

    @NotNull
    public InteractionResult useOn(UseOnContext useOnContext) {
        Level level = useOnContext.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemStack = useOnContext.getItemInHand();
            BlockPos blockPos = useOnContext.getClickedPos();
            Direction direction = useOnContext.getClickedFace();
            BlockState blockState = level.getBlockState(blockPos);
            BlockEntity entityType = level.getBlockEntity(blockPos);
            if (entityType instanceof Spawner spawner) {
                spawner.setEntityId(EntityType.SPIDER, level.getRandom());
                level.sendBlockUpdated(blockPos, blockState, blockState, 3);
                level.gameEvent(useOnContext.getPlayer(), GameEvent.BLOCK_CHANGE, blockPos);
                itemStack.shrink(1);
            } else {
                BlockPos blockPos2;
                if (blockState.getCollisionShape(level, blockPos).isEmpty()) {
                    blockPos2 = blockPos;
                } else {
                    blockPos2 = blockPos.relative(direction);
                }

                EntityType<?> type = this.getType(level.registryAccess(), itemStack);
                if (type.spawn((ServerLevel)level, itemStack, useOnContext.getPlayer(), blockPos2, EntitySpawnReason.SPAWN_ITEM_USE, true, !Objects.equals(blockPos, blockPos2) && direction == Direction.UP) != null) {
                    itemStack.shrink(1);
                    level.gameEvent(useOnContext.getPlayer(), GameEvent.ENTITY_PLACE, blockPos);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }
}