package dev.toka.pl.tokaPortal.i18n;

import dev.toka.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LanguageFileTest {
    private static final Pattern TEXT_CALL = Pattern.compile("\\b(?:text|lines)\\(\\s*\"([a-zA-Z0-9_.]+)\"\\s*[,)]");
    private static final Pattern HELP_CALL = Pattern.compile("\\bhelp\\([^,()]+,\\s*\"([a-zA-Z0-9_]+)\"\\)");
    private static final Pattern COMMAND_KEY = Pattern.compile("\"(?:cmd\\.)([a-z]+)\\.desc\"");

    private Messages messages(List<String> warnings) {
        return new Messages(getClass().getClassLoader(), Path.of("target", "no-lang-folder"), "zh-TW", warnings::add);
    }

    private Set<String> leafKeys() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("lang/zh-TW.yml")) {
            assertNotNull(stream, "lang/zh-TW.yml is missing");
            try (Reader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Object root = new Yaml().load(reader);
                Set<String> keys = new LinkedHashSet<>();
                flatten("", (Map<?, ?>) root, keys);
                return keys;
            }
        }
    }

    private static void flatten(String prefix, Map<?, ?> map, Set<String> out) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> child) flatten(key, child, out);
            else out.add(key);
        }
    }

    private List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void everyKeyInTheFileCanBeRead() throws IOException {
        List<String> warnings = new ArrayList<>();
        Messages messages = messages(warnings);
        Set<String> keys = leafKeys();
        assertFalse(keys.isEmpty());
        for (String key : keys) {
            assertTrue(messages.has(key), "Cannot read " + key);
            assertFalse(messages.lines(key).isEmpty(), key);
        }
        assertTrue(warnings.stream().noneMatch(line -> line.startsWith("Cannot read")), warnings.toString());
    }

    @Test
    void everyKeyUsedInTheSourceExistsInTheFile() throws IOException {
        Set<String> keys = leafKeys();
        Set<String> used = new LinkedHashSet<>();
        for (Path source : sources()) {
            String code = Files.readString(source, StandardCharsets.UTF_8);
            Matcher matcher = TEXT_CALL.matcher(code);
            while (matcher.find()) used.add(matcher.group(1));
            Matcher help = HELP_CALL.matcher(code);
            while (help.find()) used.add("help." + help.group(1));
            Matcher command = COMMAND_KEY.matcher(code);
            while (command.find()) used.add("cmd." + command.group(1) + ".usage");
        }
        assertFalse(used.isEmpty());
        List<String> missing = used.stream().filter(key -> !keys.contains(key)).toList();
        assertTrue(missing.isEmpty(), "Keys used in the source but missing in lang/zh-TW.yml: " + missing);
    }

    @Test
    void everyCommandHasDescriptionUsageAndHelp() throws IOException {
        Set<String> keys = leafKeys();
        for (String command : List.of("back", "home", "next", "portal", "spawn", "tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel", "dnd", "tph", "tpl", "tpp", "tpw", "wild")) {
            assertTrue(keys.contains("cmd." + command + ".desc"), command);
            assertTrue(keys.contains("cmd." + command + ".usage"), command);
            assertTrue(keys.contains("help." + command), command);
        }
    }

    @Test
    void argumentsAreFilledInOrder() {
        Messages messages = messages(new ArrayList<>());
        assertEquals("§7[傳送]§r§a傳送點 A 建立成功", messages.text("msg.point.created", "A"));
        assertTrue(messages.text("msg.point.arrived", "A", "B").contains("A"));
    }
}
