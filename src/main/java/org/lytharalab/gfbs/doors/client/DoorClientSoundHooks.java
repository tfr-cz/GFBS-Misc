package org.lytharalab.gfbs.doors.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the looping door sounds.
 *
 * <p>Prefers {@code gfbs_auralis} when it is installed and falls back to a vanilla {@link DoorLoopSound}
 * otherwise. The two paths differ in who applies the volume sliders: vanilla mixes a {@code BLOCKS} sound itself,
 * Auralis does not, so the Auralis path bakes {@code BLOCKS * MASTER} into the volume and re-applies it when the
 * player moves the slider.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DoorClientSoundHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors");
    private static final double HEARING_RANGE_SQ = 9216.0;
    private static final float AURALIS_MIN_DISTANCE = 4.0F;
    private static final float AURALIS_MAX_DISTANCE = 96.0F;
    private static final int AURALIS_PRIORITY = 60;

    private static final Map<BlockPos, LoopHandle> ACTIVE = new HashMap<>();
    private static float lastAuralisMultiplier = Float.NaN;

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
        refreshAuralisVolumeIfChanged();
        if (!be.isSoundLeader()) {
            stop(be);
        } else if (!be.isMoving()) {
            stop(be);
        } else if (!withinHearingRange(be)) {
            stop(be);
        } else {
            LoopHandle handle = ACTIVE.get(be.getBlockPos());
            if (handle != null && handle.level == be.getLevel() && handle.state == be.getState()) {
                reviveVanillaIfSilenced(be, handle);
            } else {
                ensure(be);
            }
        }
    }

    /**
     * A vanilla loop is silent while the player has the {@code BLOCKS} slider at zero and does not come back on its
     * own, so remember that and re-queue it once the slider returns.
     */
    private static void reviveVanillaIfSilenced(DoorBlockEntity be, LoopHandle handle) {
        if (handle.isAuralis()) {
            return;
        }
        float blocks = DoorSoundVolume.blocksMultiplier();
        if (blocks <= 0.0F) {
            handle.markSilenced();
            return;
        }
        if (!handle.isSilenced()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        SoundManager soundManager = mc == null ? null : mc.getSoundManager();
        if (soundManager == null) {
            return;
        }

        handle.stopVanilla(soundManager);
        DoorSpec spec = be.getSpec();
        boolean opening = be.getState() == 3;
        DoorLoopSound loop = create(opening ? spec.openLoop : spec.closeLoop, be, handle.base);
        DoorLoopSound loop2 = create(spec.loop2, be, handle.base);
        ACTIVE.put(
            be.getBlockPos(),
            new LoopHandle(handle.level, be.getState(), handle.base, loop, loop2, null, null)
        );
        if (loop != null) {
            soundManager.play(loop);
        }
        if (loop2 != null) {
            soundManager.play(loop2);
        }
        LOGGER.debug("门循环音效在方块音量恢复后自愈重播：base={} blocks={}", handle.base, blocks);
    }

    private static void refreshAuralisVolumeIfChanged() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        float current = DoorSoundVolume.auralisMultiplier();
        if (current == lastAuralisMultiplier) {
            return;
        }
        lastAuralisMultiplier = current;
        for (LoopHandle handle : ACTIVE.values()) {
            handle.applyAuralisVolume(current);
        }
    }

    private static boolean withinHearingRange(DoorBlockEntity be) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= HEARING_RANGE_SQ;
    }

    private static void ensure(DoorBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || !be.isSoundLeader() || !withinHearingRange(be)) {
            return;
        }
        LoopHandle existing = ACTIVE.get(be.getBlockPos());
        if (existing != null && existing.level == level && existing.state == be.getState()) {
            return;
        }

        stop(be);
        DoorSpec spec = be.getSpec();
        boolean opening = be.getState() == 3;
        String key1 = opening ? spec.openLoop : spec.closeLoop;
        String key2 = spec.loop2;
        float base = DoorSoundVolume.baseVolume(spec);
        float auralisMultiplier = DoorSoundVolume.auralisMultiplier();
        lastAuralisMultiplier = auralisMultiplier;

        AuralisBackend.Handle auralis = playAuralis(key1, be, base * auralisMultiplier);
        AuralisBackend.Handle auralis2 = playAuralis(key2, be, base * auralisMultiplier);
        if (auralis != null || auralis2 != null) {
            ACTIVE.put(be.getBlockPos(), new LoopHandle(level, be.getState(), base, null, null, auralis, auralis2));
            return;
        }

        DoorLoopSound loop = create(key1, be, base);
        DoorLoopSound loop2 = create(key2, be, base);
        ACTIVE.put(be.getBlockPos(), new LoopHandle(level, be.getState(), base, loop, loop2, null, null));
        if (loop != null) {
            Minecraft.getInstance().getSoundManager().play(loop);
        }
        if (loop2 != null) {
            Minecraft.getInstance().getSoundManager().play(loop2);
        }
    }

    private static AuralisBackend.Handle playAuralis(String key, DoorBlockEntity be, float volume) {
        if (key == null) {
            return null;
        }
        SoundEvent event = ModSounds.get(key);
        return event == null
            ? null
            : AuralisBackend.playLoop(event, be.getBlockPos(), volume, AURALIS_MIN_DISTANCE, AURALIS_MAX_DISTANCE, AURALIS_PRIORITY);
    }

    private static DoorLoopSound create(String key, DoorBlockEntity be, float volume) {
        SoundEvent event = ModSounds.get(key);
        return event == null ? null : new DoorLoopSound(event, be.getBlockPos(), volume);
    }

    public static void stop(DoorBlockEntity be) {
        LoopHandle handle = ACTIVE.remove(be.getBlockPos());
        if (handle != null) {
            handle.stop();
        }
    }

    public static void clearAll() {
        for (LoopHandle handle : ACTIVE.values()) {
            handle.stop();
        }
        ACTIVE.clear();
    }

    public static void onChunkUnload(ChunkPos chunkPos) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        Iterator<Entry<BlockPos, LoopHandle>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Entry<BlockPos, LoopHandle> entry = it.next();
            BlockPos pos = entry.getKey();
            if (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                entry.getValue().stop();
                it.remove();
                DoorVisualCache.invalidate(pos);
            }
        }
    }

    private static final class LoopHandle {
        private final Level level;
        private final byte state;
        private final float base;
        private final DoorLoopSound loop;
        private final DoorLoopSound loop2;
        private final AuralisBackend.Handle auralis;
        private final AuralisBackend.Handle auralis2;
        private boolean silenced;

        private LoopHandle(Level level, byte state, float base, DoorLoopSound loop, DoorLoopSound loop2,
                           AuralisBackend.Handle auralis, AuralisBackend.Handle auralis2) {
            this.level = level;
            this.state = state;
            this.base = base;
            this.loop = loop;
            this.loop2 = loop2;
            this.auralis = auralis;
            this.auralis2 = auralis2;
        }

        private boolean isAuralis() {
            return this.auralis != null || this.auralis2 != null;
        }

        private boolean isSilenced() {
            return this.silenced;
        }

        private void markSilenced() {
            this.silenced = true;
        }

        private void stopVanilla(SoundManager soundManager) {
            if (this.loop != null) {
                soundManager.stop(this.loop);
                this.loop.stop();
            }
            if (this.loop2 != null) {
                soundManager.stop(this.loop2);
                this.loop2.stop();
            }
        }

        private void applyAuralisVolume(float auralisMultiplier) {
            float volume = this.base * auralisMultiplier;
            if (this.auralis != null) {
                this.auralis.setVolume(volume);
            }
            if (this.auralis2 != null) {
                this.auralis2.setVolume(volume);
            }
        }

        private void stop() {
            if (this.loop != null) {
                this.loop.stop();
            }
            if (this.loop2 != null) {
                this.loop2.stop();
            }
            if (this.auralis != null) {
                this.auralis.stop();
            }
            if (this.auralis2 != null) {
                this.auralis2.stop();
            }
        }
    }
}
