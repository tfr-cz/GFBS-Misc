package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.lytharalab.gfbs.gltf.api.model.GltfNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * Loads the four OBJ pieces of the T-51b power armour and remembers which node drives which biped part.
 *
 * <p>The import (see {@code tools/import_hbm_power_armor.py}) splits HBM's single OBJ into one file per
 * armour slot, keeping the object names: {@code Helmet}, {@code Chest}, {@code LeftArm},
 * {@code RightArm}, {@code LeftLeg}, {@code RightLeg}, {@code LeftBoot}, {@code RightBoot}. The OBJ
 * importer names nodes {@code <object>/<material>}, so the binding is matched on the prefix.</p>
 *
 * <p>Meshes are drawn double-sided: HBM's export is not consistently wound, and a missing face on a
 * shoulder is far more noticeable than the extra overdraw.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PowerArmorModels {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors/power_armor");

    public enum Part {
        HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
    }

    public record Binding(int node, Part part) {
    }

    public record Suit(GltfInstance instance, List<Binding> bindings) {
    }

    private static final Map<String, Part> PART_BY_OBJECT = Map.of(
        "Helmet", Part.HEAD,
        "Chest", Part.BODY,
        "LeftArm", Part.LEFT_ARM,
        "RightArm", Part.RIGHT_ARM,
        "LeftLeg", Part.LEFT_LEG,
        "RightLeg", Part.RIGHT_LEG,
        "LeftBoot", Part.LEFT_LEG,
        "RightBoot", Part.RIGHT_LEG
    );

    public static final List<EquipmentSlot> SLOTS =
        List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final Map<EquipmentSlot, Suit> SUITS = new EnumMap<>(EquipmentSlot.class);
    private static final Map<EquipmentSlot, Boolean> REQUESTED = new EnumMap<>(EquipmentSlot.class);

    private PowerArmorModels() {
    }

    @Nullable
    public static Suit get(EquipmentSlot slot) {
        Suit suit = SUITS.get(slot);
        if (suit == null) {
            request(slot);
        }
        return suit;
    }

    private static void request(EquipmentSlot slot) {
        if (REQUESTED.put(slot, Boolean.TRUE) != null) {
            return;
        }
        ResourceLocation id = idFor(slot);
        ClientGltfApi.models().load(id).whenComplete((asset, error) -> {
            if (error != null) {
                REQUESTED.remove(slot);
                LOGGER.error("Unable to load the power armour model {}", id,
                    error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                return;
            }
            Minecraft.getInstance().execute(() -> SUITS.put(slot, build(asset)));
        });
    }

    private static Suit build(GltfAsset asset) {
        GltfInstance instance = new GltfInstance(asset);
        instance.renderOptions()
            .frustumCulling(false)
            .occlusionCulling(false)
            .castShadows(false)
            .cull(GltfRenderOptions.CullMode.FORCE_NO_CULL);

        List<Binding> bindings = new ArrayList<>();
        for (int index = 0; index < asset.nodes().size(); index++) {
            GltfNode node = asset.nodes().get(index);
            Part part = PART_BY_OBJECT.get(baseName(node.name()));
            if (part != null) {
                bindings.add(new Binding(index, part));
            }
        }
        return new Suit(instance, List.copyOf(bindings));
    }

    private static String baseName(String nodeName) {
        int slash = nodeName.indexOf('/');
        return slash < 0 ? nodeName : nodeName.substring(0, slash);
    }

    private static ResourceLocation idFor(EquipmentSlot slot) {
        String piece = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chest";
            case LEGS -> "legs";
            default -> "boots";
        };
        return new ResourceLocation("gfbs_doors", "models/armor/power/" + piece + ".obj");
    }

    /** Drops every instance so the next frame reloads; used on resource reload and disconnect. */
    public static void clear() {
        for (Map.Entry<EquipmentSlot, Suit> entry : SUITS.entrySet()) {
            entry.getValue().instance().close();
            ClientGltfApi.models().invalidate(idFor(entry.getKey()));
        }
        SUITS.clear();
        REQUESTED.clear();
    }
}
