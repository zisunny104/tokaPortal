package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static dev.toka.pl.tokaPortal.utils.Utils.CONFIG_VERSION;

public class YamlDataProvider extends BaseDataProvider {

    private Config config;

    public YamlDataProvider(Main main) {
        super(main);
    }

    public Config getConfig() {
        return config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IDataProvider reload(boolean save) {
        if (save) {
            save();
        }
        config = new Config(new File(main.getDataFolder(), "data.yml"), Config.YAML, new ConfigSection() {
            {
                set("version", Utils.CONFIG_VERSION);
                set("homes", new ArrayList<ConfigSection>());
            }
        });
        config.save();
        homes.clear();
        for (Object o : config.getList("homes")) {
            ConfigSection home = new ConfigSection((LinkedHashMap<String, Object>) o);
            if (getHomePoint(home.getString("name")) != null) {
                main.getLogger().warning("載入HomePoint " + home.getString("name") + " 錯誤(重複命名)");
            } else {
                int id = homes.size();
                homes.put(id, new HomePoint(this, id, home));
                main.getLogger().info("載入HomePoint " + home.getString("name") );
            }
        }
        return this;
    }

    @Override
    public IDataProvider save() {
        ArrayList<ConfigSection> homeData = new ArrayList<>();
        homes.values().forEach(home -> homeData.add(home.getRawData()));
        config.setAll(new ConfigSection() {
            {
                set("version", CONFIG_VERSION);
                set("homes", homeData);
                //TODO:WARP DATA
            }
        });
        config.save();
        return this;
    }

    @Override
    public IDataProvider close(boolean save) {
        if (save) {
            save();
        }
        config = null;
        return this;
    }

    @Override
    public String getName() {
        return "YAML";
    }
}
