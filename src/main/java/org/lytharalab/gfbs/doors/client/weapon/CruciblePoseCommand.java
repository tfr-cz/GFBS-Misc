package org.lytharalab.gfbs.doors.client.weapon;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * {@code /gfbs_crucible [third|first <roll> <y> <size>]} - dials the Crucible's held pose in without a rebuild.
 *
 * <p>The pose is the one thing about this item that cannot be checked offline: it depends on where 1.20.1 puts the
 * item inside the hand, and the numbers only look right once you see them in game. Rather than trading screenshots
 * for days, this command sets the same values the renderer uses and prints them, so the final numbers can be read
 * back and baked in.</p>
 *
 * <p>Client-only and harmless: it changes nothing on the server, and the values reset on restart.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class CruciblePoseCommand {
    private CruciblePoseCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("gfbs_crucible")
                .executes(context -> {
                    say(context.getSource().getPlayerOrException(), CrucibleItemRenderer.describePose());
                    return 1;
                })
                .then(Commands.literal("third")
                    .then(poseArguments(false)))
                .then(Commands.literal("first")
                    .then(poseArguments(true))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack, Float> poseArguments(boolean firstPerson) {
        return Commands.argument("roll", FloatArgumentType.floatArg(-360.0F, 360.0F))
            .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("size", FloatArgumentType.floatArg(0.1F, 8.0F))
                    .executes(context -> {
                        float roll = FloatArgumentType.getFloat(context, "roll");
                        float y = FloatArgumentType.getFloat(context, "y");
                        float size = FloatArgumentType.getFloat(context, "size");
                        CrucibleItemRenderer.setPose(firstPerson, roll, y * 16.0F, size);
                        say(context.getSource().getPlayerOrException(), CrucibleItemRenderer.describePose());
                        return 1;
                    })));
    }

    private static void say(net.minecraft.world.entity.player.Player player, String message) {
        player.sendSystemMessage(Component.literal("[crucible] " + message));
    }
}
