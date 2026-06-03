package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {

    private final ConfigManager cfg;

    public SetSpawnCommand(Traveller plugin) {
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
        if (!player.hasPermission("traveller.setspawn")) {
            cfg.send(player, "no-permission");
            return true;
        }
        cfg.setSpawnLocation(player.getLocation());
        cfg.send(player, "spawn-set");
        return true;
    }
}
