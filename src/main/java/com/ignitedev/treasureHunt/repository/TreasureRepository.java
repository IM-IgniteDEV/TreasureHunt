package com.ignitedev.treasureHunt.repository;

import com.ignitedev.treasureHunt.base.Treasure;
import com.ignitedev.treasureHunt.database.SQLDatabaseManager;

import java.util.*;
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
        .saveTreasureLocation(
            id,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ());

    rewardCommands.forEach(command -> this.sqlDatabaseManager.addTreasureReward(id, command));
    treasureCache.put(id, new Treasure(id, rewardCommands));
  }

  public void removeTreasure(String id) {
    this.sqlDatabaseManager.deleteTreasureLocation(id);
    treasureCache.remove(id);
  }

  public void loadAllTreasures() {
    this.treasureCache.clear();
    this.treasureCache.putAll(sqlDatabaseManager.loadAllTreasures());
  }

  public Optional<Treasure> getTreasure(String id) {
    return Optional.ofNullable(treasureCache.get(id));
  }

  public List<Treasure> getAllTreasures() {
    return new ArrayList<>(treasureCache.values());
  }

  // PLAYER INTERACTION

  public boolean hasPlayerFoundTreasure(UUID playerId, String treasureId) {
    return getTreasure(treasureId)
        .map(treasure -> sqlDatabaseManager.hasPlayerFoundTreasure(playerId, treasureId))
        .orElse(false);
  }

  public void markTreasureAsFound(UUID playerId, String treasureId) {
    getTreasure(treasureId)
        .ifPresent(treasure -> sqlDatabaseManager.markTreasureAsFound(playerId, treasureId));
  }
}
