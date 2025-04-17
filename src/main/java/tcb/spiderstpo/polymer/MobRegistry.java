package tcb.spiderstpo.polymer;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import tcb.spiderstpo.common.SpiderMod;

import java.util.Map;
import java.util.function.Function;

public class MobRegistry {
    public static final EntityType<PolyBetterSpiderEntity> SPIDER = register(
            BetterSpiderEntity.ID,
            FabricEntityType.Builder.createMob(PolyBetterSpiderEntity::new, MobCategory.MONSTER, x -> x
                            .defaultAttributes(Spider::createAttributes)
                            .spawnRestriction(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Spider::checkMonsterSpawnRules))
                    .sized(1.0F, 0.85F).eyeHeight(0.65F).passengerAttachments(0.765F).clientTrackingRange(8)
    );


    private static <T extends Entity> EntityType<T> register(ResourceLocation id, EntityType.Builder<T> builder) {
        @SuppressWarnings("unchecked") Map<String, Type<?>> types = (Map<String, Type<?>>) DataFixers.getDataFixer().getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion())).findChoiceType(References.ENTITY).types();
        types.put(id.toString(), types.get(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE).toString()));

        EntityType<T> type = builder.build(ResourceKey.create(Registries.ENTITY_TYPE, id));
        PolymerEntityUtils.registerType(type);

        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    public static void register() {
        BiomeHelper.addSpawn(SPIDER, 10, 1, 3, BiomeSelectors.spawnsOneOf(EntityType.SPIDER));
        addSpawnEgg(SPIDER, Items.SPIDER_SPAWN_EGG);

        PolymerItemGroupUtils.registerPolymerItemGroup(ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spawn-eggs"), ITEM_GROUP);
    }

    private static void addSpawnEgg(EntityType<? extends Mob> type, Item vanillaItem) {
        register(ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, EntityType.getKey(type).getPath() + "_spawn_egg"), properties -> new VanillaPolymerSpawnEggItem(type, vanillaItem, properties));
    }

    static public <T extends Item> void register(ResourceLocation identifier, Function<Item.Properties, T> function) {
        var x = function.apply(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, identifier)));
        Registry.register(BuiltInRegistries.ITEM, identifier, x);
        SPAWN_EGGS.putIfAbsent(identifier, x);
    }

    public static final Object2ObjectOpenHashMap<ResourceLocation, Item> SPAWN_EGGS = new Object2ObjectOpenHashMap<>();
    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab.Builder(null, -1)
            .title(Component.literal("BetterSpiders").withStyle(ChatFormatting.DARK_GREEN))
            .icon(Items.BAT_SPAWN_EGG::getDefaultInstance)
            .displayItems((parameters, output) -> SPAWN_EGGS.values().forEach(output::accept))
            .build();
}
