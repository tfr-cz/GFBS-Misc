package org.lytharalab.gfbs.doors.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DoorSpecs {
    public static final List<DoorSpec> ALL;
    public static final Map<String, DoorSpec> BY_ID;
    public static final DoorSpec TRANSITION_SEAL = DoorSpec.builder("transition_seal")
        .dims(23, 0, 0, 0, 13, 12)
        .time(480)
        .volume(6.0F)
        .model("doors/transition_seal")
        .parts("frame")
        .skins("transition_seal")
        .sounds(null, null, "door.transition_seal_open", null, null, null, null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec VAULT_DOOR = DoorSpec.builder("vault_door")
        .dims(4, 0, 0, 0, 2, 2)
        .extraDims(new int[]{0, 0, 1, -1, 2, 2})
        .time(120)
        .model("doors/vault_door")
        .parts("Frame", "Door", "Label")
        .skins("vault_door_3", "vault_door_3", "vault_door_3", "vault_door_4", "vault_door_4", "vault_door_s", "vault_door_s")
        .labels("label_101", "label_87", "label_106", "label_81", "label_111", "label_2", "label_99")
        .volume(1.0F)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec SLIDING_BLAST_DOOR = DoorSpec.builder("sliding_blast_door")
        .dims(3, 0, 0, 0, 3, 3)
        .time(24)
        .model("doors/blast_door")
        .parts("Frame", "LeftDoor", "RightLock", "RightDoor", "LeftLock")
        .skins("blast_door", "blast_door_industrial")
        .volume(2.0F)
        .sounds("door.sliding_door_opening", "door.sliding_door_opening", null, null, "door.sliding_door_opened", "door.sliding_door_shut", null)
        .strength(10.0F, 750.0F)
        .build();
    public static final DoorSpec FIRE_DOOR = DoorSpec.builder("fire_door")
        .dims(2, 0, 0, 0, 2, 1)
        .time(160)
        .model("doors/fire_door")
        .parts("Frame", "Door")
        .skins("fire_door", "fire_door_black", "fire_door_orange", "fire_door_yellow", "fire_door_trefoil")
        .volume(2.0F)
        .sounds("door.wgh_start", "door.wgh_start", null, null, "door.wgh_stop", "door.wgh_stop", "door.alarm6")
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec SILO_HATCH = DoorSpec.builder("silo_hatch")
        .dims(0, 0, 2, 2, 2, 2)
        .offset(2)
        .time(60)
        .model("doors/silo_hatch")
        .parts("Frame", "Hatch")
        .skins("silo_hatch")
        .volume(2.0F)
        .sounds("door.wgh_big_start", "door.wgh_big_start", null, null, "door.wgh_big_stop", "door.wgh_big_stop", null)
        .strength(10.0F, 100.0F)
        .build();
    public static final DoorSpec SILO_HATCH_LARGE = DoorSpec.builder("silo_hatch_large")
        .dims(0, 0, 3, 3, 3, 3)
        .offset(3)
        .time(60)
        .model("doors/silo_hatch_large")
        .parts("Frame", "Hatch")
        .skins("silo_hatch_large")
        .volume(2.0F)
        .sounds("door.wgh_big_start", "door.wgh_big_start", null, null, "door.wgh_big_stop", "door.wgh_big_stop", null)
        .strength(10.0F, 100.0F)
        .build();
    public static final DoorSpec SECURE_ACCESS_DOOR = DoorSpec.builder("secure_access_door")
        .dims(4, 0, 0, 0, 2, 2)
        .time(120)
        .model("doors/secure_door")
        .parts("Frame", "Door")
        .skins("secure_door", "secure_door_grey", "secure_door_black", "secure_door_yellow")
        .volume(2.0F)
        .sounds("door.garage_move", "door.garage_move", null, null, "door.garage_stop", "door.garage_stop", null)
        .strength(20.0F, 2000.0F)
        .build();
    public static final DoorSpec LARGE_VEHICLE_DOOR = DoorSpec.builder("large_vehicle_door")
        .dims(5, 0, 0, 0, 3, 3)
        .time(60)
        .model("doors/vehicle_door")
        .parts("Frame", "Left", "Right")
        .skins("vehicle_door", "vehicle_door_black", "vehicle_door_blue", "vehicle_door_red")
        .volume(2.0F)
        .sounds("door.garage_move", "door.garage_move", null, null, "door.garage_stop", "door.garage_stop", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec QE_CONTAINMENT = DoorSpec.builder("qe_containment")
        .dims(2, 0, 0, 0, 1, 1)
        .time(160)
        .model("doors/containment_door")
        .parts("Frame", "Door")
        .skins("containment_door", "containment_door_trefoil", "containment_door_trefoil_yellow")
        .volume(2.0F)
        .sounds("door.wgh_start", "door.wgh_start", null, null, "door.wgh_stop", "door.wgh_stop", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec QE_SLIDING_DOOR = DoorSpec.builder("qe_sliding_door")
        .dims(1, 0, 0, 0, 1, 0)
        .time(10)
        .model("doors/sliding_door")
        .parts("Frame", "Left", "Right")
        .skins("sliding_door", "sliding_door_black", "sliding_door_grid", "sliding_door_plate", "sliding_door_white")
        .volume(2.0F)
        .sounds("door.qe_sliding_opening", "door.qe_sliding_opening", null, null, "door.qe_sliding_opened", "door.qe_sliding_shut", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec ROUND_AIRLOCK_DOOR = DoorSpec.builder("round_airlock_door")
        .dims(3, 0, 0, 0, 2, 1)
        .time(60)
        .model("doors/airlock_door")
        .parts("Frame", "Left", "Right")
        .skins("airlock_door", "airlock_door_clean", "airlock_door_green")
        .volume(2.0F)
        .sounds("door.garage_move", "door.garage_move", null, null, "door.garage_stop", "door.garage_stop", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec SLIDING_SEAL_DOOR = DoorSpec.builder("sliding_seal_door")
        .dims(1, 0, 0, 0, 0, 0)
        .time(20)
        .model("doors/seal_door")
        .parts("Frame", "Door")
        .skins("seal_door", "seal_door_clean", "seal_door_trefoil")
        .volume(2.0F)
        .sounds(null, null, "door.sliding_seal_open", null, "door.sliding_seal_stop", "door.sliding_seal_stop", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec WATER_DOOR = DoorSpec.builder("water_door")
        .dims(2, 0, 0, 0, 1, 1)
        .time(60)
        .model("doors/water_door")
        .parts("Frame", "Door_Cube.003", "Bolts", "Top", "Bottom")
        .skins("water_door", "water_door_clean")
        .volume(2.0F)
        .sounds("door.wgh_big_start", "door.wgh_big_start", "door.lever", null, "door.wgh_big_stop", "door.lever", null)
        .strength(5.0F, 50.0F)
        .build();
    public static final DoorSpec REACTOR_DOOR = DoorSpec.builder("reactor_door")
        .dims(2, 0, 1, 0, 1, 1)
        .time(109)
        .model("doors/reactor_door")
        .parts("FrameA", "FrameB", "Wall", "Door", "Handle")
        .skins(
            "reactordoor",
            "reactordoor_trefoilless",
            "reactordoor_yellow",
            "reactordoor_gray",
            "reactordoor_gray_trefoilless",
            "reactordoor_but_grayer",
            "reactordoor_geen_trefoilless"
        )
        .partTexture("FrameA", "reactordoor_frame_a")
        .partTexture("FrameB", "reactordoor_frame_b")
        .volume(3.0F)
        .sounds(null, null, null, null, null, null, null)
        .strength(150.0F, 13500.0F)
        .build();
    public static final DoorSpec BLAST_DOOR = DoorSpec.builder("blast_door")
        .dims(6, 0, 0, 0, 0, 0)
        .time(100)
        .model("doors/blast_door_legacy")
        .parts("Base", "Block", "Tooth", "Slider")
        .skins("blast_door_base")
        .partTexture("Base", "blast_door_base")
        .partTexture("Block", "blast_door_block")
        .partTexture("Tooth", "blast_door_tooth")
        .partTexture("Slider", "blast_door_slider")
        .volume(0.5F)
        .sounds(null, null, "block.reactor_start", "block.reactor_start", "block.reactor_stop", "block.reactor_stop", null)
        .strength(10.0F, 1000.0F)
        .build();
    public static final DoorSpec CARGO_DOOR = DoorSpec.builder("cargo_door")
        .dims(2, 0, 0, 0, 1, 1)
        .time(60)
        .model("doors/cargo_door")
        .parts("Frame", "DoorTop", "DoorBot")
        .skins("cargo_door")
        .volume(2.0F)
        .sounds("door.garage_move", "door.garage_move", null, null, "door.garage_stop", "door.garage_stop", null)
        .strength(5.0F, 50.0F)
        .build();
    public static final DoorSpec VAULT_DOOR_LARGE = large(VAULT_DOOR, 1, 1);
    public static final DoorSpec LARGE_VEHICLE_DOOR_LARGE = large(LARGE_VEHICLE_DOOR, 1, 1);
    public static final DoorSpec FIRE_DOOR_LARGE = large(FIRE_DOOR, 2, 1);
    public static final DoorSpec SLIDING_BLAST_DOOR_LARGE = large(SLIDING_BLAST_DOOR, 1, 1);
    public static final DoorSpec ROUND_AIRLOCK_DOOR_LARGE = large(ROUND_AIRLOCK_DOOR, 1, 1);
    public static final DoorSpec QE_CONTAINMENT_LARGE = large(QE_CONTAINMENT, 1, 1);
    public static final DoorSpec WATER_DOOR_LARGE = large(WATER_DOOR, 1, 1);
    public static final DoorSpec SECURE_ACCESS_DOOR_LARGE = large(SECURE_ACCESS_DOOR, 1, 1);

    private static DoorSpec large(DoorSpec base, int widthIncrease, int heightIncrease) {
        int[] d = base.dimensions;
        int west = d[4] + widthIncrease / 2;
        int east = d[5] + widthIncrease - widthIncrease / 2;
        int up = d[0] + heightIncrease;
        int down = d[1];
        int oldWidth = d[4] + d[5] + 1;
        int newWidth = west + east + 1;
        int oldHeight = d[0] + d[1] + 1;
        int newHeight = up + down + 1;
        int[][] extra = new int[base.extraDimensions.length][];

        for (int i = 0; i < extra.length; i++) {
            int[] e = base.extraDimensions[i];
            extra[i] = new int[]{e[0] + heightIncrease, e[1], e[2], e[3], e[4] + widthIncrease / 2, e[5] + widthIncrease - widthIncrease / 2};
        }

        DoorSpec.Builder builder = DoorSpec.builder(base.id + "_large")
            .dims(up, down, d[2], d[3], west, east)
            .offset(base.offset)
            .time(base.timeToOpen)
            .model(base.model)
            .parts(base.parts)
            .skins(base.skinTextures)
            .volume(base.soundVolume)
            .sounds(base.openLoop, base.closeLoop, base.openStart, base.closeStart, base.openEnd, base.closeEnd, base.loop2)
            .strength(base.hardness, base.resistance)
            .scaledFrom(base.id, (float)newWidth / (float)oldWidth, (float)newHeight / (float)oldHeight);
        if (base.labelTextures != null) {
            builder.labels(base.labelTextures);
        }

        base.partTextures.forEach(builder::partTexture);
        if (extra.length > 0) {
            builder.extraDims(extra);
        }

        return builder.build();
    }

    private DoorSpecs() {
    }

    static {
        Map<String, DoorSpec> map = new LinkedHashMap<>();

        for (DoorSpec spec : new DoorSpec[]{
            TRANSITION_SEAL,
            VAULT_DOOR,
            SLIDING_BLAST_DOOR,
            FIRE_DOOR,
            SILO_HATCH,
            SILO_HATCH_LARGE,
            SECURE_ACCESS_DOOR,
            LARGE_VEHICLE_DOOR,
            QE_CONTAINMENT,
            QE_SLIDING_DOOR,
            ROUND_AIRLOCK_DOOR,
            SLIDING_SEAL_DOOR,
            WATER_DOOR,
            REACTOR_DOOR,
            CARGO_DOOR,
            BLAST_DOOR,
            VAULT_DOOR_LARGE,
            LARGE_VEHICLE_DOOR_LARGE,
            FIRE_DOOR_LARGE,
            SLIDING_BLAST_DOOR_LARGE,
            ROUND_AIRLOCK_DOOR_LARGE,
            QE_CONTAINMENT_LARGE,
            WATER_DOOR_LARGE,
            SECURE_ACCESS_DOOR_LARGE
        }) {
            map.put(spec.id, spec);
        }

        BY_ID = Collections.unmodifiableMap(map);
        ALL = List.copyOf(map.values());
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
        public static final String REACTOR_DOOR_OPEN = "door.reactor_door_open";
        public static final String REACTOR_DOOR_HANDLE = "door.reactor_door_handle";
        public static final String REACTOR_DOOR_CLOSE = "door.reactor_door_close";

        private SoundKeys() {
        }
    }
}
