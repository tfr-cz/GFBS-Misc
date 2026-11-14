package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;

/**
 * HBM's jump / landing behaviour for the T-51b: {@code handleJump}, {@code handleFall} and the
 * {@code setHasHardLanding(true)} ground slam.
 *
 * <p>In NTM, {@code handleJump} plays {@code jump} on take-off at 0.5/1.0 and {@code handleFall} plays
 * {@code fall} at 0.5/1.0 - both only with the full set on. The slam is the interesting part: with
 * {@code fallDistance > 10} every non-item entity within 3 blocks gets pushed away by
 * {@code offset * intensity * -2}, lifted by {@code 0.1 * intensity} and hurt for
 * {@code (int)(intensity * 10)} with the player as the source, where
 * {@code intensity = 3 - horizontalDistance}. Those numbers are copied verbatim, including the
 * un-normalised offset vector that makes the push grow towards the edge of the radius.</p>
 *
 * <p>HBM made its slam damage bypass armour ({@code setDamageBypassesArmor}); the same is done here with
 * a dedicated damage type that is listed in {@code minecraft:bypasses_armor}, so the shockwave does not
 * get eaten by whatever the target is wearing.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class PowerArmorImpact {
    public static final ResourceKey<DamageType> IMPACT = ResourceKey.create(
        Registries.DAMAGE_TYPE, new ResourceLocation("gfbs_doors", "power_armor_impact"));

    /** HBM: {@code fallDistance > 10.0F}. */
    private static final float SLAM_FALL_DISTANCE = 10.0F;
    /** HBM: bounding box inflated by 3 and entities closer than 3 blocks. */
    private static final double SLAM_RADIUS = 3.0D;
    private static final float JUMP_VOLUME = 0.5F;
    private static final float LAND_VOLUME = 0.5F;

    private PowerArmorImpact() {
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
            || !PowerArmorItem.isCompleteSet(player)) {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.ARMOR_STEP_JUMP.get(), SoundSource.PLAYERS, JUMP_VOLUME, 1.0F);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
            || !PowerArmorItem.isCompleteSet(player)) {
            return;
        }
        if (event.getDistance() > 0.5F) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ARMOR_STEP_LAND.get(), SoundSource.PLAYERS, LAND_VOLUME, 1.0F);
        }
        if (event.getDistance() > SLAM_FALL_DISTANCE) {
            slam(player);
        }
    }

    private static void slam(Player player) {
        DamageSource source = impactSource(player);
        AABB area = player.getBoundingBox().inflate(SLAM_RADIUS, 0.0D, SLAM_RADIUS);
        for (Entity target : player.level().getEntities(player, area, entity -> !(entity instanceof ItemEntity))) {
            double dx = player.getX() - target.getX();
            double dz = player.getZ() - target.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= SLAM_RADIUS) {
                continue;
            }
            double intensity = SLAM_RADIUS - distance;
            target.push(dx * intensity * -2.0D, 0.1D * intensity, dz * intensity * -2.0D);
            target.hurt(source, (float) (int) (intensity * 10.0D));
        }
    }

    private static DamageSource impactSource(Player player) {
        Holder<DamageType> type = player.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(IMPACT);
        return new DamageSource(type, player, player);
    }
}
