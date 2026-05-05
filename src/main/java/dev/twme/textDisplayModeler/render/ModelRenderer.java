package dev.twme.textdisplaymodeler.render;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.twme.textdisplaymodeler.model.Facet;
import dev.twme.textdisplaymodeler.model.ModelInstance;
import dev.twme.textdisplaymodeler.model.RenderMode;
import dev.twme.textdisplayshape.bukkit.BukkitShapeFactory;
import dev.twme.textdisplayshape.packet.PacketShapeFactory;
import dev.twme.textdisplayshape.shape.Shape;

public class ModelRenderer {
    private static final PacketShapeFactory packetFactory = new PacketShapeFactory();
    private static final BukkitShapeFactory bukkitFactory = new BukkitShapeFactory();

    // Directional light pointing from above and to the side (Minecraft Y-up space)
    private static final Vector3f LIGHT_DIR = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    private static final float AMBIENT = 0.4f;
    private static final float DIFFUSE = 0.6f;

    public static ModelInstance assemble(String modelName, List<Facet> facets, Location origin, float scale,
            Vector3f rotation, RenderMode mode, double viewDistance, int argbColor, boolean shadingEnabled) {
        return assemble(UUID.randomUUID(), modelName, facets, origin, scale, rotation, mode, viewDistance, argbColor, shadingEnabled);
    }

    public static ModelInstance assemble(UUID instanceId, String modelName, List<Facet> facets, Location origin,
            float scale, Vector3f rotation, RenderMode mode, double viewDistance, int argbColor, boolean shadingEnabled) {
        ModelInstance instance = new ModelInstance(instanceId, modelName, facets, origin, mode);
        instance.setScale(scale);
        instance.setRotation(rotation);
        instance.setViewDistance(viewDistance);
        instance.setArgbColor(argbColor);
        instance.setShadingEnabled(shadingEnabled);

        Matrix4f transform = new Matrix4f()
                .scale(scale)
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateZ((float) Math.toRadians(rotation.z));

        // Rotation-only matrix for transforming normals (no translation)
        Matrix4f normalTransform = new Matrix4f()
                .rotateX((float) Math.toRadians(rotation.x))
                .rotateY((float) Math.toRadians(rotation.y))
                .rotateZ((float) Math.toRadians(rotation.z));

        for (Facet facet : facets) {
            Vector3f v1 = new Vector3f(facet.v1());
            Vector3f v2 = new Vector3f(facet.v2());
            Vector3f v3 = new Vector3f(facet.v3());

            transform.transformPosition(v1);
            transform.transformPosition(v2);
            transform.transformPosition(v3);

            // Convert to absolute world coordinates as expected by TextDisplayShapes
            v1.add((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
            v2.add((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
            v3.add((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());

            int baseColor = facet.color() != null ? facet.color() : argbColor;
            int finalColor = shadingEnabled
                    ? applyShadingToFacet(baseColor, facet.normal(), facet.v1(), facet.v2(), facet.v3(), normalTransform)
                    : baseColor;

            Shape triangle;
            if (mode == RenderMode.PACKET) {
                triangle = packetFactory.triangle(origin, v1, v2, v3)
                        .color(finalColor)
                        .rootAnchor(true)
                        .doubleSided(false)
                        .seeThrough(false)
                        .viewRange((float) viewDistance / 16f)
                        .build();
            } else {
                triangle = bukkitFactory.triangle(origin, v1, v2, v3)
                        .color(finalColor)
                        .rootAnchor(true)
                        .doubleSided(false)
                        .seeThrough(false)
                        .viewRange((float) viewDistance / 16f)
                        .build();
            }

            instance.addShape(triangle);
        }

        return instance;
    }

    /**
     * Computes a shaded color for a facet based on its normal and a directional light.
     * If the stored normal is zero (e.g. OBJ faces), it is computed from the vertices.
     * Uses the original (pre-transform) vertex positions for normal computation to avoid
     * accumulating floating-point error from transforms.
     */
    private static int applyShadingToFacet(int argbColor, Vector3f storedNormal,
            Vector3f origV1, Vector3f origV2, Vector3f origV3, Matrix4f normalTransform) {
        Vector3f n = new Vector3f(storedNormal);

        // If normal is zero (OBJ files), compute from original vertices
        if (n.lengthSquared() < 0.001f) {
            Vector3f edge1 = new Vector3f(origV2).sub(origV1);
            Vector3f edge2 = new Vector3f(origV3).sub(origV1);
            edge1.cross(edge2, n);
        }

        if (n.lengthSquared() < 0.001f) {
            // Degenerate triangle — no shading possible
            return argbColor;
        }

        // Apply the model's rotation to the normal
        normalTransform.transformDirection(n);
        n.normalize();

        float dot = Math.max(0f, n.dot(LIGHT_DIR));
        float brightness = AMBIENT + dot * DIFFUSE;

        int a = (argbColor >> 24) & 0xFF;
        int r = Math.min(255, (int) (((argbColor >> 16) & 0xFF) * brightness));
        int g = Math.min(255, (int) (((argbColor >> 8) & 0xFF) * brightness));
        int b = Math.min(255, (int) ((argbColor & 0xFF) * brightness));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
