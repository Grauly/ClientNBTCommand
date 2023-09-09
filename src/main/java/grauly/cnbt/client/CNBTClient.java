package grauly.cnbt.client;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.EntityDataObject;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.NbtDataSource;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CNBTClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("cnbt").executes(context -> {
                                context.getSource().sendFeedback(Text.literal("No Arguments given: valid arguments are: hand/self/target"));
                                return 0;
                            }).then(literal("hand").executes(context -> {
                                try {
                                    var cnbt = context.getSource().getPlayer().getInventory().getMainHandStack().getNbt();
                                    context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(cnbt));
                                } catch (NullPointerException e) {
                                    context.getSource().sendError(Text.literal("Nothing in hand"));
                                    return 1;
                                }
                                return 0;
                            }))
                            .then(literal("self").executes(context -> {
                                try {
                                    EntityDataObject entityDataObject = new EntityDataObject(context.getSource().getPlayer());
                                    context.getSource().sendFeedback(entityDataObject.feedbackQuery(entityDataObject.getNbt()));
                                } catch (NullPointerException e) {
                                    context.getSource().sendError(Text.literal("You are null. How?"));
                                    return 1;
                                }
                                return 0;
                            }))
                            .then(literal("target").executes(context -> {
                                var entity = context.getSource().getEntity();
                                double d = (double)MinecraftClient.getInstance().interactionManager.getReachDistance();
                                Vec3d vec3d = entity.getCameraPosVec(MinecraftClient.getInstance().getTickDelta());
                                Vec3d vec3d2 = entity.getRotationVec(1.0F);
                                Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
                                Box box = entity.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);
                                EntityHitResult entityHitResult = ProjectileUtil.raycast(entity, vec3d, vec3d3, box, (entityx) -> !entityx.isSpectator(), d);
                                if(entityHitResult != null) {
                                    var targetedEntity = entityHitResult.getEntity();
                                    var data = new EntityDataObject(targetedEntity);
                                    context.getSource().sendFeedback(data.feedbackQuery(data.getNbt()));
                                    return 0;
                                }
                                var hitResult = context.getSource().getPlayer().raycast(5d, MinecraftClient.getInstance().getTickDelta(), false);
                                if (Objects.requireNonNull(hitResult.getType()) == HitResult.Type.BLOCK) {
                                    try {
                                        var block = ((BlockHitResult) hitResult).getBlockPos();
                                        var blockEntity = MinecraftClient.getInstance().world.getBlockEntity(block);
                                        BlockDataObject blockDataObject = new BlockDataObject(blockEntity, block);
                                        context.getSource().sendFeedback(blockDataObject.feedbackQuery(blockDataObject.getNbt()));
                                        return 0;
                                    } catch (NullPointerException e) {
                                        context.getSource().sendError(Text.literal("Not a BlockEntity or does not have data"));
                                        return 0;
                                    }
                                }
                                context.getSource().sendError(Text.literal("Could not find a target"));
                                return 0;
                            }))
            );
        }));
    }
}
