package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import org.sqlite.JDBC;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

public final class SqliteHomeStorage implements HomeStorage {
    private final Connection connection;

    public SqliteHomeStorage(Path path) throws IOException {
        Connection opened = null;
        try {
            boolean existing = Files.exists(path);
            opened = new JDBC().connect("jdbc:sqlite:" + path.toAbsolutePath(), new Properties());
            try (Statement statement = opened.createStatement()) {
                statement.execute("PRAGMA busy_timeout=5000");
                statement.execute("PRAGMA synchronous=FULL");
                try (ResultSet version = statement.executeQuery("PRAGMA user_version")) {
                    if (version.next() && (version.getInt(1) > 1 || (existing && version.getInt(1) != 1)))
                        throw new SQLException("不支援的資料庫版本，請保留原始資料。");
                }
                statement.execute("CREATE TABLE IF NOT EXISTS homes (name TEXT PRIMARY KEY, creator TEXT NOT NULL, data TEXT NOT NULL)");
                statement.execute("CREATE INDEX IF NOT EXISTS homes_creator ON homes(creator)");
                statement.execute("CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY CHECK(id=1), data TEXT NOT NULL)");
                statement.execute("PRAGMA user_version=1");
            }
            connection = opened;
        } catch (SQLException | RuntimeException e) {
            if (opened != null) try { opened.close(); } catch (SQLException close) { e.addSuppressed(close); }
            throw new IOException("無法開啟 SQLite 住家資料。", e);
        }
    }

    @Override public ConfigSection load() throws IOException {
        try (Statement statement = connection.createStatement()) {
            ConfigSection root = HomeStorage.empty();
            try (ResultSet metadata = statement.executeQuery("SELECT data FROM metadata WHERE id=1")) {
                if (metadata.next()) root = decode(metadata.getString(1));
            }
            List<ConfigSection> homes = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery("SELECT name, creator, data FROM homes ORDER BY name")) {
                while (rows.next()) {
                    ConfigSection home = decode(rows.getString("data"));
                    if (!rows.getString("name").equals(home.getString("name"))
                            || !rows.getString("creator").equals(home.getString("creator")))
                        throw new IOException("SQLite 住家欄位不一致。");
                    homes.add(home);
                }
            }
            root.set("homes", homes);
            return HomeStorage.validate(root);
        } catch (SQLException | RuntimeException e) { throw new IOException("無法讀取 SQLite 住家資料。", e); }
    }

    private static ConfigSection decode(String text) throws IOException {
        Object value = JSON.fromJson(text, Object.class);
        if (!(value instanceof Map<?, ?> map)) throw new IOException("SQLite 資料格式不正確。");
        return new ConfigSection(HomeStorage.toMap(map));
    }

    @Override public void save(ConfigSection document) throws IOException {
        ConfigSection valid = HomeStorage.validate(document);
        try {
            connection.setAutoCommit(false);
            Set<String> previous = new HashSet<>();
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM homes")) {
                while (rows.next()) previous.add(rows.getString(1));
            }
            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO homes(name,creator,data) VALUES(?,?,?) ON CONFLICT(name) DO UPDATE SET creator=excluded.creator,data=excluded.data WHERE homes.data<>excluded.data")) {
                for (Object entry : (List<?>) valid.get("homes")) {
                    Map<?, ?> home = (Map<?, ?>) entry;
                    String name = (String) home.get("name");
                    previous.remove(name);
                    upsert.setString(1, name);
                    upsert.setString(2, (String) home.get("creator"));
                    upsert.setString(3, JSON.toJson(HomeStorage.toMap(home)));
                    upsert.addBatch();
                }
                upsert.executeBatch();
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM homes WHERE name=?")) {
                for (String name : previous) { delete.setString(1, name); delete.addBatch(); }
                delete.executeBatch();
            }
            ConfigSection metadata = new ConfigSection(valid);
            metadata.remove("homes");
            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO metadata(id,data) VALUES(1,?) ON CONFLICT(id) DO UPDATE SET data=excluded.data WHERE metadata.data<>excluded.data")) {
                upsert.setString(1, JSON.toJson(HomeStorage.toMap(metadata)));
                upsert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException | IOException e) {
            try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
            throw new IOException("SQLite 儲存失敗，已取消本次交易。", e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { throw new IOException("無法完成 SQLite 交易。", e); }
        }
    }

    @Override public void close() throws IOException {
        try { connection.close(); } catch (SQLException e) { throw new IOException("無法關閉 SQLite。", e); }
    }
}
