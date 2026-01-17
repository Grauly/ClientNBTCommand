package grauly.cnbt.mixin;

import grauly.cnbt.access.NonAabbEntityGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Mixin(ClientLevel.class)
public abstract class ClientLevelEntityGetterMixin implements NonAabbEntityGetter {

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public <T extends Entity> void cnbt$getEntities(EntityTypeTest<Entity, T> entityTypeTest, Predicate<? super T> predicate, List<? super T> list, int i) {
        ArrayList<T> entities = new ArrayList<>();
        getEntities().getAll().forEach(entity -> {entities.add(entityTypeTest.tryCast(entity));});
        list.addAll(entities.stream().filter(Objects::nonNull).filter(predicate).limit(i).toList());
    }
}
