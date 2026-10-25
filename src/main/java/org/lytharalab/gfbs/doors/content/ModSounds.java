package org.lytharalab.gfbs.doors.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.gfbs.doors.GFBSDoors;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, GFBSDoors.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> BY_KEY = new LinkedHashMap<>();

    public static final RegistryObject<SoundEvent> TRANSITION_SEAL_OPEN = add(DoorSpecs.SoundKeys.TRANSITION_SEAL_OPEN);
    public static final RegistryObject<SoundEvent> ALARM6 = add(DoorSpecs.SoundKeys.ALARM6);
    public static final RegistryObject<SoundEvent> LEVER = add(DoorSpecs.SoundKeys.LEVER);
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENING = add(DoorSpecs.SoundKeys.SLIDING_DOOR_OPENING);
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENED = add(DoorSpecs.SoundKeys.SLIDING_DOOR_OPENED);
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_SHUT = add(DoorSpecs.SoundKeys.SLIDING_DOOR_SHUT);
    public static final RegistryObject<SoundEvent> SLIDING_SEAL_OPEN = add(DoorSpecs.SoundKeys.SLIDING_SEAL_OPEN);
    public static final RegistryObject<SoundEvent> SLIDING_SEAL_STOP = add(DoorSpecs.SoundKeys.SLIDING_SEAL_STOP);
    public static final RegistryObject<SoundEvent> GARAGE_MOVE = add(DoorSpecs.SoundKeys.GARAGE_MOVE);
    public static final RegistryObject<SoundEvent> GARAGE_STOP = add(DoorSpecs.SoundKeys.GARAGE_STOP);
    public static final RegistryObject<SoundEvent> QE_SLIDING_OPENING = add(DoorSpecs.SoundKeys.QE_SLIDING_OPENING);
    public static final RegistryObject<SoundEvent> QE_SLIDING_OPENED = add(DoorSpecs.SoundKeys.QE_SLIDING_OPENED);
    public static final RegistryObject<SoundEvent> QE_SLIDING_SHUT = add(DoorSpecs.SoundKeys.QE_SLIDING_SHUT);
    public static final RegistryObject<SoundEvent> WGH_START = add(DoorSpecs.SoundKeys.WGH_START);
    public static final RegistryObject<SoundEvent> WGH_STOP = add(DoorSpecs.SoundKeys.WGH_STOP);
    public static final RegistryObject<SoundEvent> WGH_BIG_START = add(DoorSpecs.SoundKeys.WGH_BIG_START);
    public static final RegistryObject<SoundEvent> WGH_BIG_STOP = add(DoorSpecs.SoundKeys.WGH_BIG_STOP);
    public static final RegistryObject<SoundEvent> REACTOR_START = add(DoorSpecs.SoundKeys.REACTOR_START);
    public static final RegistryObject<SoundEvent> REACTOR_STOP = add(DoorSpecs.SoundKeys.REACTOR_STOP);
    public static final RegistryObject<SoundEvent> VAULT_SCRAPE = add(DoorSpecs.SoundKeys.VAULT_SCRAPE);
    public static final RegistryObject<SoundEvent> VAULT_THUD = add(DoorSpecs.SoundKeys.VAULT_THUD);

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> add(String key) {
        RegistryObject<SoundEvent> sound = SOUNDS.register(key,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GFBSDoors.MODID, key)));
        BY_KEY.put(key, sound);
        return sound;
    }

    public static SoundEvent get(String key) {
        if (key == null) return null;
        RegistryObject<SoundEvent> sound = BY_KEY.get(key);
        return sound == null ? null : sound.get();
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
