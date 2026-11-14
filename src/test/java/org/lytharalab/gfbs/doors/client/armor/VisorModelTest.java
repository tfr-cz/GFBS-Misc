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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses the visor asset the same way the client does, so the things that can silently go wrong -
 * a missing texture, an opaque material, a quad the wrong size - fail here instead of in game.
 */
class VisorModelTest {
    private static final Path ROOT = Paths.get("src/main/resources/assets/gfbs_doors");

    @Test
    void visorImportsAsBlendedQuadWithItsTexture() throws Exception {
        ResourceLocation id = new ResourceLocation("gfbs_doors", "models/armor/visor.obj");
        GltfAsset asset = ModelImporters.load(id, location -> {
            Path file = ROOT.resolve(location.getPath());
            if (!Files.exists(file)) {
                throw new FileNotFoundException(location + " -> " + file.toAbsolutePath());
            }
            return Files.newInputStream(file);
        });

        assertEquals(1, asset.nodes().size(), "one node");
        GltfMesh mesh = asset.meshes().get(0);
        GltfPrimitive primitive = mesh.primitives().get(0);
        assertEquals(6, primitive.indexCount(), "a quad becomes two triangles");

        GltfMaterial material = asset.materials().get(primitive.material());
        assertEquals(AlphaMode.BLEND, material.alphaMode(), "material must be translucent");
        assertEquals(0.45F, material.baseColorAlpha(), 0.001F, "glTF blender factor comes from MTL d");
        assertTrue(material.baseColorTexture() >= 0, "material must reference a texture");
        GltfTexture texture = asset.textures().get(material.baseColorTexture());
        // The importer keeps the relative reference as the texture name and embeds the file bytes,
        // which is what proves the resolver found models/armor/tex/hazmat_visor.png.
        assertTrue(texture.name().contains("tex/hazmat_visor.png"), "texture name " + texture.name());
        assertTrue(texture.encodedImageSize() > 0, "texture bytes must be embedded");
        assertTrue(Files.exists(ROOT.resolve("models/armor/tex/hazmat_visor.png")), "texture file exists");

        float[] positions = primitive.positions();
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < positions.length; i += 3) {
            minX = Math.min(minX, positions[i]);
            maxX = Math.max(maxX, positions[i]);
            minY = Math.min(minY, positions[i + 1]);
            maxY = Math.max(maxY, positions[i + 1]);
            assertEquals(-5.83F, positions[i + 2], 0.001F, "pane sits at the frame's front");
        }
        // The pane must cover the visor opening in biped pixels (the model's glass box was
        // x -3.96..3.95, y -5.28..-0.29), and stay inside it.
        assertEquals(-3.9F, minX, 0.01F);
        assertEquals(3.89F, maxX, 0.01F);
        assertEquals(-5.22F, minY, 0.01F);
        assertEquals(-0.37F, maxY, 0.01F);
        assertFalse(primitive.positions().length == 0, "geometry present");
    }
}
