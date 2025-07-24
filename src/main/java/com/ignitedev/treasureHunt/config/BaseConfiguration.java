package com.ignitedev.treasureHunt.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class BaseConfiguration {

  private final String prefix;
  private final String foundMessage;
  private final String noPermissionMessage;

  public BaseConfiguration(FileConfiguration configuration) {
    this.prefix = configuration.getString("messages.prefix", "&7[&6TreasureHunt&7] &f");
    this.foundMessage = configuration.getString("messages.found", "&aYou found a treasure!");
    this.noPermissionMessage =
        configuration.getString(
            "messages.noPermission", "&cYou do not have permission to use this command");
  }
}
