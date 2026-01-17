package grauly.cnbt.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.List;
import java.util.function.Predicate;

public interface NonAabbEntityGetter {

    default public <T extends Entity> void cnbt$getEntities(EntityTypeTest<Entity, T> entityTypeTest, Predicate<? super T> predicate, List<? super T> list, int i) {
        // [Space intentionally left blank]
        // IMPLEMENTED IN MIXIN
    }
}
