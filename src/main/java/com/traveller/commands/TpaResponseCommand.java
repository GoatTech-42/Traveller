package com.traveller.commands;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.managers.TpaManager;
import com.traveller.model.TpaRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /tpaccept, /tpdeny and /tpcancel. All three share enough state that
 * it's simpler to keep them in one executor.
 */
public class TpaResponseCommand implements TabExecutor {

    private final Traveller plugin;
    private final ConfigManager cfg;
    private final TpaManager tpaManager;

    public TpaResponseCommand(Traveller plugin) {
        this.plugin = plugin;
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

        switch (command.getName().toLowerCase()) {
            case "tpaccept" -> handleAccept(player, args);
            case "tpdeny" -> handleDeny(player, args);
            case "tpcancel" -> handleCancel(player);
            default -> { /* not reachable */ }
        }
        return true;
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("traveller.tpaccept")) {
            cfg.send(player, "no-permission");
            return;
        }
        UUID requesterUuid = resolveRequester(args);
        TpaRequest request = tpaManager.getRequestToHandle(player.getUniqueId(), requesterUuid);
        if (request == null) {
            cfg.send(player, "tpa-no-pending");
            return;
        }

        Player requester = Bukkit.getPlayer(request.getRequester());
        Player target = Bukkit.getPlayer(request.getTarget());
        if (requester == null || target == null) {
            cfg.send(player, "tpa-no-pending");
            tpaManager.removeRequest(request);
            return;
        }

        tpaManager.removeRequest(request);

        // For /tpa the requester moves; for /tpahere the target moves.
        Player mover = request.getType() == TpaRequest.Type.TPA ? requester : target;
        Location destination = request.getType() == TpaRequest.Type.TPA
                ? target.getLocation() : requester.getLocation();

        cfg.send(target, "tpa-accepted-target", "player", requester.getName());
        cfg.send(requester, "tpa-accepted-requester", "player", target.getName());

        plugin.getTeleportManager().beginTeleport(mover, "tpa", destination, p -> p.teleport(destination));
    }

    private void handleDeny(Player player, String[] args) {
        if (!player.hasPermission("traveller.tpdeny")) {
            cfg.send(player, "no-permission");
            return;
        }
        UUID requesterUuid = resolveRequester(args);
        TpaRequest request = tpaManager.getRequestToHandle(player.getUniqueId(), requesterUuid);
        if (request == null) {
            cfg.send(player, "tpa-no-pending");
            return;
        }
        tpaManager.removeRequest(request);
        Player requester = Bukkit.getPlayer(request.getRequester());
        cfg.send(player, "tpa-denied-target", "player", requester != null ? requester.getName() : "player");
        if (requester != null) {
            cfg.send(requester, "tpa-denied-requester", "player", player.getName());
        }
    }

    private void handleCancel(Player player) {
        if (!player.hasPermission("traveller.tpa")) {
            cfg.send(player, "no-permission");
            return;
        }
        if (!tpaManager.hasOutgoing(player.getUniqueId())) {
            cfg.send(player, "tpa-no-outgoing");
            return;
        }
        tpaManager.cancelOutgoing(player.getUniqueId());
        cfg.send(player, "tpa-cancelled");
    }

    private UUID resolveRequester(String[] args) {
        if (args.length >= 1) {
            Player p = Bukkit.getPlayerExact(args[0]);
            if (p != null) {
                return p.getUniqueId();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player) || args.length != 1) {
            return result;
        }
        if (command.getName().equalsIgnoreCase("tpcancel")) {
            return result;
        }
        String prefix = args[0].toLowerCase();
        for (TpaRequest req : tpaManager.getIncoming(player.getUniqueId())) {
            Player requester = Bukkit.getPlayer(req.getRequester());
            if (requester != null && requester.getName().toLowerCase().startsWith(prefix)) {
                result.add(requester.getName());
            }
        }
        return result;
    }
}
