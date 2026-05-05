package dev.twme.textdisplaymodeler.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import dev.twme.textdisplaymodeler.TextDisplayModeler;
import dev.twme.textdisplaymodeler.render.ModelRenderer;

public class ModelManager {
    private static final Map<String, List<Facet>> loadedFacets = new HashMap<>();
    private static final List<ModelInstance> activeInstances = new ArrayList<>();
    private static File instancesFile;
    private static YamlConfiguration instancesConfig;

    public static void init() {
        instancesFile = new File(TextDisplayModeler.getInstance().getDataFolder(), "instances.yml");
        if (!instancesFile.exists()) {
            try {
                instancesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        instancesConfig = YamlConfiguration.loadConfiguration(instancesFile);
        
        // Load instances for already loaded worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            loadInstances(world.getName());
        }

        // Register world load/unload listeners
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent event) {
                loadInstances(event.getWorld().getName());
            }

            @org.bukkit.event.EventHandler
            public void onWorldUnload(org.bukkit.event.world.WorldUnloadEvent event) {
                activeInstances.removeIf(instance -> {
                    if (instance.getOrigin().getWorld().equals(event.getWorld())) {
                        instance.remove();
                        return true;
                    }
                    return false;
                });
            }
        }, TextDisplayModeler.getInstance());

        // Visibility task
        Bukkit.getScheduler().runTaskTimer(TextDisplayModeler.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ModelInstance instance : activeInstances) {
                    if (instance.getRenderMode() == RenderMode.PACKET) {
                        if (!player.getWorld().equals(instance.getOrigin().getWorld())) {
                            instance.removeViewer(player);
                            continue;
                        }
                        double distSq = player.getLocation().distanceSquared(instance.getOrigin());
                        if (distSq <= instance.getViewDistance() * instance.getViewDistance()) {
                            instance.addViewer(player);
                        } else {
                            instance.removeViewer(player);
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    private static void loadInstances(String worldName) {
        ConfigurationSection section = instancesConfig.getConfigurationSection("instances");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection instanceSec = section.getConfigurationSection(key);
            if (instanceSec == null) continue;

            // Check world name before getting location to avoid "unknown world" error
            String storedWorld = instanceSec.getString("world");
            if (storedWorld == null || !storedWorld.equals(worldName)) continue;

            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = instanceSec.getDouble("x");
            double y = instanceSec.getDouble("y");
            double z = instanceSec.getDouble("z");
            Location loc = new Location(world, x, y, z);

            UUID id = UUID.fromString(key);
            
            // Avoid duplicate loading
            boolean alreadyActive = activeInstances.stream().anyMatch(i -> i.getInstanceId().equals(id));
            if (alreadyActive) continue;

            String modelName = instanceSec.getString("modelName");
            float scale = (float) instanceSec.getDouble("scale", 1.0);
            Vector3f rot = new Vector3f(
                    (float) instanceSec.getDouble("rotation.x", 0),
                    (float) instanceSec.getDouble("rotation.y", 0),
                    (float) instanceSec.getDouble("rotation.z", 0)
            );
            RenderMode mode = RenderMode.valueOf(instanceSec.getString("mode", "PACKET"));
            double viewDist = instanceSec.getDouble("viewDistance", 64.0);
            int color = instanceSec.getInt("color", 0xFFFFFFFF);
            boolean shadingEnabled = instanceSec.getBoolean("shadingEnabled", true);

            // Try to find the file with either .stl or .obj
            if (!loadedFacets.containsKey(modelName)) {
                if (loadModel(modelName, modelName + ".stl") == -1) {
                    loadModel(modelName, modelName + ".obj");
                }
            }

            if (loadedFacets.containsKey(modelName)) {
                ModelInstance instance = ModelRenderer.assemble(id, modelName, loadedFacets.get(modelName), loc, scale, rot, mode, viewDist, color, shadingEnabled);
                instance.spawn();
                activeInstances.add(instance);
            }
        }
    }

    private static void saveInstances() {
        instancesConfig.set("instances", null);
        for (ModelInstance instance : activeInstances) {
            String path = "instances." + instance.getInstanceId().toString();
            instancesConfig.set(path + ".modelName", instance.getModelName());
            instancesConfig.set(path + ".world", instance.getOrigin().getWorld().getName());
            instancesConfig.set(path + ".x", instance.getOrigin().getX());
            instancesConfig.set(path + ".y", instance.getOrigin().getY());
            instancesConfig.set(path + ".z", instance.getOrigin().getZ());
            instancesConfig.set(path + ".scale", instance.getScale());
            instancesConfig.set(path + ".rotation.x", instance.getRotation().x);
            instancesConfig.set(path + ".rotation.y", instance.getRotation().y);
            instancesConfig.set(path + ".rotation.z", instance.getRotation().z);
            instancesConfig.set(path + ".mode", instance.getRenderMode().name());
            instancesConfig.set(path + ".viewDistance", instance.getViewDistance());
            instancesConfig.set(path + ".color", instance.getArgbColor());
            instancesConfig.set(path + ".shadingEnabled", instance.isShadingEnabled());
        }
        try {
            instancesConfig.save(instancesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int loadModel(String name, String fileName) {
        File file = new File(TextDisplayModeler.getInstance().getDataFolder(), "models/" + fileName);
        if (!file.exists()) {
            return -1;
        }
        try {
            List<Facet> facets;
            if (fileName.toLowerCase().endsWith(".obj")) {
                facets = dev.twme.textdisplaymodeler.loader.OBJReader.read(file);
            } else {
                facets = dev.twme.textdisplaymodeler.loader.STLReader.read(file);
            }

            if (facets != null && !facets.isEmpty()) {
                // --- Auto-Center and Axis Correction Logic ---
                Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
                Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

                for (Facet f : facets) {
                    updateMinMax(min, max, f.v1());
                    updateMinMax(min, max, f.v2());
                    updateMinMax(min, max, f.v3());
                }

                Vector3f center = new Vector3f();
                min.add(max, center).mul(0.5f);

                // Calculate max dimension for normalization
                float sizeX = max.x - min.x;
                float sizeY = max.y - min.y;
                float sizeZ = max.z - min.z;
                float maxSize = Math.max(sizeX, Math.max(sizeY, sizeZ));
                
                // If model is effectively flat or empty, prevent division by zero
                if (maxSize < 0.0001f) maxSize = 1.0f;
                
                float normalizationScale = 1.0f / maxSize;

                for (Facet f : facets) {
                    // Subtract center and normalize scale to 1x1x1
                    f.v1().sub(center).mul(normalizationScale);
                    f.v2().sub(center).mul(normalizationScale);
                    f.v3().sub(center).mul(normalizationScale);
                }
            }

            loadedFacets.put(name, facets);
            return facets != null ? facets.size() : 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void updateMinMax(Vector3f min, Vector3f max, Vector3f v) {
        if (v.x < min.x) min.x = v.x;
        if (v.y < min.y) min.y = v.y;
        if (v.z < min.z) min.z = v.z;
        if (v.x > max.x) max.x = v.x;
        if (v.y > max.y) max.y = v.y;
        if (v.z > max.z) max.z = v.z;
    }

    public static boolean unloadModel(String name) {
        return loadedFacets.remove(name) != null;
    }

    public static ModelInstance spawnModel(String name, Location location, float scale, Vector3f rotation, RenderMode mode, double viewDistance, int color, boolean shadingEnabled) {
        if (!loadedFacets.containsKey(name)) {
            if (loadModel(name, name + ".stl") == -1) {
                loadModel(name, name + ".obj");
            }
        }
        
        List<Facet> facets = loadedFacets.get(name);
        if (facets == null) {
            return null;
        }
        ModelInstance instance = ModelRenderer.assemble(name, facets, location, scale, rotation, mode, viewDistance, color, shadingEnabled);
        instance.spawn();
        activeInstances.add(instance);
        saveInstances();
        return instance;
    }

    public static void removeAll() {
        for (ModelInstance instance : activeInstances) {
            instance.remove();
        }
        activeInstances.clear();
        saveInstances();
    }

    public static Map<String, List<Facet>> getLoadedFacets() {
        return loadedFacets;
    }
}
