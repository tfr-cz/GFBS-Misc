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

    private DoorSpec(Builder b) {
        this.id = b.id;
        this.dimensions = b.dimensions.clone();
        this.offset = b.offset;
        this.extraDimensions = b.extraDimensions == null ? new int[0][] : Arrays.stream(b.extraDimensions).map(int[]::clone).toArray(int[][]::new);
        this.timeToOpen = b.timeToOpen;
        this.model = b.model;
        this.parts = b.parts.clone();
        this.skinTextures = b.skinTextures.clone();
        this.labelTextures = b.labelTextures == null ? null : b.labelTextures.clone();
        this.partTextures = Map.copyOf(b.partTextures);
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
        return skinTextures.length;
    }

    public String materialFor(String part, int skin) {
        int index = Math.floorMod(skin, skinCount());
        String explicit = partTextures.get(part);
        if (explicit != null) return materialName(explicit);
        if (labelTextures != null && "Label".equals(part)) {
            return materialName(labelTextures[Math.floorMod(index, labelTextures.length)]);
        }
        return materialName(skinTextures[index]);
    }

    public String itemTexture() {
        return skinTextures[0];
    }

    public String descriptionId() {
        return "block.gfbs_doors." + id;
    }

    public String itemDescriptionId() {
        return "item.gfbs_doors." + id;
    }

    public static String materialName(String texture) {
        return texture.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private int[] dimensions = {0, 0, 0, 0, 0, 0};
        private int offset;
        private int[][] extraDimensions;
        private int timeToOpen = 20;
        private String model;
        private String[] parts = {"Frame"};
        private String[] skinTextures = {"missing"};
        private String[] labelTextures;
        private final Map<String, String> partTextures = new LinkedHashMap<>();
        private float soundVolume = 1F;
        private String openLoop;
        private String closeLoop;
        private String openStart;
        private String closeStart;
        private String openEnd;
        private String closeEnd;
        private String loop2;
        private float hardness = 10F;
        private float resistance = 1000F;

        private Builder(String id) {
            this.id = id;
        }

        public Builder dims(int u, int d, int n, int s, int w, int e) {
            this.dimensions = new int[]{u, d, n, s, w, e};
            return this;
        }

        public Builder extraDims(int[]... extra) {
            this.extraDimensions = extra;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder time(int ticks) {
            this.timeToOpen = ticks;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder parts(String... parts) {
            this.parts = parts;
            return this;
        }

        public Builder skins(String... textures) {
            this.skinTextures = textures;
            return this;
        }

        public Builder labels(String... textures) {
            this.labelTextures = textures;
            return this;
        }

        public Builder partTexture(String part, String texture) {
            this.partTextures.put(part, texture);
            return this;
        }

        public Builder volume(float volume) {
            this.soundVolume = volume;
            return this;
        }

        public Builder sounds(String openLoop, String closeLoop, String openStart, String closeStart,
                              String openEnd, String closeEnd, String loop2) {
            this.openLoop = openLoop;
            this.closeLoop = closeLoop;
            this.openStart = openStart;
            this.closeStart = closeStart;
            this.openEnd = openEnd;
            this.closeEnd = closeEnd;
            this.loop2 = loop2;
            return this;
        }

        public Builder strength(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
            return this;
        }

        public DoorSpec build() {
            return new DoorSpec(this);
        }
    }
}
