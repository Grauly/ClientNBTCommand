package grauly.cnbt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import grauly.cnbt.mixin.EntitySelectorAccessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.Util;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

;

public class CnbtCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> literal = ClientCommandManager.literal("cnbt");

        literal.then(ClientCommandManager.literal("block").then(ClientCommandManager.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
            WorldCoordinates coords = context.getArgument("pos", WorldCoordinates.class);
            Vec3 pos = convertWorldCoordinates(coords, context.getSource().getPosition());
            BlockPos blockPos = BlockPos.containing(pos);
            BlockEntity entity = context.getSource().getWorld().getBlockEntity(blockPos);
            if (entity == null) {
                throw new SimpleCommandExceptionType(Component.translatable("commands.data.block.invalid")).create();
            }
            BlockDataAccessor accessor = new BlockDataAccessor(entity, blockPos);
            context.getSource().sendFeedback(accessor.getPrintSuccess(accessor.getData()));
            return 1;
        })));

        literal.then(ClientCommandManager.literal("entity").then(ClientCommandManager.argument("entity", EntityArgument.entity()).executes(context -> {
            EntitySelector entitySelector = context.getArgument("entity", EntitySelector.class);
            Entity entity = resolveEntitySelector(entitySelector, context);
            EntityDataAccessor entityDataAccessor = new EntityDataAccessor(entity);
            context.getSource().sendFeedback(entityDataAccessor.getPrintSuccess(entityDataAccessor.getData()));
            return 1;
        })));

        literal.then(ClientCommandManager.literal("hand").executes(context -> {
            ItemStack stack = context.getSource().getPlayer().getMainHandItem();
            TagValueOutput tagValueOutput = TagValueOutput.createWithoutContext(new ProblemReporter.ScopedCollector(ClientNBTCommandClient.LOGGER));
            tagValueOutput.store("components", DataComponentMap.CODEC, stack.getComponents());
            CompoundTag tag = tagValueOutput.buildResult();
            context.getSource().sendFeedback(NbtUtils.toPrettyComponent(tag));
            return 1;
        }));

        dispatcher.register(literal);
    }

    private static Entity resolveEntitySelector(EntitySelector entitySelector, CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        List<? extends Entity> foundEntities = resolveMultiEntitySelector(entitySelector, context);
        if (foundEntities.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        } else if (foundEntities.size() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        } else {
            return foundEntities.getFirst();
        }
    }

    private static List<? extends Entity> resolveMultiEntitySelector(EntitySelector entitySelector, CommandContext<FabricClientCommandSource> context) {
        EntitySelectorAccessor entitySelectorAccessor = (EntitySelectorAccessor) entitySelector;
        if (!entitySelector.includesEntities()) {
            return resolvePlayers(entitySelector, context);
        } else if (entitySelectorAccessor.getPlayerName() != null) {
            Optional<AbstractClientPlayer> optionalPlayer = context.getSource().getWorld().players().stream().filter(player -> player.getName().toString().equals(entitySelectorAccessor.getPlayerName())).findFirst();
            return optionalPlayer.isPresent() ? List.of(optionalPlayer.get()) : List.of();
        } else if (entitySelectorAccessor.getUUID() != null) {
            Entity entity = context.getSource().getWorld().getEntity(entitySelectorAccessor.getUUID());
            if (entity == null) return List.of();
            if (!entity.getType().isEnabled(context.getSource().enabledFeatures())) return List.of();
            return List.of(entity);
        } else {
            Vec3 selectorPosition = entitySelectorAccessor.getPosition().apply(context.getSource().getPosition());
            AABB aabb = entitySelectorAccessor.getAabb();
            aabb = aabb != null ? aabb.move(selectorPosition) : null;
            Predicate<Entity> predicate = producePredicate(selectorPosition, aabb, context.getSource().enabledFeatures(), entitySelector);
            if (entitySelectorAccessor.isCurrentEntity()) {
                boolean isSelfTargeted = context.getSource().getEntity() instanceof LocalPlayer player && predicate.test(player);
                return isSelfTargeted ? List.of(context.getSource().getPlayer()) : List.of();
            }
            int maxResults = getMaxResults(entitySelector);
            // omit world limiting check, we are always limited to that
            ArrayList<Entity> list = new ArrayList<>();
            context.getSource().getWorld().getEntities(entitySelectorAccessor.getType(), aabb, predicate, list, maxResults);
            return sortAndLimit(selectorPosition, list, entitySelector);
        }
    }

    private static List<? extends Player> resolvePlayers(EntitySelector entitySelector, CommandContext<FabricClientCommandSource> context) {
        EntitySelectorAccessor entitySelectorAccessor = (EntitySelectorAccessor) entitySelector;
        if (entitySelectorAccessor.getPlayerName() != null) {
            Optional<AbstractClientPlayer> optionalPlayer = context.getSource().getWorld().players().stream().filter(player -> player.getName().toString().equals(entitySelectorAccessor.getPlayerName())).findFirst();
            return optionalPlayer.isPresent() ? List.of(optionalPlayer.get()) : List.of();
        } else if (entitySelectorAccessor.getUUID() != null) {
            Player player = context.getSource().getWorld().getPlayerByUUID(entitySelectorAccessor.getUUID());
            return player != null ? List.of(player) : List.of();
        } else {
            Vec3 selectorPosition = entitySelectorAccessor.getPosition().apply(context.getSource().getPosition());
            AABB aabb = entitySelectorAccessor.getAabb();
            aabb = aabb != null ? aabb.move(selectorPosition) : null;
            Predicate<Entity> predicate = producePredicate(selectorPosition, aabb, null, entitySelector);
            if (entitySelectorAccessor.isCurrentEntity()) {
                boolean isSelfTargeted = context.getSource().getEntity() instanceof LocalPlayer player && predicate.test(player);
                return isSelfTargeted ? List.of(context.getSource().getPlayer()) : List.of();
            }
            int maxResults = getMaxResults(entitySelector);
            // omit world limiting check, we are always limited to that
            return sortAndLimit(selectorPosition, context.getSource().getWorld().players().stream().filter(predicate).limit(maxResults).toList(), entitySelector);
        }
    }

    private static <T extends Entity> List<T> sortAndLimit(Vec3 vec3, List<T> list, EntitySelector entitySelector) {
        EntitySelectorAccessor entitySelectorAccessor = (EntitySelectorAccessor) entitySelector;
        if (list.size() > 1) {
            entitySelectorAccessor.getOrder().accept(vec3, list);
        }

        return list.subList(0, Math.min(entitySelector.getMaxResults(), list.size()));
    }

    private static int getMaxResults(EntitySelector entitySelector) {
        EntitySelectorAccessor entitySelectorAccessor = (EntitySelectorAccessor) entitySelector;
        return entitySelectorAccessor.getOrder() == EntitySelector.ORDER_ARBITRARY ? entitySelector.getMaxResults() : Integer.MAX_VALUE;
    }

    private static Predicate<Entity> producePredicate(Vec3 position, AABB aabb, FeatureFlagSet featureFlagSet, EntitySelector entitySelector) {
        EntitySelectorAccessor entitySelectorAccessor = (EntitySelectorAccessor) entitySelector;
        boolean hasFeatureFlag = featureFlagSet != null;
        boolean hasAABB = aabb != null;
        boolean hasRange = entitySelectorAccessor.getRange() != null;
        ArrayList<Predicate<Entity>> predicateCollection = new ArrayList<>(entitySelectorAccessor.getContextFreePredicates());
        if (hasFeatureFlag) {
            predicateCollection.add(entity -> entity.getType().isEnabled(featureFlagSet));
        }
        if (hasAABB) {
            predicateCollection.add(entity -> aabb.intersects(entity.getBoundingBox()));
        }
        if (hasRange) {
            predicateCollection.add(entity -> entitySelectorAccessor.getRange().matchesSqr(entity.distanceToSqr(position)));
        }
        return Util.allOf(predicateCollection);
    }

    private static Vec3 convertWorldCoordinates(WorldCoordinates pos, Vec3 origin) {
        double x = pos.x().value() + (pos.isXRelative() ? origin.x() : 0);
        double y = pos.y().value() + (pos.isYRelative() ? origin.y() : 0);
        double z = pos.z().value() + (pos.isZRelative() ? origin.z() : 0);
        return new Vec3(x, y, z);
    }
}
