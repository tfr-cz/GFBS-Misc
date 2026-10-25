package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class DoorClientSoundHooks {
    private static final double HEARING_RANGE_SQ = 96.0D * 96.0D;
    private static final Map<BlockPos, LoopHandle> ACTIVE = new HashMap<>();

    private DoorClientSoundHooks() {
    }

    public static void onStateChanged(DoorBlockEntity be, byte oldState, byte newState) {
        if (newState == DoorBlockEntity.STATE_OPENING || newState == DoorBlockEntity.STATE_CLOSING) {
            ensure(be);
        } else {
            stop(be);
        }
    }

    public static void tick(DoorBlockEntity be) {
        if (!be.isSoundLeader()) {
            stop(be);
            return;
        }
        if (!be.isMoving()) {
            stop(be);
            return;
        }
        if (!withinHearingRange(be)) {
            stop(be);
            return;
        }
        LoopHandle handle = ACTIVE.get(be.getBlockPos());
        if (handle == null || handle.level != be.getLevel() || handle.state != be.getState()) {
            ensure(be);
        }
    }

    private static boolean withinHearingRange(DoorBlockEntity be) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= HEARING_RANGE_SQ;
    }

    private static void ensure(DoorBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !be.isSoundLeader()) return;
        if (!withinHearingRange(be)) return;

        LoopHandle existing = ACTIVE.get(be.getBlockPos());
        if (existing != null && existing.level == level && existing.state == be.getState()) {
            return;
        }
        stop(be);

        DoorSpec spec = be.getSpec();
        boolean opening = be.getState() == DoorBlockEntity.STATE_OPENING;
        DoorLoopSound loop = create(opening ? spec.openLoop : spec.closeLoop, be, spec.soundVolume);
        DoorLoopSound loop2 = create(spec.loop2, be, spec.soundVolume);

        ACTIVE.put(be.getBlockPos(), new LoopHandle(level, be.getState(), loop, loop2));
        if (loop != null) Minecraft.getInstance().getSoundManager().play(loop);
        if (loop2 != null) Minecraft.getInstance().getSoundManager().play(loop2);
    }

    private static DoorLoopSound create(String key, DoorBlockEntity be, float volume) {
        SoundEvent event = ModSounds.get(key);
        if (event == null) return null;
        return new DoorLoopSound(event, be.getBlockPos(), volume);
    }

    public static void stop(DoorBlockEntity be) {
        LoopHandle handle = ACTIVE.remove(be.getBlockPos());
        if (handle != null) handle.stop();
    }

    public static void clearAll() {
        for (LoopHandle handle : ACTIVE.values()) handle.stop();
        ACTIVE.clear();
    }

    private record LoopHandle(Level level, byte state, DoorLoopSound loop, DoorLoopSound loop2) {
        private void stop() {
            if (loop != null) loop.stop();
            if (loop2 != null) loop2.stop();
        }
    }
}
