package tcb.spiderstpo.common.entity.mob;

import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface IMobEntityNavigatorHook {
    @Nullable
    PathNavigation onCreateNavigator(Level world);
}
