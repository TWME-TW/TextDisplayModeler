package dev.twme.textdisplaymodeler.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MTLReader {

    public static Map<String, Integer> read(File file) throws IOException {
        Map<String, Integer> materials = new HashMap<>();
        String currentMaterial = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equalsIgnoreCase("newmtl")) {
                    currentMaterial = parts[1];
                } else if (parts[0].equalsIgnoreCase("Kd") && currentMaterial != null) {
                    // Diffuse color: Kd r g b (0.0 to 1.0)
                    float r = Float.parseFloat(parts[1]);
                    float g = Float.parseFloat(parts[2]);
                    float b = Float.parseFloat(parts[3]);
                    
                    int argb = 0xFF000000 | 
                              ((int) (r * 255) << 16) | 
                              ((int) (g * 255) << 8) | 
                              ((int) (b * 255));
                    
                    materials.put(currentMaterial, argb);
                }
            }
        }
        return materials;
    }
}
