package dev.twme.textdisplaymodeler.model;

import org.joml.Vector3f;

public class Facet {
    private final Vector3f v1, v2, v3;
    private final Vector3f normal;
    private final Integer color;

    public Facet(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal) {
        this(v1, v2, v3, normal, null);
    }

    public Facet(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal, Integer color) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.normal = normal;
        this.color = color;
    }

    public Vector3f v1() { return v1; }
    public Vector3f v2() { return v2; }
    public Vector3f v3() { return v3; }
    public Vector3f normal() { return normal; }
    public Integer color() { return color; }
}
