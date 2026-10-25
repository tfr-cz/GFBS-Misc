package org.lytharalab.cabinelevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lytharalab.cabinelevator.network.ControllerSnapshot;
import org.lytharalab.cabinelevator.network.InterfaceSnapshot;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void openController(ControllerSnapshot snapshot, boolean openIfMissing) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = minecraft.screen;
        if (current instanceof ControllerConfigScreen screen && screen.matches(snapshot.pos)) {
            screen.applySnapshot(snapshot);
            return;
        }
        if (current instanceof ControllerRideScreen screen && screen.matches(snapshot.pos)) {
            screen.applySnapshot(snapshot);
            return;
        }
        if (openIfMissing) {
            minecraft.setScreen(snapshot.configMode ? new ControllerConfigScreen(snapshot)
                    : new ControllerRideScreen(snapshot));
        }
    }

    public static void openInterface(InterfaceSnapshot snapshot, boolean openIfMissing) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = minecraft.screen;
        if (current instanceof InterfaceScreen screen && screen.matches(snapshot.pos)) {
            screen.applySnapshot(snapshot);
            return;
        }
        if (openIfMissing) {
            minecraft.setScreen(new InterfaceScreen(snapshot));
        }
    }

    public static void showStatus(String json) {
        MutableComponent component;
        try {
            component = Component.Serializer.fromJson(json);
        } catch (RuntimeException e) {
            return;
        }
        if (component == null) {
            return;
        }
        if (Minecraft.getInstance().screen instanceof StatusReceiver receiver) {
            receiver.showStatus(component);
        }
    }
}
