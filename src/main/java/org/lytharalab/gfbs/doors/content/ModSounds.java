package org.lytharalab.gfbs.doors.content;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private static final DeferredRegister SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "gfbs_doors");
    private static final Map BY_KEY = new LinkedHashMap();
    public static final RegistryObject TRANSITION_SEAL_OPEN = add("door.transition_seal_open");
    public static final RegistryObject ALARM6 = add("door.alarm6");
    public static final RegistryObject LEVER = add("door.lever");
    public static final RegistryObject SLIDING_DOOR_OPENING = add("door.sliding_door_opening");
    public static final RegistryObject SLIDING_DOOR_OPENED = add("door.sliding_door_opened");
    public static final RegistryObject SLIDING_DOOR_SHUT = add("door.sliding_door_shut");
    public static final RegistryObject SLIDING_SEAL_OPEN = add("door.sliding_seal_open");
    public static final RegistryObject SLIDING_SEAL_STOP = add("door.sliding_seal_stop");
    public static final RegistryObject GARAGE_MOVE = add("door.garage_move");
    public static final RegistryObject GARAGE_STOP = add("door.garage_stop");
    public static final RegistryObject QE_SLIDING_OPENING = add("door.qe_sliding_opening");
    public static final RegistryObject QE_SLIDING_OPENED = add("door.qe_sliding_opened");
    public static final RegistryObject QE_SLIDING_SHUT = add("door.qe_sliding_shut");
    public static final RegistryObject WGH_START = add("door.wgh_start");
    public static final RegistryObject WGH_STOP = add("door.wgh_stop");
    public static final RegistryObject WGH_BIG_START = add("door.wgh_big_start");
    public static final RegistryObject WGH_BIG_STOP = add("door.wgh_big_stop");
    public static final RegistryObject REACTOR_START = add("block.reactor_start");
    public static final RegistryObject REACTOR_STOP = add("block.reactor_stop");
    public static final RegistryObject VAULT_SCRAPE = add("block.vault_scrape_new");
    public static final RegistryObject VAULT_THUD = add("block.vault_thud_new");
    public static final RegistryObject REACTOR_DOOR_OPEN = add("door.reactor_door_open");
    public static final RegistryObject REACTOR_DOOR_HANDLE = add("door.reactor_door_handle");
    public static final RegistryObject REACTOR_DOOR_CLOSE = add("door.reactor_door_close");

    private ModSounds() {
    }

    private static RegistryObject add(String key) {
        RegistryObject sound = SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("gfbs_doors", key)));
        BY_KEY.put(key, sound);
        return sound;
    }

    public static SoundEvent get(String key) {
        if (key == null) {
            return null;
        } else {
            RegistryObject sound = (RegistryObject<SoundEvent>)BY_KEY.get(key);
            return sound == null ? null : (SoundEvent)sound.get();
        }
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
