package org.lytharalab.gfbs.doors.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DoorSpecs {
    public static final List<DoorSpec> ALL;
    public static final Map<String, DoorSpec> BY_ID;

    public static final DoorSpec TRANSITION_SEAL = DoorSpec.builder("transition_seal")
            .dims(23, 0, 0, 0, 13, 12).time(480).volume(6F)
            .model("doors/transition_seal").parts("frame")
            .skins("transition_seal")
            .sounds(null, null, SoundKeys.TRANSITION_SEAL_OPEN, null, null, null, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec VAULT_DOOR = DoorSpec.builder("vault_door")
            .dims(4, 0, 0, 0, 2, 2).extraDims(new int[]{0, 0, 1, -1, 2, 2}).time(120)
            .model("doors/vault_door").parts("Frame", "Door", "Label")
            .skins("vault_door_3", "vault_door_3", "vault_door_3", "vault_door_4", "vault_door_4", "vault_door_s", "vault_door_s")
            .labels("label_101", "label_87", "label_106", "label_81", "label_111", "label_2", "label_99")
            .volume(1F)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec SLIDING_BLAST_DOOR = DoorSpec.builder("sliding_blast_door")
            .dims(3, 0, 0, 0, 3, 3).time(24)
            .model("doors/blast_door").parts("Frame", "LeftDoor", "RightLock", "RightDoor", "LeftLock")
            .skins("blast_door", "blast_door_industrial")
            .volume(2F)
            .sounds(SoundKeys.SLIDING_DOOR_OPENING, SoundKeys.SLIDING_DOOR_OPENING,
                    null, null, SoundKeys.SLIDING_DOOR_OPENED, SoundKeys.SLIDING_DOOR_SHUT, null)
            .strength(10F, 750F)
            .build();

    public static final DoorSpec FIRE_DOOR = DoorSpec.builder("fire_door")
            .dims(2, 0, 0, 0, 2, 1).time(160)
            .model("doors/fire_door").parts("Frame", "Door")
            .skins("fire_door", "fire_door_black", "fire_door_orange", "fire_door_yellow", "fire_door_trefoil")
            .volume(2F)
            .sounds(SoundKeys.WGH_START, SoundKeys.WGH_START, null, null, SoundKeys.WGH_STOP, SoundKeys.WGH_STOP, SoundKeys.ALARM6)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec SILO_HATCH = DoorSpec.builder("silo_hatch")
            .dims(0, 0, 2, 2, 2, 2).offset(2).time(60)
            .model("doors/silo_hatch").parts("Frame", "Hatch")
            .skins("silo_hatch")
            .volume(2F)
            .sounds(SoundKeys.WGH_BIG_START, SoundKeys.WGH_BIG_START, null, null, SoundKeys.WGH_BIG_STOP, SoundKeys.WGH_BIG_STOP, null)
            .strength(10F, 100F)
            .build();

    public static final DoorSpec SILO_HATCH_LARGE = DoorSpec.builder("silo_hatch_large")
            .dims(0, 0, 3, 3, 3, 3).offset(3).time(60)
            .model("doors/silo_hatch_large").parts("Frame", "Hatch")
            .skins("silo_hatch_large")
            .volume(2F)
            .sounds(SoundKeys.WGH_BIG_START, SoundKeys.WGH_BIG_START, null, null, SoundKeys.WGH_BIG_STOP, SoundKeys.WGH_BIG_STOP, null)
            .strength(10F, 100F)
            .build();

    public static final DoorSpec SECURE_ACCESS_DOOR = DoorSpec.builder("secure_access_door")
            .dims(4, 0, 0, 0, 2, 2).time(120)
            .model("doors/secure_door").parts("Frame", "Door")
            .skins("secure_door", "secure_door_grey", "secure_door_black", "secure_door_yellow")
            .volume(2F)
            .sounds(SoundKeys.GARAGE_MOVE, SoundKeys.GARAGE_MOVE, null, null, SoundKeys.GARAGE_STOP, SoundKeys.GARAGE_STOP, null)
            .strength(20F, 2000F)
            .build();

    public static final DoorSpec LARGE_VEHICLE_DOOR = DoorSpec.builder("large_vehicle_door")
            .dims(5, 0, 0, 0, 3, 3).time(60)
            .model("doors/vehicle_door").parts("Frame", "Left", "Right")
            .skins("vehicle_door", "vehicle_door_black", "vehicle_door_blue", "vehicle_door_red")
            .volume(2F)
            .sounds(SoundKeys.GARAGE_MOVE, SoundKeys.GARAGE_MOVE, null, null, SoundKeys.GARAGE_STOP, SoundKeys.GARAGE_STOP, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec QE_CONTAINMENT = DoorSpec.builder("qe_containment")
            .dims(2, 0, 0, 0, 1, 1).time(160)
            .model("doors/containment_door").parts("Frame", "Door")
            .skins("containment_door", "containment_door_trefoil", "containment_door_trefoil_yellow")
            .volume(2F)
            .sounds(SoundKeys.WGH_START, SoundKeys.WGH_START, null, null, SoundKeys.WGH_STOP, SoundKeys.WGH_STOP, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec QE_SLIDING_DOOR = DoorSpec.builder("qe_sliding_door")
            .dims(1, 0, 0, 0, 1, 0).time(10)
            .model("doors/sliding_door").parts("Frame", "Left", "Right")
            .skins("sliding_door", "sliding_door_black", "sliding_door_grid", "sliding_door_plate", "sliding_door_white")
            .volume(2F)
            .sounds(SoundKeys.QE_SLIDING_OPENING, SoundKeys.QE_SLIDING_OPENING,
                    null, null, SoundKeys.QE_SLIDING_OPENED, SoundKeys.QE_SLIDING_SHUT, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec ROUND_AIRLOCK_DOOR = DoorSpec.builder("round_airlock_door")
            .dims(3, 0, 0, 0, 2, 1).time(60)
            .model("doors/airlock_door").parts("Frame", "Left", "Right")
            .skins("airlock_door", "airlock_door_clean", "airlock_door_green")
            .volume(2F)
            .sounds(SoundKeys.GARAGE_MOVE, SoundKeys.GARAGE_MOVE, null, null, SoundKeys.GARAGE_STOP, SoundKeys.GARAGE_STOP, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec SLIDING_SEAL_DOOR = DoorSpec.builder("sliding_seal_door")
            .dims(1, 0, 0, 0, 0, 0).time(20)
            .model("doors/seal_door").parts("Frame", "Door")
            .skins("seal_door", "seal_door_clean", "seal_door_trefoil")
            .volume(2F)
            .sounds(null, null, SoundKeys.SLIDING_SEAL_OPEN, null, SoundKeys.SLIDING_SEAL_STOP, SoundKeys.SLIDING_SEAL_STOP, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec WATER_DOOR = DoorSpec.builder("water_door")
            .dims(2, 0, 0, 0, 1, 1).time(60)
            .model("doors/water_door").parts("Frame", "Door_Cube.003", "Bolts", "Top", "Bottom")
            .skins("water_door", "water_door_clean")
            .volume(2F)
            .sounds(SoundKeys.WGH_BIG_START, SoundKeys.WGH_BIG_START, SoundKeys.LEVER, null, SoundKeys.WGH_BIG_STOP, SoundKeys.LEVER, null)
            .strength(5F, 50F)
            .build();

    public static final DoorSpec BLAST_DOOR = DoorSpec.builder("blast_door")
            .dims(6, 0, 0, 0, 0, 0).time(100)
            .model("doors/blast_door_legacy")
            .parts("Base", "Block", "Tooth", "Slider")
            .skins("blast_door_base")
            .partTexture("Base", "blast_door_base")
            .partTexture("Block", "blast_door_block")
            .partTexture("Tooth", "blast_door_tooth")
            .partTexture("Slider", "blast_door_slider")
            .volume(0.5F)
            .sounds(null, null, SoundKeys.REACTOR_START, SoundKeys.REACTOR_START,
                    SoundKeys.REACTOR_STOP, SoundKeys.REACTOR_STOP, null)
            .strength(10F, 1000F)
            .build();

    public static final DoorSpec CARGO_DOOR = DoorSpec.builder("cargo_door")
            .dims(2, 0, 0, 0, 1, 1).time(60)
            .model("doors/cargo_door").parts("Frame", "DoorTop", "DoorBot")
            .skins("cargo_door")
            .volume(2F)
            .sounds(SoundKeys.GARAGE_MOVE, SoundKeys.GARAGE_MOVE, null, null, SoundKeys.GARAGE_STOP, SoundKeys.GARAGE_STOP, null)
            .strength(5F, 50F)
            .build();

    static {
        Map<String, DoorSpec> map = new LinkedHashMap<>();
        for (DoorSpec spec : new DoorSpec[]{TRANSITION_SEAL, VAULT_DOOR, SLIDING_BLAST_DOOR, FIRE_DOOR, SILO_HATCH,
                SILO_HATCH_LARGE, SECURE_ACCESS_DOOR, LARGE_VEHICLE_DOOR, QE_CONTAINMENT, QE_SLIDING_DOOR,
                ROUND_AIRLOCK_DOOR, SLIDING_SEAL_DOOR, WATER_DOOR, CARGO_DOOR, BLAST_DOOR}) {
            map.put(spec.id, spec);
        }
        BY_ID = Collections.unmodifiableMap(map);
        ALL = List.copyOf(map.values());
    }

    private DoorSpecs() {
    }

    public static final class SoundKeys {
        public static final String TRANSITION_SEAL_OPEN = "door.transition_seal_open";
        public static final String ALARM6 = "door.alarm6";
        public static final String LEVER = "door.lever";
        public static final String SLIDING_DOOR_OPENING = "door.sliding_door_opening";
        public static final String SLIDING_DOOR_OPENED = "door.sliding_door_opened";
        public static final String SLIDING_DOOR_SHUT = "door.sliding_door_shut";
        public static final String SLIDING_SEAL_OPEN = "door.sliding_seal_open";
        public static final String SLIDING_SEAL_STOP = "door.sliding_seal_stop";
        public static final String GARAGE_MOVE = "door.garage_move";
        public static final String GARAGE_STOP = "door.garage_stop";
        public static final String QE_SLIDING_OPENING = "door.qe_sliding_opening";
        public static final String QE_SLIDING_OPENED = "door.qe_sliding_opened";
        public static final String QE_SLIDING_SHUT = "door.qe_sliding_shut";
        public static final String WGH_START = "door.wgh_start";
        public static final String WGH_STOP = "door.wgh_stop";
        public static final String WGH_BIG_START = "door.wgh_big_start";
        public static final String WGH_BIG_STOP = "door.wgh_big_stop";
        public static final String REACTOR_START = "block.reactor_start";
        public static final String REACTOR_STOP = "block.reactor_stop";
        public static final String VAULT_SCRAPE = "block.vault_scrape_new";
        public static final String VAULT_THUD = "block.vault_thud_new";

        private SoundKeys() {
        }
    }
}
