package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.JDBC;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HomeStorageTest {
    @TempDir Path folder;

    private ConfigSection data(String name) {
        LinkedHashMap<String, Object> home = new LinkedHashMap<>();
        home.put("name", name);
        home.put("creator", "玩家");
        home.put("type", "home");
        home.put("loc", new LinkedHashMap<>(Map.of("level", "unloaded", "x", -0.125, "y", 64.5,
                "z", 23.75, "yaw", 120.125, "pitch", -14.5, "custom", "保留")));
        home.put("custom", Arrays.asList(null, 9007199254740993L, "字串"));
        ConfigSection root = HomeStorage.empty();
        root.set("homes", new ArrayList<>(List.of(home)));
        root.set("custom", Map.of("enabled", true));
        return root;
    }

    private void assertData(ConfigSection root, String name) {
        Map<?, ?> home = (Map<?, ?>) root.getList("homes").getFirst();
        assertEquals(name, home.get("name"));
        assertEquals(9007199254740993L, ((Number) ((List<?>) home.get("custom")).get(1)).longValue());
        assertNull(((List<?>) home.get("custom")).getFirst());
        Map<?, ?> loc = (Map<?, ?>) home.get("loc");
        assertEquals(-0.125, ((Number) loc.get("x")).doubleValue());
        assertEquals("unloaded", loc.get("level"));
        assertEquals("保留", loc.get("custom"));
        assertTrue((Boolean) ((Map<?, ?>) root.get("custom")).get("enabled"));
    }

    @Test void bothFormatsPreserveExtraFieldsAndPrecision() throws Exception {
        for (String type : List.of("yaml", "sqlite")) {
            Path dir = Files.createDirectory(folder.resolve(type));
            try (HomeStorage storage = StorageManager.open(dir, type)) { storage.save(data("家 ' ; DROP TABLE homes; --")); }
            try (HomeStorage storage = StorageManager.open(dir, type)) { assertData(storage.load(), "家 ' ; DROP TABLE homes; --"); }
        }
    }

    @Test void repeatedSwitchUsesCurrentDataAndCreatesBackups() throws Exception {
        try (HomeStorage storage = StorageManager.open(folder, "yaml")) { storage.save(data("舊家")); }
        try (HomeStorage storage = StorageManager.open(folder, "sqlite")) {
            assertData(storage.load(), "舊家"); storage.save(data("新家"));
        }
        try (HomeStorage storage = StorageManager.open(folder, "yaml")) { assertData(storage.load(), "新家"); }
        try (HomeStorage storage = StorageManager.open(folder, "sqlite")) { assertData(storage.load(), "新家"); }
        try (var files = Files.list(folder)) { assertTrue(files.filter(path -> path.toString().contains(".backup-")).count() >= 3); }
    }

    @Test void malformedYamlDoesNotOverwriteExistingTarget() throws Exception {
        Files.writeString(folder.resolve("data.yml"), "homes: [invalid]");
        Files.writeString(folder.resolve("storage-state.txt"), "yaml");
        byte[] target = "existing database".getBytes();
        Files.write(folder.resolve("homes.db"), target);
        assertThrows(IOException.class, () -> StorageManager.open(folder, "sqlite"));
        assertArrayEquals(target, Files.readAllBytes(folder.resolve("homes.db")));
        assertEquals("yaml", Files.readString(folder.resolve("storage-state.txt")));
    }

    @Test void missingActiveFileAndAmbiguousFilesAreRejected() throws Exception {
        Files.writeString(folder.resolve("storage-state.txt"), "yaml");
        assertThrows(IOException.class, () -> StorageManager.open(folder, "yaml"));
        Files.delete(folder.resolve("storage-state.txt"));
        Files.writeString(folder.resolve("data.yml"), "homes: []");
        Files.writeString(folder.resolve("homes.db"), "unknown");
        assertThrows(IOException.class, () -> StorageManager.open(folder, "yaml"));
    }

    @Test void unsafeYamlTagsAndDuplicateKeysAreRejected() throws Exception {
        Path path = folder.resolve("data.yml");
        for (String text : List.of("homes: []\nhomes: []", "!!javax.script.ScriptEngineManager []", "homes: []\ncustom: &loop [*loop]")) {
            Files.writeString(path, text);
            assertThrows(IOException.class, () -> new YamlHomeStorage(path).load());
            assertEquals(text, Files.readString(path));
        }
    }

    @Test void invalidSavePreservesExistingData() throws Exception {
        for (String type : List.of("yaml", "sqlite")) {
            Path dir = Files.createDirectory(folder.resolve(type));
            try (HomeStorage storage = StorageManager.open(dir, type)) {
                storage.save(data("原家"));
                ConfigSection invalid = data("重複");
                List<Object> entries = new ArrayList<>(invalid.getList("homes"));
                entries.add(entries.getFirst());
                invalid.set("homes", entries);
                assertThrows(IOException.class, () -> storage.save(invalid));
                assertData(storage.load(), "原家");
            }
        }
    }

    @Test void unknownSqliteSchemaIsRejected() throws Exception {
        Path path = folder.resolve("homes.db");
        try (var conn = new JDBC().connect("jdbc:sqlite:" + path, new Properties()); var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE unrelated(value TEXT)");
        }
        byte[] original = Files.readAllBytes(path);
        assertThrows(IOException.class, () -> new SqliteHomeStorage(path));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test void sqliteFailureRollsBackEntireTransaction() throws Exception {
        Path path = folder.resolve("homes.db");
        try (HomeStorage storage = new SqliteHomeStorage(path)) {
            storage.save(data("原家"));
            try (var conn = new JDBC().connect("jdbc:sqlite:" + path, new Properties()); var stmt = conn.createStatement()) {
                stmt.execute("CREATE TRIGGER reject_home BEFORE INSERT ON homes WHEN NEW.name='失敗' BEGIN SELECT RAISE(ABORT,'test failure'); END");
            }
            ConfigSection candidate = data("先寫入");
            candidate.getList("homes").add(data("失敗").getList("homes").getFirst());
            assertThrows(IOException.class, () -> storage.save(candidate));
            assertEquals(1, storage.load().getList("homes").size());
            assertData(storage.load(), "原家");
        }
    }
}
