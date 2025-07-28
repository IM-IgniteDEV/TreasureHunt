package com.ignitedev.treasureHunt.base;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public class Treasure {
  private final String id;
  private final List<String> rewardCommands;

  public void grantTreasure(Player player) {
    rewardCommands.forEach(
        command ->
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), command.replace("%player%", player.getName())));
  }
}
