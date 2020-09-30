package com.abstractstudios.lib.database.mysql;

import com.abstractstudios.lib.callback.Callback;
import com.abstractstudios.lib.config.ConfigManager;
import com.abstractstudios.lib.config.impl.DatabaseCredentialsConfig;
import com.abstractstudios.lib.database.Database;
import com.abstractstudios.lib.database.mysql.exceptions.DatabaseException;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MySqlDatabase implements Database<HikariDataSource> {

    // Credentials config.
    private final DatabaseCredentialsConfig config;

    // Connection pool
    private HikariDataSource hikariConnectionPool;

    // Threads
    private final ExecutorService threadPool;

    public MySqlDatabase(String databaseName) {

        // Register the initial database config.
        config = ConfigManager.loadConfigFile(DatabaseCredentialsConfig.class, defaults -> new DatabaseCredentialsConfig(
                "127.0.0.1",
                3306,
                databaseName,
                "root",
                ""
        ));

        // Initialise thread pool
        threadPool = Executors.newFixedThreadPool(10);
    }

    @Override
    public void connect() {

        hikariConnectionPool = new HikariDataSource();

        hikariConnectionPool.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");

        hikariConnectionPool.addDataSourceProperty("serverName", config.getHost());
        hikariConnectionPool.addDataSourceProperty("port", config.getPort());
        hikariConnectionPool.addDataSourceProperty("databaseName", config.getDatabase());
        hikariConnectionPool.addDataSourceProperty("user", config.getUsername());
        hikariConnectionPool.addDataSourceProperty("password", config.getPassword());

        hikariConnectionPool.setMaximumPoolSize(25);

        hikariConnectionPool.setConnectionTimeout(3000);
        hikariConnectionPool.setValidationTimeout(1000);
    }

    @Override
    public void disconnect() {

        if (hikariConnectionPool != null && hikariConnectionPool.isClosed()) {
            hikariConnectionPool.close();
        }

        threadPool.shutdown();
    }

    /**
     * Send a prepared statement to the database async and return the result.
     * @param query - query.
     * @param data - data.
     */
    public void preparedStatement(String query, Callback<ResultSet> data) throws DatabaseException {

        if (hikariConnectionPool == null || hikariConnectionPool.isClosed()) {
            throw new DatabaseException("Not connected to the database.");
        }

        threadPool.submit(() -> {

            try (Connection connection = hikariConnectionPool.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

                boolean altering = (query.toUpperCase().contains("CREATE TABLE") || query.toUpperCase().contains("DROP TABLE") || query.toUpperCase().contains("INSERT INTO") || query.toUpperCase().contains("INSERT IGNORE INTO") || query.toUpperCase().contains("UPDATE") || query.toUpperCase().contains("ALTER TABLE"));

                if (altering) {
                    statement.execute();
                    data.call(null);
                    return;
                }

                // Data
                ResultSet set = statement.executeQuery();

                if (!set.next()) {
                    data.call(null);
                    return;
                }

                data.call(set);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public HikariDataSource getDatabase() {
        return hikariConnectionPool;
    }
}
