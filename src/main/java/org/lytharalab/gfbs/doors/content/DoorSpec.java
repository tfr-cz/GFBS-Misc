package org.lytharalab.gfbs.doors.content;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DoorSpec {
    public final String id;
    public final int[] dimensions;
    public final int offset;
    public final int[][] extraDimensions;
    public final int timeToOpen;
    public final String model;
    public final String[] parts;
    public final String[] skinTextures;
    public final String[] labelTextures;
    public final Map<String, String> partTextures;
    public final float scaleW;
    public final float scaleH;
    public final String baseId;
    public final float soundVolume;
    public final String openLoop;
    public final String closeLoop;
    public final String openStart;
    public final String closeStart;
    public final String openEnd;
    public final String closeEnd;
    public final String loop2;
    public final float hardness;
    public final float resistance;

    private DoorSpec(DoorSpec.Builder b) {
        this.id = b.id;
        this.dimensions = (int[])b.dimensions.clone();
        this.offset = b.offset;
        this.extraDimensions = b.extraDimensions == null
            ? new int[0][]
            : Arrays.stream(b.extraDimensions).map(int[]::clone).toArray(int[][]::new);
        this.timeToOpen = b.timeToOpen;
        this.model = b.model;
        this.parts = (String[])b.parts.clone();
        this.skinTextures = (String[])b.skinTextures.clone();
        this.labelTextures = b.labelTextures == null ? null : (String[])b.labelTextures.clone();
        this.partTextures = Map.copyOf(b.partTextures);
        this.scaleW = b.scaleW;
        this.scaleH = b.scaleH;
        this.baseId = b.baseId;
        this.soundVolume = b.soundVolume;
        this.openLoop = b.openLoop;
        this.closeLoop = b.closeLoop != null ? b.closeLoop : b.openLoop;
        this.openStart = b.openStart;
        this.closeStart = b.closeStart != null ? b.closeStart : b.openStart;
        this.openEnd = b.openEnd;
        this.closeEnd = b.closeEnd != null ? b.closeEnd : b.openEnd;
        this.loop2 = b.loop2;
        this.hardness = b.hardness;
        this.resistance = b.resistance;
    }

    public int skinCount() {
        return this.skinTextures.length;
    }

    public String materialFor(String part, int skin) {
        int index = Math.floorMod(skin, this.skinCount());
        String explicit = (String)this.partTextures.get(part);
        if (explicit != null) {
            return materialName(explicit);
        } else {
            return this.labelTextures != null && "Label".equals(part)
                ? materialName(this.labelTextures[Math.floorMod(index, this.labelTextures.length)])
                : materialName(this.skinTextures[index]);
        }
    }

    public String itemTexture() {
        return this.skinTextures[0];
    }

    public boolean inheritsFrom(String otherId) {
        return otherId.equals(this.id) || otherId.equals(this.baseId);
    }

    public String descriptionId() {
        return "block.gfbs_doors." + this.id;
    }

    public String itemDescriptionId() {
        return "item.gfbs_doors." + this.id;
    }

    public static String materialName(String texture) {
        return texture.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    public static DoorSpec.Builder builder(String id) {
        return new DoorSpec.Builder(id);
    }

    public static final class Builder {
        private final String id;
        private int[] dimensions = new int[]{0, 0, 0, 0, 0, 0};
        private int offset;
        private int[][] extraDimensions;
        private int timeToOpen = 20;
        private String model;
        private String[] parts = new String[]{"Frame"};
        private String[] skinTextures = new String[]{"missing"};
        private String[] labelTextures;
        private final Map<String, String> partTextures = new LinkedHashMap<>();
        private float scaleW = 1.0F;
        private float scaleH = 1.0F;
        private String baseId;
        private float soundVolume = 1.0F;
        private String openLoop;
        private String closeLoop;
        private String openStart;
        private String closeStart;
        private String openEnd;
        private String closeEnd;
        private String loop2;
        private float hardness = 10.0F;
        private float resistance = 1000.0F;

        private Builder(String id) {
            this.id = id;
        }

        public DoorSpec.Builder dims(int u, int d, int n, int s, int w, int e) {
            this.dimensions = new int[]{u, d, n, s, w, e};
            return this;
        }

        public DoorSpec.Builder extraDims(int[]... extra) {
            this.extraDimensions = extra;
            return this;
        }

        public DoorSpec.Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public DoorSpec.Builder time(int ticks) {
            this.timeToOpen = ticks;
            return this;
        }

        public DoorSpec.Builder model(String model) {
            this.model = model;
            return this;
        }

        public DoorSpec.Builder parts(String... parts) {
            this.parts = parts;
            return this;
        }

        public DoorSpec.Builder skins(String... textures) {
            this.skinTextures = textures;
            return this;
        }

        public DoorSpec.Builder labels(String... textures) {
            this.labelTextures = textures;
            return this;
        }

        public DoorSpec.Builder partTexture(String part, String texture) {
            this.partTextures.put(part, texture);
            return this;
        }

        public DoorSpec.Builder scaledFrom(String baseId, float scaleW, float scaleH) {
            this.baseId = baseId;
            this.scaleW = scaleW;
            this.scaleH = scaleH;
            return this;
        }

        public DoorSpec.Builder volume(float volume) {
            this.soundVolume = volume;
            return this;
        }

        public DoorSpec.Builder sounds(String openLoop, String closeLoop, String openStart, String closeStart, String openEnd, String closeEnd, String loop2) {
            this.openLoop = openLoop;
            this.closeLoop = closeLoop;
            this.openStart = openStart;
            this.closeStart = closeStart;
            this.openEnd = openEnd;
            this.closeEnd = closeEnd;
            this.loop2 = loop2;
            return this;
        }

        public DoorSpec.Builder strength(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
            return this;
        }

        public DoorSpec build() {
            return new DoorSpec(this);
        }
    }
}
