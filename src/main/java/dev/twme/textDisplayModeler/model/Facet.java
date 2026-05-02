package dev.twme.textdisplaymodeler.model;

import org.joml.Vector3f;

/**
 * Represents a single triangular facet from an STL file.
 */
public record Facet(Vector3f normal, Vector3f v1, Vector3f v2, Vector3f v3) {
}
