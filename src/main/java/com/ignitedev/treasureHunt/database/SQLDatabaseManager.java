package com.ignitedev.treasureHunt.database;

import com.ignitedev.treasureHunt.TreasureHunt;
import com.ignitedev.treasureHunt.base.Treasure;
import com.ignitedev.treasureHunt.exception.DatabaseOperationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SQLDatabaseManager {
  private final HikariDataSource dataSource;
  private final TreasureHunt plugin;
  private final Executor databaseExecutor;

  public SQLDatabaseManager(
      String host,
      int port,
      String database,
      String username,
      String password,
      TreasureHunt plugin) {
    this.plugin = plugin;
    this.databaseExecutor =
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "TreasureHunt-DB");
              thread.setDaemon(true);
              return thread;
            });

    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.MysqlDataSource");
    config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
    config.setUsername(username);
    config.setPassword(password);
    config.setMaximumPoolSize(10);
    config.setConnectionTimeout(30000);
    config.setLeakDetectionThreshold(60000);

    config.addDataSourceProperty("cachePrepstatements", "true");
    config.addDataSourceProperty("prepstatementCacheSize", "25");
    config.addDataSourceProperty("prepstatementCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(config);
    createTablesIfNotExist();
  }

  private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, databaseExecutor)
        .exceptionally(
            throwable -> {
              plugin.getLogger().severe("Database operation failed: " + throwable.getMessage());
              throw new DatabaseOperationException("Database operation failed", throwable);
            });
  }

  private CompletableFuture<Void> runAsync(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, databaseExecutor)
        .exceptionally(
            throwable -> {
              plugin.getLogger().severe("Database operation failed: " + throwable.getMessage());
              throw new DatabaseOperationException("Database operation failed", throwable);
            });
  }

  private void createTablesIfNotExist() {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // locations of treasures
      statement.execute(
          "CREATE TABLE IF NOT EXISTS treasure_locations ("
              + "id VARCHAR(36) PRIMARY KEY,"
              + "world VARCHAR(100) NOT NULL,"
              + "x DOUBLE NOT NULL,"
              + "y DOUBLE NOT NULL,"
              + "z DOUBLE NOT NULL"
              + ")");

      // rewards for treasures
      statement.execute(
          "CREATE TABLE IF NOT EXISTS treasure_rewards ("
              + "id INT AUTO_INCREMENT PRIMARY KEY,"
              + "treasure_id VARCHAR(36) NOT NULL,"
              + "command TEXT NOT NULL,"
              + "FOREIGN KEY (treasure_id) REFERENCES treasure_locations(id) ON DELETE CASCADE"
              + ");");

      // found treasures to track player found treasures
      statement.execute(
          "CREATE TABLE IF NOT EXISTS found_treasures ("
              + "player_uuid VARCHAR(36) NOT NULL,"
              + "treasure_id VARCHAR(36) NOT NULL,"
              + "found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
              + "PRIMARY KEY (player_uuid, treasure_id)"
              + ")");

    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to create tables: " + exception.getMessage());
    }
  }

  public CompletableFuture<Void> saveTreasureLocationAsync(
      @NotNull String id, @NotNull String world, double x, double y, double z) {
    return runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO treasure_locations (id, world, x, y, z) VALUES (?, ?, ?, ?, ?) "
                          + "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z)")) {
            statement.setString(1, id);
            statement.setString(2, world);
            statement.setDouble(3, x);
            statement.setDouble(4, y);
            statement.setDouble(5, z);
            statement.executeUpdate();
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to save treasure location", exception);
          }
        });
  }

  public CompletableFuture<Void> deleteTreasureLocationAsync(@NotNull String id) {
    return runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement("DELETE FROM treasure_locations WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to delete treasure location", exception);
          }
        });
  }

  public CompletableFuture<Void> addTreasureRewardAsync(
      @NotNull String treasureId, @NotNull String command) {
    return runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO treasure_rewards (treasure_id, command) VALUES (?, ?)")) {
            statement.setString(1, treasureId);
            statement.setString(2, command);
            statement.executeUpdate();
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to add treasure reward", exception);
          }
        });
  }

  public CompletableFuture<List<String>> getTreasureRewardsAsync(@NotNull String treasureId) {
    return supplyAsync(
        () -> {
          List<String> rewards = new ArrayList<>();
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "SELECT command FROM treasure_rewards WHERE treasure_id = ?")) {
            statement.setString(1, treasureId);
            try (ResultSet resultSet = statement.executeQuery()) {
              while (resultSet.next()) {
                rewards.add(resultSet.getString("command"));
              }
            }
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to get treasure rewards", exception);
          }
          return rewards;
        });
  }

  public CompletableFuture<Void> clearTreasureRewardsAsync(@NotNull String treasureId) {
    return runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "DELETE FROM treasure_rewards WHERE treasure_id = ?")) {
            statement.setString(1, treasureId);
            statement.executeUpdate();
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to clear treasure rewards", exception);
          }
        });
  }

  public CompletableFuture<Void> markTreasureAsFoundAsync(
      @NotNull UUID playerUuid, @NotNull String treasureId) {
    return runAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT IGNORE INTO found_treasures (player_uuid, treasure_id) VALUES (?, ?)")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, treasureId);
            statement.executeUpdate();
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to mark treasure as found", exception);
          }
        });
  }

  public CompletableFuture<Boolean> hasPlayerFoundTreasureAsync(
      @NotNull UUID playerUuid, @NotNull String treasureId) {
    return supplyAsync(
        () -> {
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "SELECT 1 FROM found_treasures WHERE player_uuid = ? AND treasure_id = ?")) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, treasureId);
            try (ResultSet resultSet = statement.executeQuery()) {
              return resultSet.next();
            }
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to check if player found treasure", exception);
          }
        });
  }

  public CompletableFuture<List<String>> getFoundTreasuresAsync(@NotNull UUID playerUuid) {
    return supplyAsync(
        () -> {
          List<String> foundTreasures = new ArrayList<>();
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "SELECT treasure_id FROM found_treasures WHERE player_uuid = ?")) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
              while (resultSet.next()) {
                foundTreasures.add(resultSet.getString("treasure_id"));
              }
            }
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to get found treasures", exception);
          }
          return foundTreasures;
        });
  }

  public CompletableFuture<Map<String, Treasure>> loadAllTreasuresAsync() {
    return supplyAsync(
        () -> {
          Map<String, Treasure> treasures = new HashMap<>();

          try (Connection connection = dataSource.getConnection();
              Statement statement = connection.createStatement();
              ResultSet resultSet =
                  statement.executeQuery("SELECT id, world, x, y, z FROM treasure_locations")) {

            // First, collect all treasure IDs
            List<String> treasureIds = new ArrayList<>();
            while (resultSet.next()) {
              String treasureId = resultSet.getString("id");
              treasureIds.add(treasureId);
              // Create treasure with empty rewards for now, we'll update them in parallel
              treasures.put(treasureId, new Treasure(treasureId, new ArrayList<>()));
            }

            // Load all rewards in parallel
            List<CompletableFuture<Void>> rewardFutures =
                treasureIds.stream()
                    .map(
                        treasureId ->
                            getTreasureRewardsAsync(treasureId)
                                .thenAccept(
                                    rewards -> {
                                      Treasure treasure = treasures.get(treasureId);
                                      if (treasure != null) {
                                        treasure.getRewardCommands().addAll(rewards);
                                      }
                                    }))
                    .toList();

            // Wait for all reward loads to complete
            CompletableFuture.allOf(rewardFutures.toArray(new CompletableFuture[0])).join();

          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to load treasures", exception);
          }

          return treasures;
        });
  }

  public CompletableFuture<List<UUID>> getPlayersWhoFoundTreasureAsync(@NotNull String treasureId) {
    return supplyAsync(
        () -> {
          List<UUID> playerUuids = new ArrayList<>();
          try (Connection connection = dataSource.getConnection();
              PreparedStatement statement =
                  connection.prepareStatement(
                      "SELECT player_uuid FROM found_treasures WHERE treasure_id = ?")) {
            statement.setString(1, treasureId);
            try (ResultSet resultSet = statement.executeQuery()) {
              while (resultSet.next()) {
                try {
                  playerUuids.add(UUID.fromString(resultSet.getString("player_uuid")));
                } catch (IllegalArgumentException exception) {
                  plugin
                      .getLogger()
                      .warning(
                          "Invalid UUID found in database: " + resultSet.getString("player_uuid"));
                }
              }
            }
          } catch (SQLException exception) {
            throw new DatabaseOperationException("Failed to get players who found treasure", exception);
          }
          return playerUuids;
        });
  }

  public void close() {
    if (dataSource != null) {
      dataSource.close();
    }
    if (databaseExecutor instanceof ExecutorService) {
      ((ExecutorService) databaseExecutor).shutdown();
    }
  }
}
