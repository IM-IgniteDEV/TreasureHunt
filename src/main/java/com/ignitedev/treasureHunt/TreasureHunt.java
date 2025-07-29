package com.ignitedev.treasureHunt;

import com.ignitedev.treasureHunt.command.TreasureCommand;
import com.ignitedev.treasureHunt.config.BaseConfiguration;
import com.ignitedev.treasureHunt.database.SQLDatabaseManager;
import com.ignitedev.treasureHunt.listener.*;
import com.ignitedev.treasureHunt.repository.TreasureRepository;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureHunt extends JavaPlugin {

  private SQLDatabaseManager sqlDatabaseManager;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    BaseConfiguration baseConfiguration = new BaseConfiguration(getConfig());

    this.sqlDatabaseManager =
        new SQLDatabaseManager(
            baseConfiguration.getHost(),
            baseConfiguration.getPort(),
            baseConfiguration.getDatabase(),
            baseConfiguration.getUsername(),
            baseConfiguration.getPassword(),
            this);

    TreasureRepository.initialize(this.sqlDatabaseManager);

    TreasureRepository treasureRepository = TreasureRepository.get();
    treasureRepository.loadAllTreasures();

    // Register listeners
    PluginManager pluginManager = getServer().getPluginManager();

    pluginManager.registerEvents(new TreasureSelectionListener(), this);
    pluginManager.registerEvents(new TreasureInteractListener(baseConfiguration, this), this);
    pluginManager.registerEvents(new GuiListener(), this);

    // Register commands
    getCommand("treasure")
        .setExecutor(
            new TreasureCommand( treasureRepository, sqlDatabaseManager, baseConfiguration));
  }

  @Override
  public void onDisable() {
    if (this.sqlDatabaseManager != null) {
      this.sqlDatabaseManager.close();
    }
  }
}
