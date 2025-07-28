package com.ignitedev.treasureHunt.database;

import com.ignitedev.treasureHunt.TreasureHunt;
import com.ignitedev.treasureHunt.base.Treasure;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public class SQLDatabaseManager {
  private final HikariDataSource dataSource;
  private final TreasureHunt plugin;

  public SQLDatabaseManager(
      String host,
      int port,
      String database,
      String username,
      String password,
      TreasureHunt plugin) {
    this.plugin = plugin;
    HikariConfig config = new HikariConfig();

    config.setDriverClassName("com.mysql.cj.jdbc.MysqlDataSource");
    config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
    config.setUsername(username);
    config.setPassword(password);

    config.addDataSourceProperty("cachePrepstatements", "true");
    config.addDataSourceProperty("prepstatementCacheSize", "25");
    config.addDataSourceProperty("prepstatementCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(config);
    createTablesIfNotExist();
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

  public void saveTreasureLocation(String id, String world, double x, double y, double z) {
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
      plugin.getLogger().severe("Failed to save treasure location: " + exception.getMessage());
    }
  }

  public void deleteTreasureLocation(String id) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM treasure_locations WHERE id = ?")) {
      statement.setString(1, id);
      statement.executeUpdate();
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to delete treasure location: " + exception.getMessage());
    }
  }

  // Treasure Rewards CRUD
  public void addTreasureReward(String treasureId, String command) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO treasure_rewards (treasure_id, command) VALUES (?, ?)")) {
      statement.setString(1, treasureId);
      statement.setString(2, command);
      statement.executeUpdate();
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to add treasure reward: " + exception.getMessage());
    }
  }

  public List<String> getTreasureRewards(String treasureId) {
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
      plugin.getLogger().severe("Failed to get treasure rewards: " + exception.getMessage());
    }
    return rewards;
  }

  public void clearTreasureRewards(String treasureId) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM treasure_rewards WHERE treasure_id = ?")) {
      statement.setString(1, treasureId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to clear treasure rewards: " + exception.getMessage());
    }
  }

  // Found Treasures CRUD
  public void markTreasureAsFound(UUID playerUuid, String treasureId) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT IGNORE INTO found_treasures (player_uuid, treasure_id) VALUES (?, ?)")) {
      statement.setString(1, playerUuid.toString());
      statement.setString(2, treasureId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to mark treasure as found: " + exception.getMessage());
    }
  }

  public boolean hasPlayerFoundTreasure(UUID playerUuid, String treasureId) {
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
      plugin
          .getLogger()
          .severe("Failed to check if player found treasure: " + exception.getMessage());
      return false;
    }
  }

  public List<String> getFoundTreasures(UUID playerUuid) {
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
      plugin.getLogger().severe("Failed to get found treasures: " + exception.getMessage());
    }
    return foundTreasures;
  }

  public Map<String, Treasure> loadAllTreasures() {
    Map<String, Treasure> treasures = new HashMap<>();

    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery("SELECT id, world, x, y, z FROM treasure_locations")) {

      while (resultSet.next()) {
        String treasureId = resultSet.getString("id");
        List<String> rewards = getTreasureRewards(treasureId);

        treasures.put(treasureId, new Treasure(treasureId, rewards));
      }
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to load treasures: " + exception.getMessage());
    }
    return treasures;
  }

  public List<UUID> getPlayersWhoFoundTreasure(String treasureId) {
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
            plugin.getLogger().warning("Invalid UUID found in database: " + resultSet.getString("player_uuid"));
          }
        }
      }
    } catch (SQLException exception) {
      plugin.getLogger().severe("Failed to get players who found treasure: " + exception.getMessage());
    }
    return playerUuids;
  }

  public void close() {
    if (dataSource != null) {
      dataSource.close();
    }
  }
}
