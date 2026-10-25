package org.lytharalab.cabinelevator.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class Msg {
    private Msg() {
    }

    public static void action(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.YELLOW), true);
    }

    public static void success(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(key, args).withStyle(ChatFormatting.GREEN));
    }

    public static void error(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(key, args).withStyle(ChatFormatting.RED));
    }

    public static void info(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefixed(key, args).withStyle(ChatFormatting.YELLOW));
    }

    public static String serializedSuccess(String key, Object... args) {
        return Component.Serializer.toJson(Component.translatable(key, args).withStyle(ChatFormatting.GREEN));
    }

    public static String serializedError(String key, Object... args) {
        return Component.Serializer.toJson(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }

    private static MutableComponent prefixed(String key, Object... args) {
        return Component.translatable("message.cabin_elevator.prefix").append(Component.translatable(key, args));
    }
}
