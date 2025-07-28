package com.ignitedev.treasureHunt;

import com.ignitedev.treasureHunt.command.TreasureCommand;
import com.ignitedev.treasureHunt.config.BaseConfiguration;
import com.ignitedev.treasureHunt.database.SQLDatabaseManager;
import com.ignitedev.treasureHunt.listener.TreasureSelectionListener;
import com.ignitedev.treasureHunt.repository.TreasureRepository;
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
    TreasureRepository.get().loadAllTreasures();
    
    // Register listeners
    getServer().getPluginManager().registerEvents(new TreasureSelectionListener(this), this);
    
    // Register commands
    getCommand("treasure").setExecutor(new TreasureCommand(this, baseConfiguration));
  }

  @Override
  public void onDisable() {
    if (this.sqlDatabaseManager != null) {
      this.sqlDatabaseManager.close();
    }
  }
}
