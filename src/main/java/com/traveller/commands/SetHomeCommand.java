package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.HomeManager;
import java.util.regex.Pattern;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand implements CommandExecutor {

    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    private final ConfigManager cfg;
    private final HomeManager homeManager;

    public SetHomeCommand(Traveller plugin) {
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
        if (!player.hasPermission("traveller.sethome")) {
            cfg.send(player, "no-permission");
            return true;
        }

        String name = args.length >= 1 ? args[0] : cfg.defaultHomeName();

        if (name.length() > cfg.maxHomeNameLength()) {
            cfg.send(player, "home-name-too-long", "max", String.valueOf(cfg.maxHomeNameLength()));
            return true;
        }
        if (cfg.enforceSafeNames() && !SAFE_NAME.matcher(name).matches()) {
            cfg.send(player, "home-name-invalid");
            return true;
        }

        boolean exists = homeManager.hasHome(player.getUniqueId(), name);
        if (exists && !cfg.allowOverwrite()) {
            cfg.send(player, "home-already-exists", "home", name);
            return true;
        }

        if (!exists) {
            int limit = homeManager.getHomeLimit(player);
            int count = homeManager.getHomeCount(player.getUniqueId());
            if (count >= limit) {
                String limitStr = limit == Integer.MAX_VALUE ? "unlimited" : String.valueOf(limit);
                cfg.send(player, "home-limit-reached", "limit", limitStr);
                return true;
            }
        }

        homeManager.setHome(player.getUniqueId(), name, player.getLocation());
        homeManager.save();

        cfg.send(player, exists ? "home-overwritten" : "home-set", "home", name);
        return true;
    }
}
