package com.ignitedev.treasureHunt.util;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class BaseUtility {

  public @NotNull Component colorize(String message) {
    return MiniMessage.miniMessage().deserialize(message);
  }
}
