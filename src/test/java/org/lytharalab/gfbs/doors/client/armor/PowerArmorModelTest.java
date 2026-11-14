package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.gltf.api.io.ModelImporters;
import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.lytharalab.gfbs.gltf.api.model.GltfMaterial;
import org.lytharalab.gfbs.gltf.api.model.GltfMesh;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;
import org.lytharalab.gfbs.gltf.api.model.GltfTexture;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Imports the four T-51b pieces the way the client does, so a bad split (missing object, missing
 * texture, wrong vertex space) fails the build instead of showing up as an invisible suit in game.
 */
class PowerArmorModelTest {
    private static final Path ROOT = Paths.get("src/main/resources/assets/gfbs_doors");

    @Test
    void everyPieceImportsWithItsPartsAndTextures() throws Exception {
        Map<String, List<String>> expected = Map.of(
            "helmet", List.of("Helmet"),
            "chest", List.of("Chest", "LeftArm", "RightArm"),
            "legs", List.of("LeftLeg", "RightLeg"),
            "boots", List.of("LeftBoot", "RightBoot")
        );

        for (Map.Entry<String, List<String>> piece : expected.entrySet()) {
            GltfAsset asset = load(piece.getKey());
            List<String> groups = asset.nodes().stream()
                .map(node -> node.name().split("/")[0])
                .distinct()
                .sorted()
                .toList();
            assertEquals(piece.getValue().stream().sorted().toList(), groups,
                piece.getKey() + " groups");

            GltfMesh mesh = asset.meshes().get(0);
            GltfPrimitive primitive = mesh.primitives().get(0);
            GltfMaterial material = asset.materials().get(primitive.material());
            assertEquals(AlphaMode.OPAQUE, material.alphaMode(), "solid armour, no blending needed");
            assertTrue(material.baseColorTexture() >= 0, piece.getKey() + " must reference a texture");
            GltfTexture texture = asset.textures().get(material.baseColorTexture());
            assertTrue(texture.encodedImageSize() > 0, piece.getKey() + " texture bytes embedded");
            assertTrue(texture.name().contains("t51_"), "texture " + texture.name());

            float[] positions = primitive.positions();
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (int i = 1; i < positions.length; i += 3) {
                minY = Math.min(minY, positions[i]);
                maxY = Math.max(maxY, positions[i]);
            }
            // Authored in vanilla biped pixels: the top of the helmet is around -8.65, the soles at 24.24.
            assertTrue(minY >= -9.0F && maxY <= 25.0F, piece.getKey() + " stays in biped space: " + minY + ".." + maxY);
        }
    }

    private static GltfAsset load(String piece) throws Exception {
        ResourceLocation id = new ResourceLocation("gfbs_doors", "models/armor/power/" + piece + ".obj");
        return ModelImporters.load(id, location -> {
            Path file = ROOT.resolve(location.getPath());
            if (!Files.exists(file)) {
                throw new FileNotFoundException(location + " -> " + file.toAbsolutePath());
            }
            return Files.newInputStream(file);
        });
    }
}
