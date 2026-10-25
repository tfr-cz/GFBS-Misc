package org.lytharalab.cabinelevator.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record DimPos(ResourceKey<Level> dimension, BlockPos pos) {
    public static DimPos of(ServerLevel level, BlockPos pos) {
        return new DimPos(level.dimension(), pos.immutable());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    @Nullable
    public static DimPos load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
        if (id == null) {
            return null;
        }
        return new DimPos(ResourceKey.create(Registries.DIMENSION, id),
                new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
    }

    @Nullable
    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(dimension);
    }
}
