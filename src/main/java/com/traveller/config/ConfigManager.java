package com.traveller.config;

import com.traveller.Traveller;
import com.traveller.util.TextUtil;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Traveller plugin;
    private FileConfiguration config;

    public ConfigManager(Traveller plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return config;
    }

    public boolean isFeatureEnabled(String feature) {
        return config.getBoolean("features." + feature, true);
    }

    public int warmup(String feature) {
        int override = config.getInt(feature + ".warmup-seconds", -1);
        if (override >= 0) {
            return override;
        }
        return Math.max(0, config.getInt("teleport.warmup-seconds", 3));
    }

    public int cooldown(String feature) {
        int override = config.getInt(feature + ".cooldown-seconds", -1);
        if (override >= 0) {
            return override;
        }
        return Math.max(0, config.getInt("teleport.cooldown-seconds", 5));
    }

    public boolean cancelOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }

    public boolean cancelOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }

    public boolean soundsEnabled() {
        return config.getBoolean("teleport.sounds.enabled", true);
    }

    public String sound(String key, String def) {
        return config.getString("teleport.sounds." + key, def);
    }

    public boolean particlesEnabled() {
        return config.getBoolean("teleport.particles.enabled", true);
    }

    public String particleEffect() {
        return config.getString("teleport.particles.effect", "PORTAL");
    }

    public int particleCount() {
        return config.getInt("teleport.particles.count", 40);
    }

    public boolean countdownEnabled() {
        return config.getBoolean("teleport.countdown.enabled", true);
    }

    public String countdownType() {
        return config.getString("teleport.countdown.type", "ACTIONBAR").toUpperCase();
    }

    public boolean spawnTeleportOnFirstJoin() {
        return config.getBoolean("spawn.teleport-on-first-join", false);
    }

    public boolean spawnTeleportOnRespawn() {
        return config.getBoolean("spawn.teleport-on-respawn", false);
    }

    public Location getSpawnLocation() {
        String worldName = config.getString("spawn.location.world", "");
        World world = null;
        if (worldName != null && !worldName.isEmpty()) {
            world = plugin.getServer().getWorld(worldName);
        }
        if (world == null) {
            return null;
        }
        return new Location(world,
                config.getDouble("spawn.location.x", 0.5),
                config.getDouble("spawn.location.y", 64.0),
                config.getDouble("spawn.location.z", 0.5),
                (float) config.getDouble("spawn.location.yaw", 0.0),
                (float) config.getDouble("spawn.location.pitch", 0.0));
    }

    public void setSpawnLocation(Location loc) {
        config.set("spawn.location.world", loc.getWorld().getName());
        config.set("spawn.location.x", loc.getX());
        config.set("spawn.location.y", loc.getY());
        config.set("spawn.location.z", loc.getZ());
        config.set("spawn.location.yaw", loc.getYaw());
        config.set("spawn.location.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public int defaultHomeLimit() {
        return config.getInt("homes.default-limit", 3);
    }

    public String defaultHomeName() {
        return config.getString("homes.default-home-name", "home");
    }

    public boolean allowOverwrite() {
        return config.getBoolean("homes.allow-overwrite", true);
    }

    public boolean enforceSafeNames() {
        return config.getBoolean("homes.enforce-safe-names", true);
    }

    public int maxHomeNameLength() {
        return config.getInt("homes.max-name-length", 32);
    }

    public int tpaTimeoutSeconds() {
        return config.getInt("tpa.request-timeout-seconds", 60);
    }

    public boolean allowTpahere() {
        return config.getBoolean("tpa.allow-tpahere", true);
    }

    public boolean acceptMostRecent() {
        return config.getBoolean("tpa.accept-most-recent", true);
    }

    public boolean tpaNotifyOnExpire() {
        return config.getBoolean("tpa.notify-on-expire", true);
    }

    public boolean oneRequestAtATime() {
        return config.getBoolean("tpa.one-request-at-a-time", true);
    }

    public boolean backSaveOnDeath() {
        return config.getBoolean("back.save-on-death", true);
    }

    public boolean backSaveOnTeleport() {
        return config.getBoolean("back.save-on-teleport", true);
    }

    public int autoSaveMinutes() {
        return config.getInt("storage.auto-save-minutes", 5);
    }

    public String rawMessage(String key) {
        return config.getString("messages." + key, "&cMissing message: " + key);
    }

    public Component message(String key, Object... placeholders) {
        String prefix = config.getString("messages.prefix", "");
        String raw = applyPlaceholders(rawMessage(key), placeholders);
        return TextUtil.component(prefix + raw);
    }

    public Component messageNoPrefix(String key, Object... placeholders) {
        String raw = applyPlaceholders(rawMessage(key), placeholders);
        return TextUtil.component(raw);
    }

    public Component custom(String raw, Object... placeholders) {
        String prefix = config.getString("messages.prefix", "");
        return TextUtil.component(prefix + applyPlaceholders(raw, placeholders));
    }

    private String applyPlaceholders(String raw, Object... placeholders) {
        if (placeholders.length == 0) {
            return raw;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        return raw;
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(message(key, placeholders));
    }
}
