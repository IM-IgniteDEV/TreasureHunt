package com.ignitedev.treasureHunt.base;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record Treasure(String id, Location location, List<String> rewardCommands) {
  public void grantTreasure(Player player) {
    rewardCommands.forEach(
        command ->
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), command.replace("%player%", player.getName())));
  }
}
