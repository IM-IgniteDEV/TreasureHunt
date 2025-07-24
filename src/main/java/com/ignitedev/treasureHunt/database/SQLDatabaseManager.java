package com.ignitedev.treasureHunt.database;

import com.ignitedev.treasureHunt.TreasureHunt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "25");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(config);
    createTablesIfNotExist();
  }

  private void createTablesIfNotExist() {
    try (Connection conn = dataSource.getConnection();
        Statement statement = conn.createStatement()) {

      statement.execute(
          "CREATE TABLE IF NOT EXISTS treasure_locations ("
              + "id VARCHAR(36) PRIMARY KEY,"
              + "world VARCHAR(100) NOT NULL,"
              + "x DOUBLE NOT NULL,"
              + "y DOUBLE NOT NULL,"
              + "z DOUBLE NOT NULL"
              + ")");

      // commands to support multiple commands
      statement.execute(
          "CREATE TABLE IF NOT EXISTS treasure_commands ("
              + "id INT AUTO_INCREMENT PRIMARY KEY,"
              + "treasure_id VARCHAR(36) NOT NULL,"
              + "command TEXT NOT NULL,"
              + "command_order INT NOT NULL,"
              + "FOREIGN KEY (treasure_id) REFERENCES treasure_locations(id) ON DELETE CASCADE,"
              + "UNIQUE KEY unique_command_order (treasure_id, command_order)"
              + ")");

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

  public void close() {
    if (dataSource != null) {
      dataSource.close();
    }
  }
}
