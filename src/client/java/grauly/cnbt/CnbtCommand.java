package grauly.cnbt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
            context.getSource().sendFeedback(Component.literal("entity: " + entitySelector.toString()));
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


    private static Vec3 convertWorldCoordinates(WorldCoordinates pos, Vec3 origin) {
        double x = pos.x().value() + (pos.isXRelative() ? origin.x() : 0);
        double y = pos.y().value() + (pos.isYRelative() ? origin.y() : 0);
        double z = pos.z().value() + (pos.isZRelative() ? origin.z() : 0);
        return new Vec3(x, y, z);
    }
}
