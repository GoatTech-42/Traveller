package com.traveller;

import com.traveller.commands.BackCommand;
import com.traveller.commands.DelHomeCommand;
import com.traveller.commands.HomeCommand;
import com.traveller.commands.HomesCommand;
import com.traveller.commands.SetHomeCommand;
import com.traveller.commands.SetSpawnCommand;
import com.traveller.commands.SpawnCommand;
import com.traveller.commands.TpaCommand;
import com.traveller.commands.TpaResponseCommand;
import com.traveller.commands.TravellerCommand;
import com.traveller.config.ConfigManager;
import com.traveller.listeners.PlayerListener;
import com.traveller.listeners.TeleportListener;
import com.traveller.managers.HomeManager;
import com.traveller.managers.TeleportManager;
import com.traveller.managers.TpaManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Traveller extends JavaPlugin {

    private ConfigManager configManager;
    private HomeManager homeManager;
    private TeleportManager teleportManager;
    private TpaManager tpaManager;
    private BukkitTask autoSaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        homeManager = new HomeManager(this);
        teleportManager = new TeleportManager(this);
        tpaManager = new TpaManager(this);

        registerCommands();
        registerListeners();
        startAutoSave();

        getLogger().info("Traveller v" + getPluginMeta().getVersion() + " enabled.");
        logFeatureStatus();
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (teleportManager != null) {
            teleportManager.shutdown();
        }
        if (tpaManager != null) {
            tpaManager.shutdown();
        }
        if (homeManager != null) {
            homeManager.save();
        }
        getLogger().info("Traveller disabled. Player data saved.");
    }

    private void registerCommands() {
        bind("spawn", new SpawnCommand(this));
        bind("setspawn", new SetSpawnCommand(this));
        bind("home", new HomeCommand(this));
        bind("sethome", new SetHomeCommand(this));
        bind("delhome", new DelHomeCommand(this));
        bind("homes", new HomesCommand(this));

        TpaCommand tpaCommand = new TpaCommand(this);
        bind("tpa", tpaCommand);
        bind("tpahere", tpaCommand);

        TpaResponseCommand tpaResponse = new TpaResponseCommand(this);
        bind("tpaccept", tpaResponse);
        bind("tpdeny", tpaResponse);
        bind("tpcancel", tpaResponse);

        bind("back", new BackCommand(this));
        bind("traveller", new TravellerCommand(this));
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        int minutes = configManager.autoSaveMinutes();
        if (minutes > 0) {
            long ticks = (long) minutes * 60L * 20L;
            autoSaveTask = getServer().getScheduler()
                    .runTaskTimerAsynchronously(this, () -> homeManager.save(), ticks, ticks);
        }
    }

    public void reloadAll() {
        configManager.reload();
        startAutoSave();
        logFeatureStatus();
    }

    private void logFeatureStatus() {
        getLogger().info("Features -> spawn: " + configManager.isFeatureEnabled("spawn")
                + ", homes: " + configManager.isFeatureEnabled("homes")
                + ", tpa: " + configManager.isFeatureEnabled("tpa")
                + ", back: " + configManager.isFeatureEnabled("back"));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }
}
