package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {

    private final Traveller plugin;
    private final ConfigManager cfg;

    public BackCommand(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!cfg.isFeatureEnabled("back")) {
            cfg.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            cfg.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("traveller.back")) {
            cfg.send(player, "no-permission");
            return true;
        }

        Location back = plugin.getHomeManager().getBackLocation(player.getUniqueId());
        if (back == null || back.getWorld() == null) {
            cfg.send(player, "back-none");
            return true;
        }

        Location destination = back.clone();
        plugin.getTeleportManager().beginTeleport(player, "back", destination, p -> {
            p.teleport(destination);
            cfg.send(p, "back-teleport");
        });
        return true;
    }
}
