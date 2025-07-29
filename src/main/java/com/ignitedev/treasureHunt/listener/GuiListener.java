package com.ignitedev.treasureHunt.listener;

import com.ignitedev.treasureHunt.gui.TreasureGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof TreasureGui gui) {
      if (event.getClickedInventory() != event.getView().getTopInventory()) {
        return;
      }
      event.setCancelled(true);
      gui.handleClick(event);
    }
  }
}
