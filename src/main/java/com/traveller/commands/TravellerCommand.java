package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TravellerCommand implements TabExecutor {

    private static final List<String> SUB_COMMANDS = Arrays.asList("reload", "info", "version");

    private final Traveller plugin;
    private final ConfigManager cfg;

    public TravellerCommand(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("traveller.admin")) {
            cfg.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAll();
                cfg.send(sender, "reload-success");
            }
            case "version", "info" -> {
                sender.sendMessage(cfg.custom("&bTraveller &7v" + plugin.getPluginMeta().getVersion()));
                sender.sendMessage(cfg.custom("&7Features: &fspawn=" + cfg.isFeatureEnabled("spawn")
                        + " homes=" + cfg.isFeatureEnabled("homes")
                        + " tpa=" + cfg.isFeatureEnabled("tpa")
                        + " back=" + cfg.isFeatureEnabled("back")));
            }
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(cfg.custom("&b&lTraveller &7Admin Commands:"));
        sender.sendMessage(cfg.custom("&e/" + label + " reload &7- Reload the configuration"));
        sender.sendMessage(cfg.custom("&e/" + label + " info &7- Show plugin info & feature status"));
        sender.sendMessage(cfg.custom("&e/" + label + " version &7- Show plugin version"));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String opt : SUB_COMMANDS) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    options.add(opt);
                }
            }
            return options;
        }
        return new ArrayList<>();
    }
}
