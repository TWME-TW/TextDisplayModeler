package dev.twme.textdisplaymodeler.model;

import dev.twme.textdisplaymodeler.TextDisplayModeler;
import dev.twme.textdisplayshape.packet.PacketTriangle;
import dev.twme.textdisplayshape.shape.Shape;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ModelInstance {
    private final UUID instanceId;
    private final String modelName;
    private final List<Facet> facets;
    private final Location origin;
    private final List<Shape> shapes = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();
    private final RenderMode renderMode;
    private float scale = 1.0f;
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private double viewDistance = 64.0;
    private int argbColor = 0xFFFFFFFF; // Default white
    private boolean spawned = false;

    public ModelInstance(UUID instanceId, String modelName, List<Facet> facets, Location origin, RenderMode renderMode) {
        this.instanceId = instanceId;
        this.modelName = modelName;
        this.facets = facets;
        this.origin = origin;
        this.renderMode = renderMode;
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public void spawn() {
        if (spawned) return;

        if (renderMode == RenderMode.PACKET) {
            Bukkit.getScheduler().runTaskAsynchronously(TextDisplayModeler.getInstance(), () -> {
                for (Shape shape : shapes) {
                    shape.spawn();
                }
                
                for (UUID uuid : viewers) {
                    for (Shape shape : shapes) {
                        shape.addViewer(uuid);
                    }
                }
                spawned = true;
            });
        } else {
            for (Shape shape : shapes) {
                shape.spawn();
            }
            spawned = true;
        }
    }

    public void remove() {
        for (Shape shape : shapes) {
            shape.remove();
        }
        spawned = false;
    }

    public void addViewer(Player player) {
        viewers.add(player.getUniqueId());
        if (spawned) {
            for (Shape shape : shapes) {
                shape.addViewer(player.getUniqueId());
            }
        }
    }

    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        if (spawned) {
            for (Shape shape : shapes) {
                shape.removeViewer(player.getUniqueId());
            }
        }
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getModelName() {
        return modelName;
    }

    public Location getOrigin() {
        return origin;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public double getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(double viewDistance) {
        this.viewDistance = viewDistance;
    }

    public int getArgbColor() {
        return argbColor;
    }

    public void setArgbColor(int argbColor) {
        this.argbColor = argbColor;
    }
}
