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
  private Component foundMessage;
  private Component noPermissionMessage;

  // sql

  public BaseConfiguration(FileConfiguration configuration) {
    this.fileConfiguration = configuration;

    loadMessages();
  }

  private void loadMessages() {
    ConfigurationSection messages = this.fileConfiguration.getConfigurationSection("messages");

    if (messages == null) {
      messages = this.fileConfiguration.createSection("messages");
    }
    this.prefix = getMessage(messages, "prefix", "&7[&6TreasureHunt&7] &f");
    this.foundMessage = getMessage(messages, "found", "&aYou found a treasure!");
    this.noPermissionMessage =
        getMessage(messages, "noPermission", "&cYou do not have permission to use this command");
  }

  private Component getMessage(
      ConfigurationSection configurationSection, String path, @Nullable String def) {
    String string = configurationSection.getString(path);

    if (string == null) {
      string = def;
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
