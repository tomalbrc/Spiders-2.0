package tcb.spiderstpo.mixins.access;

import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(EntityType.Builder.class)
public interface EntityTypeBuilderAccessor {
    @Accessor
    void setLootTable(DependantName<EntityType<?>, Optional<ResourceKey<LootTable>>> lootTable);
}
