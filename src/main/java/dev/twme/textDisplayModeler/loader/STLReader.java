package dev.twme.textdisplaymodeler.loader;

import dev.twme.textdisplaymodeler.model.Facet;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class STLReader {

    /**
     * Reads an STL file (detects whether it is ASCII or Binary).
     *
     * @param file the STL file to read
     * @return a list of facets
     * @throws IOException if an I/O error occurs
     */
    public static List<Facet> read(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            if (isAscii(file)) {
                return readAscii(new FileInputStream(file));
            } else {
                return readBinary(bis);
            }
        }
    }

    private static boolean isAscii(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.trim().startsWith("solid");
        }
    }

    private static List<Facet> readBinary(InputStream is) throws IOException {
        byte[] header = new byte[80];
        if (is.read(header) != 80) {
            throw new IOException("Malformed binary STL: Header too short");
        }

        byte[] countBytes = new byte[4];
        if (is.read(countBytes) != 4) {
            throw new IOException("Malformed binary STL: Facet count missing");
        }

        int facetCount = ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        List<Facet> facets = new ArrayList<>(facetCount);

        byte[] facetBuffer = new byte[50];
        for (int i = 0; i < facetCount; i++) {
            if (is.read(facetBuffer) != 50) {
                break; // Unexpected end of file
            }
            ByteBuffer bb = ByteBuffer.wrap(facetBuffer).order(ByteOrder.LITTLE_ENDIAN);
            
            // Convert STL Z-up to Minecraft Y-up: STL(x,y,z) -> MC(y,z,x)
            // This cyclic permutation has det=+1 (preserves face winding)
            float nx = bb.getFloat(), ny = bb.getFloat(), nz = bb.getFloat();
            Vector3f normal = new Vector3f(ny, nz, nx);
            float v1x = bb.getFloat(), v1y = bb.getFloat(), v1z = bb.getFloat();
            Vector3f v1 = new Vector3f(v1y, v1z, v1x);
            float v2x = bb.getFloat(), v2y = bb.getFloat(), v2z = bb.getFloat();
            Vector3f v2 = new Vector3f(v2y, v2z, v2x);
            float v3x = bb.getFloat(), v3y = bb.getFloat(), v3z = bb.getFloat();
            Vector3f v3 = new Vector3f(v3y, v3z, v3x);
            // bb.getShort(); // attribute byte count

            facets.add(new Facet(v1, v2, v3, normal));
        }

        return facets;
    }

    private static List<Facet> readAscii(InputStream is) throws IOException {
        List<Facet> facets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            Vector3f normal = null;
            List<Vector3f> vertices = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("facet normal")) {
                    String[] parts = line.split("\\s+");
                    // Convert STL Z-up to Minecraft Y-up: STL(x,y,z) -> MC(y,z,x)
                    normal = new Vector3f(
                            Float.parseFloat(parts[3]),
                            Float.parseFloat(parts[4]),
                            Float.parseFloat(parts[2])
                    );
                } else if (line.startsWith("vertex")) {
                    String[] parts = line.split("\\s+");
                    // Convert STL Z-up to Minecraft Y-up: STL(x,y,z) -> MC(y,z,x)
                    vertices.add(new Vector3f(
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3]),
                            Float.parseFloat(parts[1])
                    ));
                } else if (line.startsWith("endfacet")) {
                    if (vertices.size() == 3) {
                        facets.add(new Facet(vertices.get(0), vertices.get(1), vertices.get(2), normal));
                    }
                    vertices.clear();
                }
            }
        }
        return facets;
    }
}
