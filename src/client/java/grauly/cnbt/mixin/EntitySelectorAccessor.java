package grauly.cnbt.mixin;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {

    @Accessor("playerName")
    String getPlayerName();

    @Accessor("entityUUID")
    UUID getUUID();

    @Accessor("position")
    Function<Vec3, Vec3> getPosition();

    @Accessor("aabb")
    AABB getAabb();

    @Accessor("contextFreePredicates")
    List<Predicate<Entity>> getContextFreePredicates();

    @Accessor("range")
    MinMaxBounds.Doubles getRange();

    @Accessor("currentEntity")
    boolean isCurrentEntity();

    @Accessor("order")
    BiConsumer<Vec3, List<? extends Entity>> getOrder();

    @Accessor("type")
    EntityTypeTest<Entity, ?> getType();

}
