package com.lenis0012.bukkit.marriage2.internal.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import com.google.common.collect.Lists;
import com.lenis0012.bukkit.marriage2.MPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import com.lenis0012.bukkit.marriage2.MData;
import com.lenis0012.bukkit.marriage2.MarriageLog;
import com.lenis0012.bukkit.marriage2.internal.MarriageCore;
import com.lenis0012.bukkit.marriage2.misc.ListQuery;
import org.bukkit.entity.Player;

public class DataManager {

    private final MarriageCore core;
    private final String url;
    private final String prefix;

    public DataManager(MarriageCore core, FileConfiguration config) {
        this.core = core;
        String driver = "org.sqlite.JDBC";
        String idLine = "id INTEGER PRIMARY KEY AUTOINCREMENT,";
        String endLine = ");";

        if (config.getBoolean("MySQL.enabled")) {
            String user = config.getString("MySQL.user", "root");
            String pswd = config.getString("MySQL.password", "");
            String host = config.getString("MySQL.host", "localhost:3306");
            String database = config.getString("MySQL.database", "myserver");
            this.prefix = config.getString("MySQL.prefix", "marriage_");
            this.url = String.format("jdbc:mysql://%s/%s?user=%s&password=%s", host, database, user, pswd);
            idLine = "id INT NOT NULL AUTO_INCREMENT,";
            endLine = ",PRIMARY KEY(id));";
        } else {
            this.url = String.format("jdbc:sqlite:%s", new File(core.getPlugin().getDataFolder(), "database.db"));
            this.prefix = "";
        }

        try {
            Class.forName(driver);
        } catch (Exception e) {
            MarriageLog.severe("Failed to initiate database driver" + e);
        }

        Connection connection = newConnection();
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %splayers ("
                    //					+ "id INT NOT NULL AUTO_INCREMENT,"
                    + "unique_user_id VARCHAR(128) NOT NULL UNIQUE,"
                    + "gender VARCHAR(32),"
                    + "priest BIT,"
                    + "lastlogin BIGINT);", prefix));
            statement.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %sdata ("
                    + idLine
                    + "player1 VARCHAR(128) NOT NULL,"
                    + "player2 VARCHAR(128) NOT NULL,"
                    + "home_world VARCHAR(128) NOT NULL,"
                    + "home_x DOUBLE,"
                    + "home_y DOUBLE,"
                    + "home_z DOUBLE,"
                    + "home_yaw FLOAT,"
                    + "home_pitch FLOAT,"
                    + "pvp_enabled BIT"
                    + endLine, prefix));
        } catch (SQLException e) {
            core.getLogger().log(Level.WARNING, "Failed to load player data", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public MarriagePlayer loadPlayer(UUID uuid) {
        MarriagePlayer player = null;
        Connection connection = newConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(String.format(
                    "SELECT * FROM %splayers WHERE unique_user_id=?;", prefix));
            ps.setString(1, uuid.toString());
            player = new MarriagePlayer(uuid, ps.executeQuery());
            loadMarriages(connection, player, false);
        } catch (SQLException e) {
            MarriageLog.warning("Failed to load player data" + e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }

        return player;
    }

    public void savePlayer(MarriagePlayer player) {
        Connection connection = newConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(String.format(
                    "SELECT * FROM %splayers WHERE unique_user_id=?;", prefix));
            ps.setString(1, player.getUniqueId().toString());
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                // Already in database (update)
                ps = connection.prepareStatement(String.format(
                        "UPDATE %splayers SET gender=?,priest=?,lastlogin=? WHERE unique_user_id=?;", prefix));
                ps.setString(1, player.getGender().toString());
                ps.setBoolean(2, player.isPriest());
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, player.getUniqueId().toString());
                ps.executeUpdate();
            } else {
                // Not in database yet
                ps = connection.prepareStatement(String.format(
                        "INSERT INTO %splayers (unique_user_id,gender,priest,lastlogin) VALUES(?,?,?,?);", prefix));
                player.save(ps);
                ps.executeUpdate();
            }

            // Save marriages
            if (player.getMarriage() != null) {
                MarriageData mdata = (MarriageData) player.getMarriage();
                ps = connection.prepareStatement(String.format("SELECT * FROM %sdata WHERE player1=? AND player2=?;", prefix));
                ps.setString(1, mdata.getPlayer1Id().toString());
                ps.setString(2, mdata.getPllayer2Id().toString());
                result = ps.executeQuery();
                if (result.next()) {
                    // Update existing entry
                    ps = connection.prepareStatement(String.format(
                            "UPDATE %sdata SET player1=?,player2=?,home_world=?,home_x=?,home_y=?,home_z=?,home_yaw=?,home_pitch=?,pvp_enabled=? WHERE id=?;", prefix));
                    mdata.save(ps);
                    ps.setInt(10, mdata.getId());
                    ps.executeUpdate();
                } else {
                    mdata.setSaved(true);
                    ps = connection.prepareStatement(String.format(
                            "INSERT INTO %sdata (player1,player2,home_world,home_x,home_y,home_z,home_yaw,home_pitch,pvp_enabled) VALUES(?,?,?,?,?,?,?,?,?);", prefix));
                    mdata.save(ps);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            MarriageLog.warning("Failed to load player data" + e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private void loadMarriages(Connection connection, MarriagePlayer player, boolean alt) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(String.format(
                "SELECT * FROM %sdata WHERE %s=?;", prefix, alt ? "player2" : "player1", prefix));
        ps.setString(1, player.getUniqueId().toString());
        ResultSet result = ps.executeQuery();
        while (result.next()) {
            UUID partnerId = UUID.fromString(result.getString(alt ? "player1" : "player2"));
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null && partner.isOnline()) {
                // Copy marriage data from partner to ensure a match.
                MPlayer mpartner = core.getMPlayer(partnerId);
                player.addMarriage((MarriageData) mpartner.getMarriage());
            } else {
                player.addMarriage(new MarriageData(result));
            }
        }

        if (!alt) {
            loadMarriages(connection, player, true);
        }
    }

    public void deleteMarriage(UUID player1, UUID player2) {
        Connection connection = newConnection();
        try {
            PreparedStatement ps = connection.prepareStatement(String.format("DELETE FROM %sdata WHERE player1=? AND player2=?;", prefix));
            ps.setString(1, player1.toString());
            ps.setString(2, player2.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            MarriageLog.warning("Failed to load player data" + e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public ListQuery listMarriages(int scale, int page) {
        Connection connection = newConnection();
        try {
            // Count rows to get amount of pages
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + prefix + "data;");
            ResultSet result = ps.executeQuery();
            result.next();
            int pages = (int) Math.ceil(result.getInt(1) / (double) scale);

            // Fetch te right page
            ps = connection.prepareStatement(String.format(
                    "SELECT * FROM %sdata LIMIT %s OFFSET %s;", prefix, scale, scale * page));
            //"SELECT * FROM %sdata ORDER BY id DESC LIMIT %s OFFSET %s;", prefix, scale, scale * page));
            result = ps.executeQuery();

            List<MData> list = Lists.newArrayList();
            while (result.next()) {
                list.add(new MarriageData(result));
            }

            return new ListQuery(pages, page, list);
        } catch (SQLException e) {
            MarriageLog.warning("Failed to load marriage list" + e);
            return new ListQuery(0, 0, new ArrayList<MData>());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private Connection newConnection() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            MarriageLog.warning("Failed to connect to database" + e);
            return null;
        }
    }
}
