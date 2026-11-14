package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

@OnlyIn(Dist.CLIENT)
public final class DoorLoopSound extends AbstractSoundInstance implements TickableSoundInstance {
    private final BlockPos pos;
    private boolean stopped;

    public DoorLoopSound(SoundEvent event, BlockPos pos, float volume) {
        super(event, SoundSource.BLOCKS, RandomSource.create());
        this.pos = pos.immutable();
        this.looping = true;
        this.volume = volume;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.x = (double)pos.getX() + 0.5;
        this.y = (double)pos.getY() + 0.5;
        this.z = (double)pos.getZ() + 0.5;
    }

    public boolean isRelative() {
        return false;
    }

    public boolean canPlaySound() {
        return !this.stopped;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public boolean canStartSilent() {
        return true;
    }

    public void tick() {
        if (!this.stopped) {
            Level level = Minecraft.getInstance().level;
            if (level == null) {
                this.stop();
            } else if (!(level.getBlockEntity(this.pos) instanceof DoorBlockEntity be) || be.isRemoved() || !be.isMoving()) {
                this.stop();
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void stop() {
        this.stopped = true;
    }
}
