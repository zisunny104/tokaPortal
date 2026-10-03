package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

public interface HomeStorage extends AutoCloseable {
    Gson JSON = new GsonBuilder().serializeNulls().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    ConfigSection load() throws IOException;
    void save(ConfigSection document) throws IOException;
    @Override default void close() throws IOException {}

    static ConfigSection validate(Object value) throws IOException {
        if (!(value instanceof Map<?, ?> map)) throw new IOException("住家資料必須是物件。");
        ConfigSection root = new ConfigSection(toMap(map));
        Object entries = root.get("homes");
        if (!(entries instanceof List<?> list)) throw new IOException("住家資料缺少 homes 列表。");
        Set<String> names = new HashSet<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> home)) throw new IOException("住家資料格式不正確。");
            String name = requiredString(home, "name");
            if (!names.add(name)) throw new IOException("住家名稱重複: " + name);
            requiredString(home, "creator");
            requiredString(home, "type");
            if (!(home.get("loc") instanceof Map<?, ?> location)) throw new IOException("住家缺少位置: " + name);
            requiredString(location, "level");
            for (String coordinate : List.of("x", "y", "z", "yaw", "pitch")) {
                if (!(location.get(coordinate) instanceof Number number) || !Double.isFinite(number.doubleValue()))
                    throw new IOException("住家位置無效: " + name + "/" + coordinate);
            }
        }
        return root;
    }

    private static String requiredString(Map<?, ?> map, String key) throws IOException {
        if (!(map.get(key) instanceof String text) || text.isBlank()) throw new IOException("資料欄位無效: " + key);
        return text;
    }

    static LinkedHashMap<String, Object> toMap(Map<?, ?> map) throws IOException {
        return toMap(map, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private static LinkedHashMap<String, Object> toMap(Map<?, ?> map, Set<Object> visiting, int depth) throws IOException {
        if (depth > 50 || !visiting.add(map)) throw new IOException("資料包含循環參照或過多巢狀欄位。");
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IOException("資料欄位名稱必須是字串。");
            copy.put(key, plain(entry.getValue(), visiting, depth + 1));
        }
        visiting.remove(map);
        return copy;
    }

    private static Object plain(Object value, Set<Object> visiting, int depth) throws IOException {
        if (depth > 50) throw new IOException("資料包含過多巢狀欄位。");
        if (value instanceof Map<?, ?> map) return toMap(map, visiting, depth);
        if (value instanceof List<?> list) {
            if (!visiting.add(list)) throw new IOException("資料包含循環參照。");
            List<Object> copy = new ArrayList<>();
            for (Object item : list) copy.add(plain(item, visiting, depth + 1));
            visiting.remove(list);
            return copy;
        }
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) return value;
        throw new IOException("資料包含不支援的值: " + value.getClass().getSimpleName());
    }

    static ConfigSection empty() {
        ConfigSection root = new ConfigSection();
        root.set("version", "1.0.4");
        root.set("homes", new ArrayList<>());
        return root;
    }

    static void writeAtomic(Path destination, byte[] bytes) throws IOException {
        Path temp = Files.createTempFile(destination.toAbsolutePath().getParent(), ".tokaPortal-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            replace(temp, destination);
        } finally { Files.deleteIfExists(temp); }
    }

    static void replace(Path source, Path destination) throws IOException {
        // A failed replacement leaves the existing file intact.
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
