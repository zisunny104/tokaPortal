package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlHomeStorage implements HomeStorage {
    private final Path path;
    public YamlHomeStorage(Path path) { this.path = path; }

    @Override public ConfigSection load() throws IOException {
        if (!Files.exists(path)) return HomeStorage.empty();
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        DumperOptions dumper = new DumperOptions();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return HomeStorage.validate(new Yaml(new SafeConstructor(options), new Representer(dumper), dumper, options).load(reader));
        } catch (RuntimeException e) { throw new IOException("無法讀取 YAML 住家資料。", e); }
    }

    @Override public void save(ConfigSection document) throws IOException {
        ConfigSection valid = HomeStorage.validate(document);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        byte[] bytes = new Yaml(options).dump(HomeStorage.toMap(valid)).getBytes(StandardCharsets.UTF_8);
        HomeStorage.writeAtomic(path, bytes);
    }
}
