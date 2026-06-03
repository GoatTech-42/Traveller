package com.traveller.managers;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.NamespacedKey;

/**
 * Handles teleport warmups, cooldowns and the visual countdown.
 *
 * The warmup runs a repeating task once per second. The countdown shows the
 * number of seconds the player still has to wait, so a 5 second warmup shows
 * 5, 4, 3, 2, 1 and then teleports as the last second finishes.
 */
public class TeleportManager {

    private final Traveller plugin;
    private final ConfigManager cfg;
    private final Map<UUID, ActiveWarmup> warmups = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public TeleportManager(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    public void beginTeleport(Player player, String feature, Location destination, Consumer<Player> onSuccess) {
        if (!player.hasPermission("traveller.bypass.cooldown")) {
            long remaining = getCooldownRemaining(player.getUniqueId(), feature);
            if (remaining > 0L) {
                cfg.send(player, "cooldown-active", "seconds", String.valueOf((remaining + 999L) / 1000L));
                return;
            }
        }

        int warmup = player.hasPermission("traveller.bypass.warmup") ? 0 : cfg.warmup(feature);
        if (warmup <= 0) {
            completeTeleport(player, feature, destination, onSuccess);
            return;
        }

        // Replace any warmup already running for this player.
        cancelWarmup(player.getUniqueId(), false);

        cfg.send(player, "warmup-start", "seconds", String.valueOf(warmup));

        ActiveWarmup state = new ActiveWarmup(player.getUniqueId(), feature, destination,
                onSuccess, player.getLocation().clone(), warmup);
        warmups.put(player.getUniqueId(), state);

        // Show the first number straight away, then tick down once per second.
        showCountdown(player, warmup);
        playSound(player, cfg.sound("warmup-tick", "block.note_block.hat"));
        state.task = new WarmupTask(state).runTaskTimer(plugin, 20L, 20L);
    }

    private void completeTeleport(Player player, String feature, Location destination, Consumer<Player> onSuccess) {
        if (cfg.isFeatureEnabled("back") && cfg.backSaveOnTeleport()) {
            plugin.getHomeManager().setBackLocation(player.getUniqueId(), player.getLocation());
        }

        spawnParticles(player.getLocation());
        playSound(player, cfg.sound("success", "entity.enderman.teleport"));

        onSuccess.accept(player);

        if (destination != null) {
            spawnParticles(destination);
        }

        int cooldown = cfg.cooldown(feature);
        if (cooldown > 0 && !player.hasPermission("traveller.bypass.cooldown")) {
            cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(feature, System.currentTimeMillis() + (long) cooldown * 1000L);
        }
    }

    public boolean hasWarmup(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    public Location getWarmupOrigin(UUID uuid) {
        ActiveWarmup w = warmups.get(uuid);
        return w == null ? null : w.origin;
    }

    public void cancelWarmup(UUID uuid, boolean dueToMove) {
        ActiveWarmup state = warmups.remove(uuid);
        if (state != null && state.task != null) {
            state.task.cancel();
        }
    }

    public void cancelWarmupWithMessage(UUID uuid, boolean dueToMove) {
        ActiveWarmup state = warmups.remove(uuid);
        if (state == null) {
            return;
        }
        if (state.task != null) {
            state.task.cancel();
        }
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            cfg.send(player, dueToMove ? "warmup-cancelled-move" : "warmup-cancelled-damage");
            playSound(player, cfg.sound("cancelled", "block.note_block.bass"));
        }
    }

    public long getCooldownRemaining(UUID uuid, String feature) {
        Map<String, Long> m = cooldowns.get(uuid);
        if (m == null) {
            return 0L;
        }
        Long expiry = m.get(feature);
        if (expiry == null) {
            return 0L;
        }
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }

    private void spawnParticles(Location loc) {
        if (!cfg.particlesEnabled() || loc == null || loc.getWorld() == null) {
            return;
        }
        Particle particle = resolveParticle(cfg.particleEffect());
        if (particle == null) {
            plugin.getLogger().warning("Invalid particle effect in config: " + cfg.particleEffect());
            return;
        }
        loc.getWorld().spawnParticle(particle, loc.clone().add(0.0, 1.0, 0.0),
                cfg.particleCount(), 0.5, 1.0, 0.5, 0.01);
    }

    private Particle resolveParticle(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void playSound(Player player, String soundName) {
        if (!cfg.soundsEnabled() || soundName == null || soundName.isEmpty()) {
            return;
        }
        Sound sound = resolveSound(soundName);
        if (sound == null) {
            plugin.getLogger().warning("Invalid sound in config: " + soundName);
            return;
        }
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    /**
     * Resolves a sound from either the modern key form (block.note_block.hat)
     * or the old enum constant form (BLOCK_NOTE_BLOCK_HAT) so existing configs
     * keep working on 1.21.11.
     */
    private Sound resolveSound(String name) {
        // Modern key form, e.g. "block.note_block.hat".
        if (name.contains(".") || name.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
            if (key != null) {
                Sound sound = Registry.SOUNDS.get(key);
                if (sound != null) {
                    return sound;
                }
            }
        }
        // Old enum form, e.g. "BLOCK_NOTE_BLOCK_HAT".
        NamespacedKey legacyKey = NamespacedKey.minecraft(name.toLowerCase().replace('_', '.'));
        return Registry.SOUNDS.get(legacyKey);
    }

    private void showCountdown(Player player, int secondsLeft) {
        if (!cfg.countdownEnabled()) {
            return;
        }
        Component msg = cfg.messageNoPrefix("warmup-countdown", "seconds", String.valueOf(secondsLeft));
        switch (cfg.countdownType()) {
            case "TITLE" -> player.showTitle(Title.title(
                    Component.empty(), msg,
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100L), Duration.ZERO)));
            case "CHAT" -> player.sendMessage(msg);
            default -> player.sendActionBar(msg);
        }
    }

    public void shutdown() {
        for (UUID uuid : warmups.keySet()) {
            cancelWarmup(uuid, false);
        }
        warmups.clear();
    }

    private static final class ActiveWarmup {
        final UUID uuid;
        final String feature;
        final Location destination;
        final Consumer<Player> onSuccess;
        final Location origin;
        int secondsLeft;
        BukkitTask task;

        ActiveWarmup(UUID uuid, String feature, Location destination,
                     Consumer<Player> onSuccess, Location origin, int seconds) {
            this.uuid = uuid;
            this.feature = feature;
            this.destination = destination;
            this.onSuccess = onSuccess;
            this.origin = origin;
            this.secondsLeft = seconds;
        }
    }

    private final class WarmupTask extends BukkitRunnable {
        private final ActiveWarmup state;

        WarmupTask(ActiveWarmup state) {
            this.state = state;
        }

        @Override
        public void run() {
            Player player = plugin.getServer().getPlayer(state.uuid);
            if (player == null || !player.isOnline()) {
                cancelWarmup(state.uuid, false);
                cancel();
                return;
            }

            // One second has elapsed since the last number we showed.
            state.secondsLeft--;

            if (state.secondsLeft <= 0) {
                warmups.remove(state.uuid);
                completeTeleport(player, state.feature, state.destination, state.onSuccess);
                cancel();
                return;
            }

            showCountdown(player, state.secondsLeft);
            playSound(player, cfg.sound("warmup-tick", "block.note_block.hat"));
        }
    }
}
