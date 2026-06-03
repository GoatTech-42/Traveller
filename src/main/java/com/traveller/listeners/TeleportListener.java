package com.traveller.listeners;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.TeleportManager;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TeleportListener implements Listener {

    private final Traveller plugin;
    private final TeleportManager teleportManager;
    private final ConfigManager cfg;

    public TeleportListener(Traveller plugin) {
        this.plugin = plugin;
        this.teleportManager = plugin.getTeleportManager();
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!cfg.cancelOnMove()) {
            return;
        }
        Player player = event.getPlayer();
        if (!teleportManager.hasWarmup(player.getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        // Only cancel when the player actually changes block, so looking
        // around or tiny position jitters don't break the warmup.
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            teleportManager.cancelWarmupWithMessage(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!cfg.cancelOnDamage()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (teleportManager.hasWarmup(player.getUniqueId())) {
            teleportManager.cancelWarmupWithMessage(player.getUniqueId(), false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        teleportManager.cancelWarmup(uuid, false);
        teleportManager.clearCooldowns(uuid);
        plugin.getTpaManager().clearForPlayer(uuid);
    }
}
