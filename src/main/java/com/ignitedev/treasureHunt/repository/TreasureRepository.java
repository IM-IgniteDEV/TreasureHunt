package com.ignitedev.treasureHunt.repository;

import com.ignitedev.treasureHunt.base.Treasure;
import com.ignitedev.treasureHunt.database.SQLDatabaseManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

@RequiredArgsConstructor
public class TreasureRepository {

  private static volatile TreasureRepository instance;

  @Getter private final Map<String, Treasure> treasureCache = new ConcurrentHashMap<>();

  private final SQLDatabaseManager sqlDatabaseManager;

  public static void initialize(SQLDatabaseManager sqlDatabaseManager) {
    if (instance == null) {
      synchronized (TreasureRepository.class) {
        if (instance == null) {
          instance = new TreasureRepository(sqlDatabaseManager);
        }
      }
    }
  }

  public static TreasureRepository get() {
    if (instance == null) {
      throw new IllegalStateException(
          "TreasureRepository not initialized. Call initialize() first.");
    }
    return instance;
  }

  // TREASURE MANAGEMENT

  public void createTreasure(Location location, String id, List<String> rewardCommands) {
    this.sqlDatabaseManager
        .saveTreasureLocationAsync(
            id, location.getWorld().getName(), location.getX(), location.getY(), location.getZ())
        .thenRun(
            () -> {
              rewardCommands.forEach(
                  command -> this.sqlDatabaseManager.addTreasureRewardAsync(id, command));
              treasureCache.put(id, new Treasure(id, location, rewardCommands));
            });
  }

  public void removeTreasure(String id) {
    this.sqlDatabaseManager.deleteTreasureLocationAsync(id).thenRun(() -> treasureCache.remove(id));
  }

  public void loadAllTreasures() {
    this.treasureCache.clear();

    this.sqlDatabaseManager
        .loadAllTreasuresAsync()
        .whenComplete(
            (treasures, exception) -> {
              this.treasureCache.putAll(treasures);
            });
  }

  public Optional<Treasure> getTreasure(String id) {
    return Optional.ofNullable(treasureCache.get(id));
  }

  public Optional<Treasure> getTreasureByBlock(org.bukkit.block.Block block) {
    if (block == null) {
      return Optional.empty();
    }
    Location blockLoc = block.getLocation();
    
    return treasureCache.values().stream()
        .filter(treasure -> {
          Location treasureLoc = treasure.location();
          return treasureLoc != null && 
                 treasureLoc.getWorld() != null &&
                 treasureLoc.getWorld().getName().equals(blockLoc.getWorld().getName()) &&
                 treasureLoc.getBlockX() == blockLoc.getBlockX() &&
                 treasureLoc.getBlockY() == blockLoc.getBlockY() &&
                 treasureLoc.getBlockZ() == blockLoc.getBlockZ();
        })
        .findFirst();
  }

  public List<Treasure> getAllTreasures() {
    return new ArrayList<>(treasureCache.values());
  }

  // PLAYER INTERACTION

  public CompletableFuture<Boolean> hasPlayerFoundTreasure(UUID playerId, String treasureId) {
    return getTreasure(treasureId)
        .map(treasure -> sqlDatabaseManager.hasPlayerFoundTreasureAsync(playerId, treasureId))
        .orElse(CompletableFuture.completedFuture(false));
  }

  public void markTreasureAsFound(UUID playerId, String treasureId) {
    getTreasure(treasureId)
        .ifPresent(treasure -> sqlDatabaseManager.markTreasureAsFoundAsync(playerId, treasureId));
  }
}
