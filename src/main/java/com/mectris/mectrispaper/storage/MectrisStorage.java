package com.mectris.mectrispaper.storage;

import com.mectris.mectrispaper.Mectris;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class MectrisStorage {

    private final HikariDataSource dataSource;

    public MectrisStorage() throws SQLException {
        var dataFolder = Mectris.getInstance().getDataFolder().getAbsolutePath();

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:" + dataFolder + "/data;DB_CLOSE_ON_EXIT=FALSE");
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionTimeout(5000);

        dataSource = new HikariDataSource(hikariConfig);

        initSchema();
    }

    private void initSchema() throws SQLException {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS credentials (
                        id   INT          PRIMARY KEY DEFAULT 1,
                        api_key         VARCHAR(256),
                        server_id       VARCHAR(36),
                        installation_id VARCHAR(36)
                    )
                    """);
        }
    }

    public Optional<Credentials> loadCredentials() throws SQLException {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement("SELECT api_key, server_id, installation_id FROM credentials WHERE id = 1")) {
            var rs = stmt.executeQuery();
            if (!rs.next()) return Optional.empty();

            var apiKey = rs.getString("api_key");
            var serverId = rs.getString("server_id");
            var installId = rs.getString("installation_id");

            if (apiKey == null || serverId == null || installId == null) return Optional.empty();

            return Optional.of(new Credentials(apiKey, UUID.fromString(serverId), UUID.fromString(installId)));
        }
    }

    public void saveCredentials(String apiKey, UUID serverId, UUID installationId) throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("""
                     MERGE INTO credentials (id, api_key, server_id, installation_id)
                     KEY(id)
                     VALUES (1, ?, ?, ?)
                     """)) {
            stmt.setString(1, apiKey);
            stmt.setString(2, serverId.toString());
            stmt.setString(3, installationId.toString());
            stmt.executeUpdate();
        }
    }

    public void close() {
        dataSource.close();
    }

    public record Credentials(String apiKey, UUID serverId, UUID installationId) {}
}