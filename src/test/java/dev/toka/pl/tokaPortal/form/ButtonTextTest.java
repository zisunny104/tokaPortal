package dev.toka.pl.tokaPortal.form;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Button labels carry no colour codes; only the close, cancel and delete buttons may start with the red code. */
class ButtonTextTest {
    private static final String[] BUTTON_KEYS = {
            "ui.button.teleportList", "ui.button.shopboxList", "ui.button.regionList", "ui.button.landList", "ui.button.playerList",
            "ui.button.back", "ui.button.prevPage", "ui.button.options", "ui.button.nextPage", "ui.button.pass", "ui.button.pointAdmin",
            "ui.button.close", "ui.button.agree", "ui.button.refuse", "ui.common.cancel", "ui.home.del.button",
            "ui.points.add", "ui.points.list", "ui.points.go", "ui.points.delete",
            "ui.home.edit.delete", "ui.home.edit.go", "ui.home.edit.back"};

    private static Object find(Map<?, ?> map, String path) {
        Object current = map;
        for (String part : path.split("\\.")) current = ((Map<?, ?>) current).get(part);
        return current;
    }

    @Test
    void buttonLabelsHaveNoColourCodesExceptLeadingRed() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("lang/zh-TW.yml")) {
            Map<?, ?> root = new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String key : BUTTON_KEYS) {
                Object value = find(root, key);
                assertNotNull(value, key);
                String text = value.toString();
                String rest = text.startsWith("§c") ? text.substring(2) : text;
                assertFalse(rest.contains("§"), key + " has a colour code: " + text);
            }
        }
    }
}
