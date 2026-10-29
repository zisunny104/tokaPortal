package dev.toka.pl.tokaPortal.tpa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Who chose to accept /tpa requests by themselves (true) or to be asked every time (false); a player without an entry follows the server default. */
public final class AutoAccept {
    private final Map<UUID, Boolean> choices = new HashMap<>();

    /** The choice of the player, or null when they follow the server default. */
    public Boolean choice(UUID id) {
        return choices.get(id);
    }

    public boolean effective(UUID id, boolean serverDefault) {
        Boolean own = choices.get(id);
        return own != null ? own : serverDefault;
    }

    /** A null choice removes the entry (back to the server default). */
    public void set(UUID id, Boolean choice) {
        if (choice == null) choices.remove(id);
        else choices.put(id, choice);
    }

    public void load(Path file) throws IOException {
        choices.clear();
        if (!Files.isRegularFile(file)) return;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            int at = line.indexOf('=');
            if (at < 0) continue;
            try {
                choices.put(UUID.fromString(line.substring(0, at).trim()), line.substring(at + 1).trim().equals("on"));
            } catch (IllegalArgumentException skipped) { /* a damaged line is dropped */ }
        }
    }

    public void save(Path file) throws IOException {
        StringBuilder text = new StringBuilder();
        choices.forEach((id, on) -> text.append(id).append('=').append(on ? "on" : "off").append('\n'));
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, text.toString(), StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
