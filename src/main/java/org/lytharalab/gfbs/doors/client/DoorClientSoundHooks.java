package org.lytharalab.gfbs.doors.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

@OnlyIn(Dist.CLIENT)
public final class DoorClientSoundHooks {
    private static final double HEARING_RANGE_SQ = 9216.0;
    private static final Map<BlockPos, DoorClientSoundHooks.LoopHandle> ACTIVE = new HashMap<>();

    private DoorClientSoundHooks() {
    }

    public static void onStateChanged(DoorBlockEntity be, byte oldState, byte newState) {
        if (newState != 3 && newState != 2) {
            stop(be);
        } else {
            ensure(be);
        }
    }

    public static void tick(DoorBlockEntity be) {
        if (!be.isSoundLeader()) {
            stop(be);
        } else if (!be.isMoving()) {
            stop(be);
        } else if (!withinHearingRange(be)) {
            stop(be);
        } else {
            DoorClientSoundHooks.LoopHandle handle = (DoorClientSoundHooks.LoopHandle)ACTIVE.get(be.getBlockPos());
            if (handle == null || handle.level != be.getLevel() || handle.state != be.getState()) {
                ensure(be);
            }
        }
    }

    private static boolean withinHearingRange(DoorBlockEntity be) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? false : player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 9216.0;
    }

    private static void ensure(DoorBlockEntity be) {
        Level level = be.getLevel();
        if (level != null && be.isSoundLeader() && withinHearingRange(be)) {
            DoorClientSoundHooks.LoopHandle existing = (DoorClientSoundHooks.LoopHandle)ACTIVE.get(be.getBlockPos());
            if (existing == null || existing.level != level || existing.state != be.getState()) {
                stop(be);
                DoorSpec spec = be.getSpec();
                boolean opening = be.getState() == 3;
                DoorLoopSound loop = create(opening ? spec.openLoop : spec.closeLoop, be, spec.soundVolume);
                DoorLoopSound loop2 = create(spec.loop2, be, spec.soundVolume);
                ACTIVE.put(be.getBlockPos(), new DoorClientSoundHooks.LoopHandle(level, be.getState(), loop, loop2));
                if (loop != null) {
                    Minecraft.getInstance().getSoundManager().play(loop);
                }

                if (loop2 != null) {
                    Minecraft.getInstance().getSoundManager().play(loop2);
                }
            }
        }
    }

    private static DoorLoopSound create(String key, DoorBlockEntity be, float volume) {
        SoundEvent event = ModSounds.get(key);
        return event == null ? null : new DoorLoopSound(event, be.getBlockPos(), volume);
    }

    public static void stop(DoorBlockEntity be) {
        DoorClientSoundHooks.LoopHandle handle = (DoorClientSoundHooks.LoopHandle)ACTIVE.remove(be.getBlockPos());
        if (handle != null) {
            handle.stop();
        }
    }

    public static void clearAll() {
        for (DoorClientSoundHooks.LoopHandle handle : ACTIVE.values()) {
            handle.stop();
        }

        ACTIVE.clear();
    }

    public static void onChunkUnload(ChunkPos chunkPos) {
        if (!ACTIVE.isEmpty()) {
            int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            Iterator it = ACTIVE.entrySet().iterator();

            while (it.hasNext()) {
                Entry entry = (Entry<BlockPos, DoorClientSoundHooks.LoopHandle>)it.next();
                BlockPos pos = (BlockPos)entry.getKey();
                if (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                    ((DoorClientSoundHooks.LoopHandle)entry.getValue()).stop();
                    it.remove();
                    DoorVisualCache.invalidate(pos);
                }
            }
        }
    }

    private static record LoopHandle(Level level, byte state, DoorLoopSound loop, DoorLoopSound loop2) {
        private void stop() {
            if (this.loop != null) {
                this.loop.stop();
            }

            if (this.loop2 != null) {
                this.loop2.stop();
            }
        }
    }
}
