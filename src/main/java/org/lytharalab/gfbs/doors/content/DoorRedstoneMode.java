package org.lytharalab.gfbs.doors.content;

public enum DoorRedstoneMode {
    NONE("none"),
    PULSE("pulse"),
    TRIGGER("trigger"),
    INVERTED("inverted"),
    GFBS_NORMALLY_CLOSED("gfbs_closed"),
    GFBS_NORMALLY_OPEN("gfbs_open");

    public final String id;

    DoorRedstoneMode(String id) {
        this.id = id;
    }

    public static DoorRedstoneMode byId(String id) {
        for (DoorRedstoneMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }

    public static DoorRedstoneMode byOrdinal(int ordinal) {
        DoorRedstoneMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TRIGGER;
    }

    public boolean isGfbs() {
        return this == GFBS_NORMALLY_CLOSED || this == GFBS_NORMALLY_OPEN;
    }

    public String translationKey() {
        return "gui.gfbs_doors.mode." + id;
    }

    public String tooltipKey() {
        return "gui.gfbs_doors.mode." + id + ".tooltip";
    }
}
