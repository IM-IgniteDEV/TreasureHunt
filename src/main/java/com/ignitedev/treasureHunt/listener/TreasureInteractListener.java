package com.ignitedev.treasureHunt.listener;

import com.ignitedev.treasureHunt.TreasureHunt;
import com.ignitedev.treasureHunt.config.BaseConfiguration;
import com.ignitedev.treasureHunt.repository.TreasureRepository;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@RequiredArgsConstructor
public class TreasureInteractListener implements Listener {

  private final BaseConfiguration configuration;
  private final TreasureHunt plugin;

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Block clickedBlock = event.getClickedBlock();
    TreasureRepository treasureRepository = TreasureRepository.get();
    Player player = event.getPlayer();

    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    treasureRepository
        .getTreasureByBlock(clickedBlock)
        .ifPresent(
            treasure -> {
              if (!player.hasPermission("treasurehunt.use")) {
                player.sendMessage(
                    configuration.getPrefix().append(configuration.getNoPermissionMessage()));
                return;
              }
              treasureRepository
                  .hasPlayerFoundTreasure(player.getUniqueId(), treasure.id())
                  .whenComplete(
                      (found, throwable) -> {
                        if (found) {
                          player.sendMessage(
                              configuration.getPrefix().append(configuration.getAlreadyFound()));
                        } else {
                          Bukkit.getScheduler()
                              .runTask(
                                  plugin,
                                  () -> {
                                    treasure.grantTreasure(player);
                                    treasureRepository.markTreasureAsFound(
                                        player.getUniqueId(), treasure.id());
                                  });
                        }
                      });
            });
  }
}
