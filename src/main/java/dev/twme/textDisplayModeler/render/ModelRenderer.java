package dev.twme.textdisplaymodeler.render;

import dev.twme.textdisplaymodeler.model.Facet;
import dev.twme.textdisplaymodeler.model.ModelInstance;
import dev.twme.textdisplaymodeler.model.RenderMode;
import dev.twme.textdisplayshape.bukkit.BukkitShapeFactory;
import dev.twme.textdisplayshape.packet.PacketShapeFactory;
import dev.twme.textdisplayshape.shape.Shape;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class ModelRenderer {
    private static final PacketShapeFactory packetFactory = new PacketShapeFactory();
    private static final BukkitShapeFactory bukkitFactory = new BukkitShapeFactory();

    public static ModelInstance assemble(String modelName, List<Facet> facets, Location origin, float scale,
            Vector3f rotation, RenderMode mode, double viewDistance, int argbColor) {
        return assemble(UUID.randomUUID(), modelName, facets, origin, scale, rotation, mode, viewDistance, argbColor);
    }

    public static ModelInstance assemble(UUID instanceId, String modelName, List<Facet> facets, Location origin,
            float scale, Vector3f rotation, RenderMode mode, double viewDistance, int argbColor) {
        ModelInstance instance = new ModelInstance(instanceId, modelName, facets, origin, mode);
        instance.setScale(scale);
        instance.setRotation(rotation);
        instance.setViewDistance(viewDistance);
        instance.setArgbColor(argbColor);

        Matrix4f transform = new Matrix4f()
                .scale(scale)
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

            int finalColor = facet.color() != null ? facet.color() : argbColor;

            Shape triangle;
            if (mode == RenderMode.PACKET) {
                triangle = packetFactory.triangle(origin, v1, v2, v3)
                        .color(finalColor)
                        .doubleSided(false) // Force double sided to eliminate normal issues
                        .seeThrough(false)
                        .viewRange((float) viewDistance / 16f)
                        .build();
            } else {
                triangle = bukkitFactory.triangle(origin, v1, v2, v3)
                        .color(finalColor)
                        .doubleSided(false) // Force double sided
                        .seeThrough(false)
                        .viewRange((float) viewDistance / 16f)
                        .build();
            }

            instance.addShape(triangle);
        }

        return instance;
    }
}
