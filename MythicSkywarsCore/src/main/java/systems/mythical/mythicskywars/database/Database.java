package systems.mythical.mythicskywars.database;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Database {

    private final String connectionUri;
    private final String username;
    private final String password;
    private final Driver mySqlDriver;
    private Connection connection;

    public Database() throws ClassNotFoundException, SQLException {
        FileConfiguration config = MythicSkywars.get().getConfig();
        final String hostname = config.getString("sqldatabase.hostname");
        final int port = config.getInt("sqldatabase.port");
        final String database = config.getString("sqldatabase.database");
        final boolean ssl = config.getBoolean("sqldatabase.ssl", false);
        final boolean verifyCert = ssl && config.getBoolean("sqldatabase.verifyCertificate", true);
        final boolean pubKeyRetrieval = config.getBoolean("sqldatabase.publicKeyRetrieval", true);
        final int connectTimeoutMs = Math.max(1000, config.getInt("sqldatabase.connectTimeoutMs", 10000));
        final int socketTimeoutMs = Math.max(1000, config.getInt("sqldatabase.socketTimeoutMs", 30000));
        final String serverTimezone = config.getString("sqldatabase.serverTimezone", "UTC");
        final boolean useUnicode = config.getBoolean("sqldatabase.useUnicode", true);
        final String characterEncoding = config.getString("sqldatabase.characterEncoding", "utf8");

        connectionUri = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&verifyServerCertificate=%s&allowPublicKeyRetrieval=%s"
                        + "&connectTimeout=%d&socketTimeout=%d&serverTimezone=%s&useUnicode=%s&characterEncoding=%s",
                hostname, port, database, ssl, verifyCert, pubKeyRetrieval,
                connectTimeoutMs, socketTimeoutMs, serverTimezone, useUnicode, characterEncoding);
        username = config.getString("sqldatabase.username");
        password = config.getString("sqldatabase.password");

        try {
            mySqlDriver = createMySqlDriver();
            connect();

        } catch (SQLException sqlException) {
            close();
            throw sqlException;
        }
    }

    private void connect() throws SQLException {
        if (connection != null) {
            try {
                connection.createStatement().execute("SELECT 1;");

            } catch (SQLException sqlException) {
                if (sqlException.getSQLState().equals("08S01")) {
                    try {
                        connection.close();

                    } catch (SQLException ignored) {
                    }
                }
            }
        }

        if (connection == null || connection.isClosed()) {
            if (mySqlDriver == null) {
                throw new SQLException("MySQL JDBC driver not available.");
            }
            Properties properties = new Properties();
            if (username != null) {
                properties.setProperty("user", username);
            }
            if (password != null) {
                properties.setProperty("password", password);
            }
            connection = mySqlDriver.connect(connectionUri, properties);
            if (connection == null) {
                throw new SQLException("MySQL JDBC driver rejected connection URL: " + connectionUri);
            }
        }
    }

    private Driver createMySqlDriver() throws ClassNotFoundException, SQLException {
        try {
            Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                return (Driver) driverClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new SQLException("Failed to instantiate com.mysql.cj.jdbc.Driver", e);
            }
        } catch (ClassNotFoundException ignored) {
            Class<?> driverClass = Class.forName("com.mysql.jdbc.Driver");
            try {
                return (Driver) driverClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new SQLException("Failed to instantiate com.mysql.jdbc.Driver", e);
            }
        }
    }

    Connection getConnection() {
        return connection;
    }

    private void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

        } catch (SQLException ignored) {

        }

        connection = null;
    }

    boolean checkConnection() {
        try {
            connect();
        } catch (SQLException sqlException) {
            close();
            sqlException.printStackTrace();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void createTables() throws IOException, SQLException {
        URL resource = Resources.getResource(MythicSkywars.class, "/tables.sql");
        String[] databaseStructure = Resources.toString(resource, Charsets.UTF_8).split(";");

        if (databaseStructure.length == 0) {
            return;
        }

        Statement statement = null;

        try {
            connection.setAutoCommit(false);
            statement = connection.createStatement();

            for (String query : databaseStructure) {
                query = query.trim();

                if (query.isEmpty()) {
                    continue;
                }

                statement.execute(query);
            }

            connection.commit();

        } finally {
            connection.setAutoCommit(true);

            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        }
    }

    boolean doesPlayerExist(String fId) {
        if (checkConnection()) {
            return false;
        }

        int count = 0;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            String query = "SELECT Count(`player_id`) FROM `sw_player` WHERE `uuid` = ? LIMIT 1;";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, fId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                count = resultSet.getInt(1);
            }

        } catch (final SQLException sqlException) {
            sqlException.printStackTrace();

        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (final SQLException ignored) {
                }
            }

            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException ignored) {
                }
            }
        }

        return count > 0;
    }

    public void ensureSoulColumn() {
        if (checkConnection()) {
            return;
        }
        ensureColumn("prestige_icon", "VARCHAR(64) NOT NULL DEFAULT 'icon1'");
        ensureColumn("souls", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("soulwell_usages", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("soulwell_legendaries", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("soulwell_rares", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("soulwell_souls_gathered", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("soulwell_souls_purchased", "INT(6) NOT NULL DEFAULT 0");
        ensureColumn("economy", "DOUBLE NOT NULL DEFAULT 0");
        ensureColumn("usw_data", "LONGTEXT NULL");
    }

    private void ensureColumn(String column, String ddl) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate("ALTER TABLE `sw_player` ADD COLUMN `" + column + "` " + ddl);
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Duplicate column") || msg.contains("already exists"))) {
                return;
            }
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public synchronized double getStoredEconomy(String uuid, String playerName) {
        if (checkConnection()) {
            return 0D;
        }
        ensureEconomyRow(uuid, playerName);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SELECT `economy` FROM `sw_player` WHERE `uuid` = ? LIMIT 1;");
            statement.setString(1, uuid);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Math.max(0D, resultSet.getDouble("economy"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
            } catch (SQLException ignored) {
            }
            try {
                if (statement != null) statement.close();
            } catch (SQLException ignored) {
            }
        }
        return 0D;
    }

    public synchronized boolean setStoredEconomy(String uuid, String playerName, double amount) {
        if (checkConnection()) {
            return false;
        }
        ensureEconomyRow(uuid, playerName);
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("UPDATE `sw_player` SET `player_name` = ?, `economy` = ? WHERE `uuid` = ?;");
            statement.setString(1, safeName(playerName, uuid));
            statement.setDouble(2, Math.max(0D, amount));
            statement.setString(3, uuid);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public synchronized boolean addStoredEconomy(String uuid, String playerName, double delta) {
        double current = getStoredEconomy(uuid, playerName);
        return setStoredEconomy(uuid, playerName, current + delta);
    }

    private void ensureEconomyRow(String uuid, String playerName) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(
                    "INSERT INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, `pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`, `economy`) " +
                            "VALUES (NULL, ?, ?, 0, 0, 0, 0, 0, 'none', 'none', 'none', 'none', 'none', 'none', 'icon1', 0, 0, 0, 0, 0, 0, 0) " +
                            "ON DUPLICATE KEY UPDATE `player_name` = VALUES(`player_name`);");
            statement.setString(1, uuid);
            statement.setString(2, safeName(playerName, uuid));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private String safeName(String playerName, String uuid) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            return playerName.trim();
        }
        return uuid;
    }

    void createNewPlayer(String fId, String name) {
        if (checkConnection()) {
            return;
        }

        PreparedStatement preparedStatement = null;

        try {
            String query = "INSERT INTO `sw_player` (`player_id`, `uuid`, `player_name`, `wins`, `losses`, `kills`, `deaths`, `xp`, " +
                    "`pareffect`, `proeffect`, `glasscolor`, `killsound`, `winsound`, `taunt`, `prestige_icon`, `souls`, `soulwell_usages`, `soulwell_legendaries`, `soulwell_rares`, `soulwell_souls_gathered`, `soulwell_souls_purchased`) " +
                    "VALUES (NULL, ?, ?, 0, 0, 0, 0, 0, ?, ?, ?, ?, ?, ?, 'icon1', 0, 0, 0, 0, 0, 0);";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, fId);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, "none");
            preparedStatement.setString(4, "none");
            preparedStatement.setString(5, "none");
            preparedStatement.setString(6, "none");
            preparedStatement.setString(7, "none");
            preparedStatement.setString(8, "none");

            preparedStatement.executeUpdate();

        } catch (final SQLException sqlException) {
            sqlException.printStackTrace();

        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException ignored) {
                }
            }
        }
    }

}