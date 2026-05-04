package dev.twme.textdisplaymodeler.loader;

import dev.twme.textdisplaymodeler.model.Facet;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OBJReader {

    public static List<Facet> read(File file) throws IOException {
        List<Vector3f> vertices = new ArrayList<>();
        List<Facet> facets = new ArrayList<>();
        Map<String, Integer> materialMap = new HashMap<>();
        Integer currentColor = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equalsIgnoreCase("mtllib")) {
                    // Load material library
                    File mtlFile = new File(file.getParentFile(), parts[1]);
                    if (mtlFile.exists()) {
                        materialMap.putAll(MTLReader.read(mtlFile));
                    }
                } else if (parts[0].equalsIgnoreCase("usemtl")) {
                    // Set current material
                    currentColor = materialMap.get(parts[1]);
                } else if (parts[0].equalsIgnoreCase("v")) {
                    // Vertex: v x y z
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    vertices.add(new Vector3f(x, y, z));
                } else if (parts[0].equalsIgnoreCase("f")) {
                    // Face: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                    List<Integer> faceIndices = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String indexPart = parts[i].split("/")[0];
                        if (indexPart.isEmpty()) continue;
                        int index = Integer.parseInt(indexPart);
                        if (index > 0) {
                            faceIndices.add(index - 1);
                        } else if (index < 0) {
                            faceIndices.add(vertices.size() + index);
                        }
                    }

                    // Triangulate if it's a polygon (Fan triangulation)
                    if (faceIndices.size() >= 3) {
                        for (int i = 1; i < faceIndices.size() - 1; i++) {
                            Vector3f v1 = vertices.get(faceIndices.get(0));
                            Vector3f v2 = vertices.get(faceIndices.get(i));
                            Vector3f v3 = vertices.get(faceIndices.get(i + 1));
                            // IMPORTANT: Create new Vector3f copies so that shared vertices don't get mutated multiple times
                            facets.add(new Facet(new Vector3f(v1), new Vector3f(v2), new Vector3f(v3), new Vector3f(0, 0, 0), currentColor));
                        }
                    }
                }
            }
        }
        return facets;
    }
}
