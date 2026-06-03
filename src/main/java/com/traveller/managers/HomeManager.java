package com.traveller.managers;

import com.traveller.Traveller;
import com.traveller.model.Home;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class HomeManager {

    private final Traveller plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<UUID, Map<String, Home>> homes = new ConcurrentHashMap<>();
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();

    public HomeManager(Traveller plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    public synchronized void load() {
        homes.clear();
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            return;
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid UUID in playerdata.yml: " + uuidStr);
                continue;
            }
            ConfigurationSection homesSection = playersSection.getConfigurationSection(uuidStr + ".homes");
            if (homesSection == null) {
                continue;
            }
            Map<String, Home> playerHomes = new LinkedHashMap<>();
            for (String homeName : homesSection.getKeys(false)) {
                ConfigurationSection h = homesSection.getConfigurationSection(homeName);
                if (h == null) {
                    continue;
                }
                Home home = new Home(homeName,
                        h.getString("world", ""),
                        h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                        (float) h.getDouble("yaw"), (float) h.getDouble("pitch"));
                playerHomes.put(homeName.toLowerCase(Locale.ROOT), home);
            }
            homes.put(uuid, playerHomes);
        }
    }

    public synchronized void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Home>> entry : homes.entrySet()) {
            String base = "players." + entry.getKey() + ".homes.";
            for (Home home : entry.getValue().values()) {
                String path = base + home.getName() + ".";
                out.set(path + "world", home.getWorld());
                out.set(path + "x", home.getX());
                out.set(path + "y", home.getY());
                out.set(path + "z", home.getZ());
                out.set(path + "yaw", home.getYaw());
                out.set(path + "pitch", home.getPitch());
            }
        }
        data = out;
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            out.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save playerdata.yml", ex);
        }
    }

    public Map<String, Home> getHomes(UUID uuid) {
        return homes.getOrDefault(uuid, new LinkedHashMap<>());
    }

    public int getHomeCount(UUID uuid) {
        Map<String, Home> m = homes.get(uuid);
        return m == null ? 0 : m.size();
    }

    public Home getHome(UUID uuid, String name) {
        Map<String, Home> m = homes.get(uuid);
        if (m == null) {
            return null;
        }
        return m.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean hasHome(UUID uuid, String name) {
        return getHome(uuid, name) != null;
    }

    public void setHome(UUID uuid, String name, Location loc) {
        homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                .put(name.toLowerCase(Locale.ROOT), Home.fromLocation(name, loc));
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, Home> m = homes.get(uuid);
        if (m == null) {
            return false;
        }
        return m.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public List<String> getHomeNames(UUID uuid) {
        Map<String, Home> m = homes.get(uuid);
        if (m == null) {
            return new ArrayList<>();
        }
        List<String> names = new ArrayList<>();
        for (Home h : m.values()) {
            names.add(h.getName());
        }
        return names;
    }

    public int getHomeLimit(Player player) {
        if (player.hasPermission("traveller.homes.unlimited")) {
            return Integer.MAX_VALUE;
        }
        int highest = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (!info.getValue() || !perm.startsWith("traveller.homes.")) {
                continue;
            }
            String suffix = perm.substring("traveller.homes.".length());
            try {
                highest = Math.max(highest, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Non-numeric suffix such as "unlimited" is handled above.
            }
        }
        if (highest >= 0) {
            return highest;
        }
        return plugin.getConfigManager().defaultHomeLimit();
    }

    public void setBackLocation(UUID uuid, Location loc) {
        if (loc != null && loc.getWorld() != null) {
            backLocations.put(uuid, loc.clone());
        }
    }

    public Location getBackLocation(UUID uuid) {
        return backLocations.get(uuid);
    }

    public boolean hasBackLocation(UUID uuid) {
        return backLocations.containsKey(uuid);
    }

    public void clearBackLocation(UUID uuid) {
        backLocations.remove(uuid);
    }
}
