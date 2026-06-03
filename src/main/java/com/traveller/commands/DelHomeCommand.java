package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.HomeManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelHomeCommand implements TabExecutor {

    private final ConfigManager cfg;
    private final HomeManager homeManager;

    public DelHomeCommand(Traveller plugin) {
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
        if (!player.hasPermission("traveller.delhome")) {
            cfg.send(player, "no-permission");
            return true;
        }

        String name = args.length >= 1 ? args[0] : cfg.defaultHomeName();
        if (homeManager.deleteHome(player.getUniqueId(), name)) {
            homeManager.save();
            cfg.send(player, "home-deleted", "home", name);
        } else {
            cfg.send(player, "home-not-found", "home", name);
        }
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
