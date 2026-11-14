package org.lytharalab.gfbs.doors;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Boots the vanilla registries once for the whole test JVM.
 *
 * <p>{@code Bootstrap.bootStrap()} freezes the registries, so a second call from another test class throws
 * {@code Registry is already frozen}. Every test that needs an {@code ItemStack} asks this instead of
 * calling bootstrap itself.</p>
 *
 * <p>The bootstrap is not expected to finish: Forge hooks {@code NetworkHooks.init()} into the tail of it,
 * and that needs a mod loading context a plain JVM does not have. The registries are frozen before that
 * point, so the failure is swallowed and the result is decided by trying to build an {@code ItemStack} -
 * which is the only thing the tests actually need.</p>
 */
public final class TestBootstrap {
    private static Boolean available;

    private TestBootstrap() {
    }

    /** @return whether items can be created in this JVM */
    public static synchronized boolean itemsAvailable() {
        if (available == null) {
            try {
                SharedConstants.tryDetectVersion();
                Bootstrap.bootStrap();
            } catch (Throwable expected) {
                // Forge's NetworkHooks cannot initialise without mod loading; registries are already frozen.
            }
            try {
                new ItemStack(new Item(new Item.Properties()));
                available = Boolean.TRUE;
            } catch (Throwable unusable) {
                available = Boolean.FALSE;
            }
        }
        return available.booleanValue();
    }
}
