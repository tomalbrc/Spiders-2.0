package tcb.spiderstpo.mixins;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tcb.spiderstpo.common.SpiderMod;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> {
    @Unique Identifier spider = Identifier.withDefaultNamespace("spider");
    @Unique Identifier fakeSpider = Identifier.fromNamespaceAndPath(SpiderMod.MODID, "spider");
    @Unique ResourceKey<T> spiderKey;

    @ModifyVariable(
            method = {"get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", "containsKey(Lnet/minecraft/resources/Identifier;)Z"},
            at = @At("HEAD"),
            argsOnly = true
    )
    private Identifier aliasIdentifierParameter(Identifier original) {
        if (this == BuiltInRegistries.ENTITY_TYPE && original.equals(spider)) return fakeSpider;
        return original;
    }

    @ModifyVariable(
            method = {"getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", "getOrCreateHolderOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;", "containsKey(Lnet/minecraft/resources/ResourceKey;)Z", "registrationInfo(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;"},
            at = @At("HEAD"),
            argsOnly = true
    )
    private ResourceKey<T> aliasRegistryKeyParameter(ResourceKey<T> original) {
        if (this == BuiltInRegistries.ENTITY_TYPE && original.identifier().equals(spider)) {
            if (spiderKey == null) spiderKey = ResourceKey.create(original.registryKey(), Identifier.fromNamespaceAndPath("spiderstpo", "spider"));
            return spiderKey;
        }
        return original;
    }
}
