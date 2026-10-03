package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class StorageManager {
    private StorageManager() {}

    public static HomeStorage open(Path folder, String format) throws IOException {
        String selected = format.toLowerCase(Locale.ROOT);
        if (!Set.of("yaml", "sqlite").contains(selected)) throw new IOException("storage.type 僅支援 yaml 或 sqlite。");
        Files.createDirectories(folder);
        Path state = folder.resolve("storage-state.txt");
        Path target = file(folder, selected);
        String previous = Files.exists(state) ? Files.readString(state).strip() : null;
        if (previous != null && !Set.of("yaml", "sqlite").contains(previous)) throw new IOException("儲存格式紀錄無效。");
        if (selected.equals(previous) && !Files.exists(target)) throw new IOException("找不到目前使用的住家資料: " + target.getFileName());
        if (previous == null) {
            String other = selected.equals("yaml") ? "sqlite" : "yaml";
            if (Files.exists(target) && Files.exists(file(folder, other)))
                throw new IOException("同時存在兩種資料檔且缺少格式紀錄，請確認目前使用的資料。");
            if (!Files.exists(target) && Files.exists(file(folder, other))) previous = other;
        }
        if (previous != null && !previous.equals(selected)) {
            Path source = file(folder, previous);
            if (!Files.exists(source)) throw new IOException("找不到目前使用的住家資料: " + source.getFileName());
            ConfigSection document;
            try (HomeStorage storage = create(source, previous)) { document = storage.load(); }
            String suffix = ".backup-" + UUID.randomUUID();
            Files.copy(source, source.resolveSibling(source.getFileName() + suffix));
            if (Files.exists(target)) Files.copy(target, target.resolveSibling(target.getFileName() + suffix));
            Path temporary = folder.resolve(".migration-" + UUID.randomUUID());
            try {
                try (HomeStorage storage = create(temporary, selected)) {
                    storage.save(document);
                    if (!canonical(document).equals(canonical(storage.load()))) throw new IOException("住家轉換驗證失敗。");
                }
                HomeStorage.replace(temporary, target);
            } finally { Files.deleteIfExists(temporary); }
        }
        HomeStorage storage = create(target, selected);
        try {
            ConfigSection document = storage.load();
            if (!Files.exists(target)) storage.save(document);
            HomeStorage.writeAtomic(state, selected.getBytes(StandardCharsets.UTF_8));
            return storage;
        } catch (IOException | RuntimeException e) {
            try { storage.close(); } catch (IOException close) { e.addSuppressed(close); }
            throw e;
        }
    }

    private static JsonElement canonical(ConfigSection root) throws IOException {
        Map<String, Object> copy = HomeStorage.toMap(root);
        List<?> homes = (List<?>) copy.get("homes");
        List<Object> sorted = new ArrayList<>(homes);
        sorted.sort(Comparator.comparing(home -> ((Map<?, ?>) home).get("name").toString()));
        copy.put("homes", sorted);
        return HomeStorage.JSON.toJsonTree(copy);
    }

    private static Path file(Path folder, String format) { return folder.resolve(format.equals("yaml") ? "data.yml" : "homes.db"); }
    private static HomeStorage create(Path path, String format) throws IOException {
        return format.equals("yaml") ? new YamlHomeStorage(path) : new SqliteHomeStorage(path);
    }
}
