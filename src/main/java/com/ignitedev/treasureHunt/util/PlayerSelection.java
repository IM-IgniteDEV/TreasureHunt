package com.ignitedev.treasureHunt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerSelection {

  private static final Map<UUID, Consumer<Location>> pendingSelections = new HashMap<>();

  public static void awaitSelection(Player player, Consumer<Location> callback) {
    pendingSelections.put(player.getUniqueId(), callback);
  }

  public static void handleSelection(Player player, Location location) {
    Consumer<Location> callback = pendingSelections.remove(player.getUniqueId());
    if (callback != null) {
      callback.accept(location);
    }
  }

  public static boolean hasPendingSelection(Player player) {
    return pendingSelections.containsKey(player.getUniqueId());
  }

  public static void clearSelection(Player player) {
    pendingSelections.remove(player.getUniqueId());
  }
}
