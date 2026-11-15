package org.lytharalab.gfbs.doors.content.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.lytharalab.gfbs.doors.content.ModSounds;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * HBM's Crucible - 裁决剑 - the giant glowing sword, ported with infinite energy.
 *
 * <p>The original ran on three charges: every hit spent one point of durability, {@code getAttributeModifiers}
 * only paid out the 5000 damage while {@code damage < maxDamage}, and once empty the sword refused to hurt
 * anything and told you "Not enough energy". All of that is dropped here: {@link #canBeDepleted()} is false, so
 * no charge is ever spent, the modifiers always apply and the blade never goes dark. Everything else - the
 * numbers, the hit sound, the killed-enemy burst, the deploy sound - is NTM's.</p>
 *
 * <p>Appearance: the mesh is HBM's own OBJ (Hilt / GuardLeft / GuardRight / Blade) with its three textures, drawn
 * by {@code CrucibleItemRenderer} instead of the flat sprite. See that class for the pose.</p>
 */
public final class CrucibleItem extends SwordItem {
    /** HBM: {@code new ItemCrucible(5000.0F, 1.0, matCrucible)} - the whole attack damage, not a bonus. */
    private static final int ATTACK_DAMAGE = 5000;
    /** What a player's bare hand already deals; {@code SwordItem} adds its modifier on top of this. */
    private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0F;
    /** NTM passed no attack-speed modifier at all, so the sword swings at the player's base speed. */
    private static final float ATTACK_SPEED = 0.0F;
    /** {@code new AttributeModifier(uuid, "Tool modifier", 1.0, 1)} - operation 1, so +100% of base speed. */
    private static final double MOVEMENT_BONUS = 1.0;
    private static final UUID MOVEMENT_MODIFIER = UUID.fromString("0d1f4a7e-2b6c-4f5a-9c31-7a8e5d2b0c94");
    /** Remembers whether this stack was the selected hotbar item last tick, for the deploy sound. */
    private static final String HELD_TAG = "gfbs_held";

    public CrucibleItem() {
        super(CrucibleTier.INSTANCE, attackDamageModifier(), ATTACK_SPEED, new Properties().stacksTo(1));
    }

    /** {@code SwordItem} builds {@code attackDamage = modifier + tier bonus}, and the player adds 1 on top. */
    private static int attackDamageModifier() {
        return (int) (ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE - CrucibleTier.ATTACK_DAMAGE_BONUS);
    }

    /** The number the player actually deals with this in hand, for the test that pins NTM's 5000. */
    static int totalAttackDamage() {
        return attackDamageModifier() + (int) CrucibleTier.ATTACK_DAMAGE_BONUS + (int) PLAYER_BASE_ATTACK_DAMAGE;
    }

    /**
     * Infinite energy: the sword is never depleted, so {@code ItemStack#hurtAndBreak} turns into a no-op and the
     * durability bar never appears. NTM's sword spent one of its three charges per hit; this is the one behaviour
     * the port deliberately changes.
     */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /** NTM's speed bonus was a held-item modifier, which 1.20.1 asks for per slot. */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND) {
            return base;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
            .putAll(base)
            .put(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_MODIFIER, "Crucible movement",
                MOVEMENT_BONUS, AttributeModifier.Operation.MULTIPLY_BASE))
            .build();
    }

    /**
     * NTM's {@code ItemCrucible#hitEntity}, which ran after the damage landed: a door-crunch at the victim, and if
     * that hit killed it, a burst of red block dust scaled by the victim's maximum health.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel server) {
            server.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                ModSounds.CRUCIBLE_SWING.get(), SoundSource.PLAYERS, 1.0F,
                0.8F + attacker.getRandom().nextFloat() * 0.2F);
            server.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F,
                0.75F + target.getRandom().nextFloat() * 0.2F);
            if (!target.isAlive()) {
                // NTM: min(ceil(maxHealth / 3), 250) * 4 block particles, motion 0.1, at the victim's middle.
                int count = Math.min((int) Math.ceil(target.getMaxHealth() / 3.0D), 250) * 4;
                server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_BRICKS.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    count, 0.1D, 0.1D, 0.1D, 0.1D);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * NTM's {@code IEquipReceiver#onEquip}: the deploy sound when the sword becomes the held item.
     *
     * <p>1.20.1 has no equip hook, but {@code inventoryTick} is called for every hotbar stack with
     * {@code selected} telling us whether it is the held one, so the transition is what triggers the sound.
     * The previous state is kept on the stack so there is no per-player bookkeeping to leak.</p>
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player)) {
            return;
        }
        CompoundTag tag = stack.getTag();
        boolean wasSelected = tag != null && tag.getBoolean(HELD_TAG);
        if (selected == wasSelected) {
            return;
        }
        stack.getOrCreateTag().putBoolean(HELD_TAG, selected);
        if (selected) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.CRUCIBLE_DEPLOY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** NTM's charge readout, which with infinite energy can only ever say one thing. */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gfbs_doors.crucible.charge").withStyle(ChatFormatting.RED));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                // Only touched on the client, so the renderer class never loads on a dedicated server.
                return org.lytharalab.gfbs.doors.client.weapon.CrucibleItemRenderer.getInstance();
            }
        });
    }
}
