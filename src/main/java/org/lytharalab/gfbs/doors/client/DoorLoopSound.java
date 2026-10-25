package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
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
        this.pitch = 1F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public boolean canPlaySound() {
        return !stopped;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (stopped) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            stop();
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof DoorBlockEntity be) || be.isRemoved() || !be.isMoving()) {
            stop();
        }
    }

    public void stop() {
        stopped = true;
    }
}
