package com.ignitedev.treasureHunt.gui;

import com.ignitedev.treasureHunt.base.Treasure;
import com.ignitedev.treasureHunt.config.BaseConfiguration;
import com.ignitedev.treasureHunt.repository.TreasureRepository;
import com.ignitedev.treasureHunt.util.BaseUtility;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class TreasureGui implements InventoryHolder {

  private final Inventory inventory;
  private final TreasureRepository treasureRepository;
  private final BaseConfiguration config;
  private final UUID viewerId;
  private int currentPage = 1;
  private static final int PAGE_SIZE = 45;

  public TreasureGui(
      Player player, TreasureRepository treasureRepository, BaseConfiguration config) {
    this.treasureRepository = treasureRepository;
    this.config = config;
    this.viewerId = player.getUniqueId();
    this.inventory =
        Bukkit.createInventory(
            this,
            54,
            BaseUtility.colorize("<gradient:gold:yellow>Treasure Manager - Page 1</gradient>"));
    loadTreasures();
  }

  private void loadTreasures() {
    List<Treasure> treasures = treasureRepository.getAllTreasures();
    int totalPages = Math.max(1, (int) Math.ceil((double) treasures.size() / PAGE_SIZE));

    currentPage = Math.max(1, Math.min(currentPage, totalPages));

    for (int i = 0; i < PAGE_SIZE; i++) {
      inventory.setItem(i, null);
    }

    if (!treasures.isEmpty()) {
      int startIndex = (currentPage - 1) * PAGE_SIZE;
      int endIndex = Math.min(startIndex + PAGE_SIZE, treasures.size());

      for (int i = startIndex; i < endIndex; i++) {
        Treasure treasure = treasures.get(i);
        inventory.setItem(i - startIndex, createTreasureItem(treasure));
      }
    }
    setupNavigation(totalPages);
  }

  private ItemStack createTreasureItem(Treasure treasure) {
    ItemStack item = new ItemStack(Material.CHEST);
    ItemMeta meta = item.getItemMeta();

    meta.displayName(
        Component.text("Treasure: " + treasure.id())
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
    List<Component> lore = new ArrayList<>();

    lore.add(
        Component.text(
                "Location: "
                    + String.format(
                        "%.0f, %.0f, %.0f in %s",
                        treasure.location().getX(),
                        treasure.location().getY(),
                        treasure.location().getZ(),
                        treasure.location().getWorld().getName()))
            .color(NamedTextColor.GRAY));

    lore.add(Component.text(""));
    lore.add(Component.text("Commands:").color(NamedTextColor.YELLOW));

    for (String cmd : treasure.rewardCommands()) {
      lore.add(Component.text("- " + cmd).color(NamedTextColor.GRAY));
    }
    lore.add(Component.text(""));
    lore.add(Component.text("Click to teleport").color(NamedTextColor.YELLOW));
    lore.add(Component.text("Shift+Click to delete").color(NamedTextColor.RED));

    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  private void setupNavigation(int totalPages) {
    if (currentPage > 1) {
      ItemStack prevButton = new ItemStack(Material.ARROW);
      ItemMeta prevMeta = prevButton.getItemMeta();

      prevMeta.displayName(Component.text("Previous Page"));
      prevButton.setItemMeta(prevMeta);
      inventory.setItem(48, prevButton);
    }
    if (currentPage < totalPages) {
      ItemStack nextButton = new ItemStack(Material.ARROW);
      ItemMeta nextMeta = nextButton.getItemMeta();

      nextMeta.displayName(Component.text("Next Page"));
      nextButton.setItemMeta(nextMeta);
      inventory.setItem(50, nextButton);
    }
    ItemStack closeButton = new ItemStack(Material.BARRIER);
    ItemMeta closeMeta = closeButton.getItemMeta();

    closeMeta.displayName(Component.text("Close", NamedTextColor.RED));
    closeButton.setItemMeta(closeMeta);
    inventory.setItem(53, closeButton);
  }

  public void handleClick(InventoryClickEvent event) {
    event.setCancelled(true);
    
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    ItemStack clicked = event.getCurrentItem();

    if (clicked == null || clicked.getType() == Material.AIR) {
      return;
    }
    int clickedSlot = event.getSlot();
    
    if (clickedSlot >= 45) {
      if (clickedSlot == 48 && currentPage > 1) {
        currentPage--;
        updateTitle();
        loadTreasures();
      } else if (clickedSlot == 50) {
        List<Treasure> allTreasures = treasureRepository.getAllTreasures();
        int totalPages = Math.max(1, (int) Math.ceil((double) allTreasures.size() / PAGE_SIZE));

        if (currentPage < totalPages) {
          currentPage++;
          updateTitle();
          loadTreasures();
        }
      } else if (clickedSlot == 53) {
        player.closeInventory();
      }
      return;
    }
    if (clicked.getType() == Material.CHEST && clicked.hasItemMeta()) {
      ItemMeta meta = clicked.getItemMeta();
      if (meta.hasDisplayName() && meta.getDisplayName().contains("Treasure: ")) {
        String treasureId = meta.getDisplayName().split(": ")[1];
        
        if (event.isShiftClick()) {
          treasureRepository.removeTreasure(treasureId);
          player.sendMessage(
              config.getPrefix()
                  .append(Component.text("Treasure ", NamedTextColor.GREEN)
                  .append(Component.text(treasureId, NamedTextColor.YELLOW))
                  .append(Component.text(" deleted!"))));
          loadTreasures();
        } else {
          treasureRepository.getTreasure(treasureId).ifPresent(treasure -> {
            player.teleport(treasure.location());
            player.sendMessage(
                config.getPrefix()
                    .append(Component.text("Teleported to treasure ", NamedTextColor.GREEN)
                    .append(Component.text(treasureId, NamedTextColor.YELLOW))));
          });
        }
      }
    }
  }

  private void updateTitle() {
    Inventory newInventory =
        Bukkit.createInventory(
            this,
            54,
            MiniMessage.miniMessage()
                .deserialize(
                    "<gradient:gold:yellow>Treasure Manager - Page "
                        + currentPage
                        + "</gradient>"));

    for (int i = 0; i < inventory.getSize(); i++) {
      newInventory.setItem(i, inventory.getItem(i));
    }
    Player player = Bukkit.getPlayer(viewerId);

    if (player != null) {
      player.openInventory(newInventory);
    }
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }
}
