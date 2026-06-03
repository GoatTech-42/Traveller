package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {

    private final Traveller plugin;
    private final ConfigManager cfg;

    public SpawnCommand(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!cfg.isFeatureEnabled("spawn")) {
            cfg.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            cfg.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("traveller.spawn")) {
            cfg.send(player, "no-permission");
            return true;
        }
        Location spawn = cfg.getSpawnLocation();
        if (spawn == null) {
            cfg.send(player, "spawn-not-set");
            return true;
        }
        plugin.getTeleportManager().beginTeleport(player, "spawn", spawn, p -> {
            p.teleport(spawn);
            cfg.send(p, "spawn-teleport");
        });
        return true;
    }
}
