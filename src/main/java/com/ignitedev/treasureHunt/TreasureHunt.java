package com.ignitedev.treasureHunt;

import com.ignitedev.treasureHunt.config.BaseConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureHunt extends JavaPlugin {

  @Override
  public void onEnable() {
    saveDefaultConfig();

    BaseConfiguration baseConfiguration = new BaseConfiguration();

  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }
}
