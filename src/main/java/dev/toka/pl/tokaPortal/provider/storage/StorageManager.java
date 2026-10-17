package dev.toka.pl.tokaPortal.provider.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Opens the storage of the homes. The homes are always kept by tokaStorage, in the folder of the plugin under
 * storage/<backend>/. The first backend that is used is written to storage/active-backend.txt, so that changing the
 * config by mistake does not start with an empty store.
 */
public final class StorageManager {
    private StorageManager() {
    }

    public static HomeStorage openConfigured(Path folder, Map<String, Object> config) throws IOException {
        Object raw = config.get("storage");
        if (raw != null && !(raw instanceof Map<?, ?>)) {
            throw new IOException("The storage setting must be an object.");
        }
        Map<?, ?> settings = raw instanceof Map<?, ?> map ? map : Map.of();
        Object backend = settings.get("backend");
        if (backend == null && settings.get("type") != null) {
            throw new IOException("The legacy storage.type is no longer supported; use storage.backend: sqlite, json or yaml.");
        }

        String selected = Objects.toString(backend, "sqlite").toLowerCase(Locale.ROOT);
        dev.toka.storage.Backend format;
        try {
            format = dev.toka.storage.Backend.valueOf(selected.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IOException("storage.backend supports only sqlite, json or yaml.", error);
        }

        Path root = folder.resolve("storage");
        Path target = root.resolve(selected);
        Path marker = root.resolve("active-backend.txt");
        if (Files.exists(marker) && !Files.readString(marker).strip().equals(selected)) {
            throw new IOException("A tokaStorage backend is already active; migrate explicitly while offline, do not only change the setting.");
        }
        if (!Files.exists(marker)) {
            for (String candidate : List.of("sqlite", "json", "yaml")) {
                if (!candidate.equals(selected) && Files.exists(root.resolve(candidate))) {
                    throw new IOException("Found a different tokaStorage directory; verify it and migrate explicitly.");
                }
            }
        }

        TokaHomeStorage storage = new TokaHomeStorage(target, format);
        try {
            storage.save(storage.load());
            HomeStorage.writeAtomic(marker, selected.getBytes(StandardCharsets.UTF_8));
            return storage;
        } catch (IOException | RuntimeException error) {
            try {
                storage.close();
            } catch (IOException cleanup) {
                error.addSuppressed(cleanup);
            }
            throw error;
        }
    }
}
