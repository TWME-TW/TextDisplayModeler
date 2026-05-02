package dev.twme.textdisplaymodeler.model;

import dev.twme.textdisplaymodeler.TextDisplayModeler;
import dev.twme.textdisplaymodeler.loader.STLReader;
import dev.twme.textdisplaymodeler.render.ModelRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        loadInstances();

        // Visibility task
        Bukkit.getScheduler().runTaskTimer(TextDisplayModeler.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ModelInstance instance : activeInstances) {
                    if (instance.getRenderMode() == RenderMode.PACKET) {
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

    private static void loadInstances() {
        ConfigurationSection section = instancesConfig.getConfigurationSection("instances");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection instanceSec = section.getConfigurationSection(key);
            if (instanceSec == null) continue;

            UUID id = UUID.fromString(key);
            String modelName = instanceSec.getString("modelName");
            Location loc = instanceSec.getLocation("location");
            float scale = (float) instanceSec.getDouble("scale", 1.0);
            Vector3f rot = new Vector3f(
                    (float) instanceSec.getDouble("rotation.x", 0),
                    (float) instanceSec.getDouble("rotation.y", 0),
                    (float) instanceSec.getDouble("rotation.z", 0)
            );
            RenderMode mode = RenderMode.valueOf(instanceSec.getString("mode", "PACKET"));
            double viewDist = instanceSec.getDouble("viewDistance", 64.0);
            int color = instanceSec.getInt("color", 0xFFFFFFFF);

            // Try to find the file with either .stl or .obj
            if (!loadedFacets.containsKey(modelName)) {
                if (!loadModel(modelName, modelName + ".stl")) {
                    loadModel(modelName, modelName + ".obj");
                }
            }

            if (loadedFacets.containsKey(modelName)) {
                ModelInstance instance = ModelRenderer.assemble(id, modelName, loadedFacets.get(modelName), loc, scale, rot, mode, viewDist, color);
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
            instancesConfig.set(path + ".location", instance.getOrigin());
            instancesConfig.set(path + ".scale", instance.getScale());
            instancesConfig.set(path + ".rotation.x", instance.getRotation().x);
            instancesConfig.set(path + ".rotation.y", instance.getRotation().y);
            instancesConfig.set(path + ".rotation.z", instance.getRotation().z);
            instancesConfig.set(path + ".mode", instance.getRenderMode().name());
            instancesConfig.set(path + ".viewDistance", instance.getViewDistance());
            instancesConfig.set(path + ".color", instance.getArgbColor());
        }
        try {
            instancesConfig.save(instancesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadModel(String name, String fileName) {
        File file = new File(TextDisplayModeler.getInstance().getDataFolder(), "models/" + fileName);
        if (!file.exists()) {
            return false;
        }
        try {
            List<Facet> facets;
            if (fileName.toLowerCase().endsWith(".obj")) {
                facets = dev.twme.textdisplaymodeler.loader.OBJReader.read(file);
            } else {
                facets = STLReader.read(file);
            }
            loadedFacets.put(name, facets);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ModelInstance spawnModel(String name, Location location, float scale, Vector3f rotation, RenderMode mode, double viewDistance, int color) {
        List<Facet> facets = loadedFacets.get(name);
        if (facets == null) {
            return null;
        }
        ModelInstance instance = ModelRenderer.assemble(name, facets, location, scale, rotation, mode, viewDistance, color);
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
