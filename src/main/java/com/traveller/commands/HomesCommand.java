package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.HomeManager;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCommand implements CommandExecutor {

    private final ConfigManager cfg;
    private final HomeManager homeManager;

    public HomesCommand(Traveller plugin) {
        this.cfg = plugin.getConfigManager();
        this.homeManager = plugin.getHomeManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!cfg.isFeatureEnabled("homes")) {
            cfg.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            cfg.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("traveller.homes")) {
            cfg.send(player, "no-permission");
            return true;
        }

        List<String> names = homeManager.getHomeNames(player.getUniqueId());
        if (names.isEmpty()) {
            cfg.send(player, "home-list-empty");
            return true;
        }

        int limit = homeManager.getHomeLimit(player);
        String limitStr = limit == Integer.MAX_VALUE ? "\u221e" : String.valueOf(limit);

        Component line = cfg.messageNoPrefix("home-list-header",
                "count", String.valueOf(names.size()), "limit", limitStr);
        Component separator = cfg.messageNoPrefix("home-list-separator");

        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                line = line.append(separator);
            }
            line = line.append(cfg.messageNoPrefix("home-list-entry", "home", names.get(i)));
        }
        player.sendMessage(line);
        return true;
    }
}
