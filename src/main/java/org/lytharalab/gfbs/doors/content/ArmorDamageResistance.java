package org.lytharalab.gfbs.doors.content;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.EuphemiumArmorItem;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistances;

/**
 * Applies registered armour resistances, the 1.20.1 port of HBM's {@code DamageResistanceHandler}.
 *
 * <p>HBM worked in two steps, and so does this:</p>
 * <ol>
 *   <li>{@code LivingAttackEvent}: if the threshold would swallow the whole hit ({@code dt >= amount}) or
 *       the set is completely immune ({@code dr >= 1}), cancel the attack outright. HBM also hurt the
 *       armour's durability here; this port does not, because cancelling the hit already means nothing was
 *       absorbed and losing durability for a blocked hit would be surprising.</li>
 *   <li>{@code LivingHurtEvent}: {@code amount = (amount - dt) * (1 - dr)}.</li>
 * </ol>
 *
 * <p>Only players are considered, because only players can wear the armour sets this mod registers. The
 * vanilla armour calculation still runs afterwards, exactly as it did in NTM where the resistance system
 * sat on top of armour points.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class ArmorDamageResistance {
    private ArmorDamageResistance() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (!(entity instanceof Player) || entity.level().isClientSide() || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float[] dtDr = ArmorResistances.resolve(entity, source);
        float threshold = Math.max(0.0F, dtDr[0]);
        float resistance = dtDr[1];
        if (resistance >= 1.0F || (threshold > 0.0F && threshold >= event.getAmount())) {
            // HBM turned a hit the suit fully absorbed into a "break" tink; the hook is here because this is the
            // one place that knows the hit really was absorbed rather than merely cancelled by someone else.
            EuphemiumArmorItem.playAbsorbSound((Player) entity);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (!(entity instanceof Player) || entity.level().isClientSide()
            || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float[] dtDr = ArmorResistances.resolve(entity, source);
        float remaining = event.getAmount() - Math.max(0.0F, dtDr[0]);
        if (remaining <= 0.0F) {
            event.setAmount(0.0F);
            return;
        }
        float resistance = net.minecraft.util.Mth.clamp(dtDr[1], 0.0F, 1.0F);
        event.setAmount(remaining * (1.0F - resistance));
    }
}
