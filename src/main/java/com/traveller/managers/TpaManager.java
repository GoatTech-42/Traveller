package com.traveller.managers;

import com.traveller.Traveller;
import com.traveller.config.ConfigManager;
import com.traveller.model.TpaRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TpaManager {

    private final Traveller plugin;
    private final ConfigManager cfg;
    private final Map<UUID, List<TpaRequest>> incoming = new ConcurrentHashMap<>();
    private BukkitTask expiryTask;

    public TpaManager(Traveller plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        startExpiryTask();
    }

    private void startExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }
        expiryTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::checkExpiry, 20L, 20L);
    }

    private void checkExpiry() {
        long timeoutMillis = (long) cfg.tpaTimeoutSeconds() * 1000L;
        for (Map.Entry<UUID, List<TpaRequest>> entry : incoming.entrySet()) {
            Iterator<TpaRequest> it = entry.getValue().iterator();
            while (it.hasNext()) {
                TpaRequest req = it.next();
                if (!req.isExpired(timeoutMillis)) {
                    continue;
                }
                it.remove();
                if (!cfg.tpaNotifyOnExpire()) {
                    continue;
                }
                Player requester = plugin.getServer().getPlayer(req.getRequester());
                Player target = plugin.getServer().getPlayer(req.getTarget());
                if (requester != null) {
                    cfg.send(requester, "tpa-expired-requester",
                            "player", target != null ? target.getName() : "player");
                }
                if (target != null) {
                    cfg.send(target, "tpa-expired-target",
                            "player", requester != null ? requester.getName() : "player");
                }
            }
        }
    }

    public boolean hasOutgoing(UUID requester) {
        for (List<TpaRequest> list : incoming.values()) {
            for (TpaRequest req : list) {
                if (req.getRequester().equals(requester)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TpaRequest getOutgoingTo(UUID requester, UUID target) {
        List<TpaRequest> list = incoming.get(target);
        if (list == null) {
            return null;
        }
        for (TpaRequest req : list) {
            if (req.getRequester().equals(requester)) {
                return req;
            }
        }
        return null;
    }

    public void cancelOutgoing(UUID requester) {
        for (List<TpaRequest> list : incoming.values()) {
            list.removeIf(req -> req.getRequester().equals(requester));
        }
    }

    public void addRequest(TpaRequest request) {
        if (cfg.oneRequestAtATime()) {
            cancelOutgoing(request.getRequester());
        }
        incoming.computeIfAbsent(request.getTarget(), k -> new ArrayList<>()).add(request);
    }

    public List<TpaRequest> getIncoming(UUID target) {
        return incoming.getOrDefault(target, new ArrayList<>());
    }

    public TpaRequest getRequestToHandle(UUID target, UUID requesterUuid) {
        List<TpaRequest> list = incoming.get(target);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (requesterUuid != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).getRequester().equals(requesterUuid)) {
                    return list.get(i);
                }
            }
            return null;
        }
        if (cfg.acceptMostRecent()) {
            return list.get(list.size() - 1);
        }
        return list.size() == 1 ? list.get(0) : null;
    }

    public void removeRequest(TpaRequest request) {
        List<TpaRequest> list = incoming.get(request.getTarget());
        if (list != null) {
            list.remove(request);
        }
    }

    public void clearForPlayer(UUID uuid) {
        incoming.remove(uuid);
        cancelOutgoing(uuid);
    }

    public void shutdown() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }
        incoming.clear();
    }
}
