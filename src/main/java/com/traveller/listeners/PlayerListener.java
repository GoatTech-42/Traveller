package com.traveller.listeners;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final Traveller plugin;
    private final ConfigManager cfg;

    public PlayerListener(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!cfg.isFeatureEnabled("spawn") || !cfg.spawnTeleportOnFirstJoin()) {
            return;
        }
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }
        Location spawn = cfg.getSpawnLocation();
        if (spawn != null) {
            // Teleport a tick later so it runs after the join completes.
            plugin.getServer().getScheduler()
                    .runTask(plugin, () -> event.getPlayer().teleport(spawn));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!cfg.isFeatureEnabled("spawn") || !cfg.spawnTeleportOnRespawn()) {
            return;
        }
        Location spawn = cfg.getSpawnLocation();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!cfg.isFeatureEnabled("back") || !cfg.backSaveOnDeath()) {
            return;
        }
        plugin.getHomeManager().setBackLocation(
                event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }
}
