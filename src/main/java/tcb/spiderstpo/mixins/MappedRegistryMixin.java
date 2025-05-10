package tcb.spiderstpo.mixins;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tcb.spiderstpo.common.SpiderMod;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> {
    @Unique ResourceLocation spider = ResourceLocation.withDefaultNamespace("spider");
    @Unique ResourceLocation fakeSpider = ResourceLocation.fromNamespaceAndPath(SpiderMod.MODID, "spider");
    @Unique ResourceKey<T> spiderKey;

    @ModifyVariable(
            method = {"get(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", "getValue(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;", "containsKey(Lnet/minecraft/resources/ResourceLocation;)Z"},
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceLocation aliasIdentifierParameter(ResourceLocation original) {
        if (this == BuiltInRegistries.ENTITY_TYPE && original.equals(spider)) return fakeSpider;
        return original;
    }

    @ModifyVariable(
            method = {"getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", "getOrCreateHolderOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;", "containsKey(Lnet/minecraft/resources/ResourceKey;)Z", "registrationInfo(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"},
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceKey<T> aliasRegistryKeyParameter(ResourceKey<T> original) {
        if (this == BuiltInRegistries.ENTITY_TYPE && original.location().equals(spider)) {
            if (spiderKey == null) spiderKey = ResourceKey.create(original.registryKey(), ResourceLocation.fromNamespaceAndPath("spiderstpo", "spider"));
            return spiderKey;
        }
        return original;
    }
}
