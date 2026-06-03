package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.HomeManager;
import com.traveller.model.Home;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeCommand implements TabExecutor {

    private final Traveller plugin;
    private final ConfigManager cfg;
    private final HomeManager homeManager;

    public HomeCommand(Traveller plugin) {
        this.plugin = plugin;
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
        if (!player.hasPermission("traveller.home")) {
            cfg.send(player, "no-permission");
            return true;
        }

        String name = args.length >= 1 ? args[0] : cfg.defaultHomeName();
        Home home = homeManager.getHome(player.getUniqueId(), name);

        // If they typed just /home and only own one home, send them there.
        if (home == null && args.length == 0) {
            List<String> names = homeManager.getHomeNames(player.getUniqueId());
            if (names.size() == 1) {
                name = names.get(0);
                home = homeManager.getHome(player.getUniqueId(), name);
            }
        }

        if (home == null) {
            cfg.send(player, "home-not-found", "home", name);
            return true;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            cfg.send(player, "home-not-found", "home", name);
            return true;
        }

        String finalName = name;
        plugin.getTeleportManager().beginTeleport(player, "homes", loc, p -> {
            p.teleport(loc);
            cfg.send(p, "home-teleport", "home", finalName);
        });
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        String prefix = args[0].toLowerCase();
        for (String name : homeManager.getHomeNames(player.getUniqueId())) {
            if (name.toLowerCase().startsWith(prefix)) {
                result.add(name);
            }
        }
        return result;
    }
}
