package com.ignitedev.treasureHunt.listener;

import com.ignitedev.treasureHunt.util.PlayerSelection;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TreasureSelectionListener implements Listener {

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_BLOCK
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    if (PlayerSelection.hasPendingSelection(player)) {
      event.setCancelled(true);
      Block clickedBlock = event.getClickedBlock();
      if (clickedBlock != null && !clickedBlock.getType().isAir()) {
        PlayerSelection.handleSelection(player, clickedBlock.getLocation());
      }
    }
  }
}
