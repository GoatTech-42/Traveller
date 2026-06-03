package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.TpaManager;
import com.traveller.model.TpaRequest;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TpaCommand implements TabExecutor {

    private final ConfigManager cfg;
    private final TpaManager tpaManager;

    public TpaCommand(Traveller plugin) {
        this.cfg = plugin.getConfigManager();
        this.tpaManager = plugin.getTpaManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!cfg.isFeatureEnabled("tpa")) {
            cfg.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            cfg.send(sender, "player-only");
            return true;
        }

        boolean here = command.getName().equalsIgnoreCase("tpahere");
        if (here && !cfg.allowTpahere()) {
            cfg.send(player, "feature-disabled");
            return true;
        }

        String perm = here ? "traveller.tpahere" : "traveller.tpa";
        if (!player.hasPermission(perm)) {
            cfg.send(player, "no-permission");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(cfg.custom("&cUsage: &e/" + command.getName() + " <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            cfg.send(player, "player-not-found", "player", args[0]);
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            cfg.send(player, "tpa-self");
            return true;
        }
        if (tpaManager.getOutgoingTo(player.getUniqueId(), target.getUniqueId()) != null) {
            cfg.send(player, "tpa-already-sent", "player", target.getName());
            return true;
        }

        TpaRequest.Type type = here ? TpaRequest.Type.TPAHERE : TpaRequest.Type.TPA;
        tpaManager.addRequest(new TpaRequest(player.getUniqueId(), target.getUniqueId(), type));

        if (here) {
            cfg.send(player, "tpahere-sent", "player", target.getName());
            cfg.send(target, "tpahere-received", "player", player.getName());
        } else {
            cfg.send(player, "tpa-sent", "player", target.getName());
            cfg.send(target, "tpa-received", "player", player.getName());
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        String prefix = args[0].toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player s && p.getUniqueId().equals(s.getUniqueId())) {
                continue;
            }
            if (p.getName().toLowerCase().startsWith(prefix)) {
                result.add(p.getName());
            }
        }
        return result;
    }
}
