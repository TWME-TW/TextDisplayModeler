package dev.twme.textdisplaymodeler.model;

import dev.twme.textdisplayshape.shape.Shape;
import org.bukkit.Location;
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
    private boolean spawned = false;

    public ModelInstance(UUID instanceId, String modelName, List<Facet> facets, Location origin, RenderMode renderMode) {
        this.instanceId = instanceId;
        this.modelName = modelName;
        this.facets = facets;
        this.origin = origin;
        this.renderMode = renderMode;
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getModelName() {
        return modelName;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public double getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(double viewDistance) {
        this.viewDistance = viewDistance;
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public Location getOrigin() {
        return origin;
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public void addViewer(Player player) {
        if (viewers.add(player.getUniqueId())) {
            for (Shape shape : shapes) {
                shape.addViewer(player.getUniqueId());
            }
        }
    }

    public void removeViewer(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            for (Shape shape : shapes) {
                shape.removeViewer(player.getUniqueId());
            }
        }
    }

    public void spawn() {
        if (spawned) return;
        for (Shape shape : shapes) {
            shape.spawn();
        }
        spawned = true;
    }

    public void remove() {
        for (Shape shape : shapes) {
            shape.remove();
        }
        shapes.clear();
        viewers.clear();
        spawned = false;
    }

    public boolean isSpawned() {
        return spawned;
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
}
