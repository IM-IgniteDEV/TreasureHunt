package com.ignitedev.treasureHunt.config;

import com.ignitedev.treasureHunt.util.BaseUtility;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

@Getter
public class BaseConfiguration {

  private final FileConfiguration fileConfiguration;

  // messages

  private Component prefix;
  private Component alreadyFound;
  private Component foundMessage;
  private Component noPermissionMessage;
  private Component playerOnlyMessage;

  // sql

  private int port;
  private String host;
  private String database;
  private String username;
  private String password;

  public BaseConfiguration(FileConfiguration configuration) {
    this.fileConfiguration = configuration;

    loadMessages();
    loadSql();
  }

  private void loadMessages() {
    ConfigurationSection messages = this.fileConfiguration.getConfigurationSection("messages");

    if (messages == null) {
      messages = this.fileConfiguration.createSection("messages");
    }
    this.alreadyFound = getMessage(messages, "alreadyFound", "&cYou have already found this treasure");
    this.prefix = getMessage(messages, "prefix", "&7[&6TreasureHunt&7] &f");
    this.foundMessage = getMessage(messages, "found", "&aYou found a treasure!");
    this.noPermissionMessage =
        getMessage(messages, "noPermission", "&cYou do not have permission to use this command");
    this.playerOnlyMessage =
        getMessage(messages, "playerOnly", "&cThis command can only be used by players");
  }

  private void loadSql() {
    ConfigurationSection sql = this.fileConfiguration.getConfigurationSection("sql");

    if (sql == null) {
      sql = this.fileConfiguration.createSection("sql");
    }
    this.port = sql.getInt("port");
    this.host = sql.getString("host");
    this.database = sql.getString("database");
    this.username = sql.getString("username");
    this.password = sql.getString("password");
  }

  private Component getMessage(
      ConfigurationSection configurationSection, String path, @Nullable String def) {
    String string = configurationSection.getString(path, def);
    if (string == null) {
      return Component.empty();
    }
    return BaseUtility.colorize(string);
  }

  private Sound getSound(
      ConfigurationSection configurationSection, String path, @Nullable Sound def) {
    Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(path));

    if (sound == null) {
      sound = def;
    }
    return sound;
  }
}
