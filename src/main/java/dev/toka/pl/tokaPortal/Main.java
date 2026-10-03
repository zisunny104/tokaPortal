package dev.toka.pl.tokaPortal;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.plugin.PluginBase;
import dev.toka.pl.tokaPortal.bstats.MetricsLite;
import dev.toka.pl.tokaPortal.command.*;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import dev.toka.pl.tokaPortal.provider.BaseDataProvider;
import dev.toka.pl.tokaPortal.provider.storage.StorageManager;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.utils.PortalHistory;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

public class Main extends PluginBase implements Listener {

    private static Main instance;
    private static IDataProvider provider;

    public static Main getInstance() {
        return instance;
    }

    public static IDataProvider getProvider() {
        return provider;
    }

    @Override
    public void onEnable() {
        instance = this;
        dev.toka.pl.tokaPortal.integration.ZeroIntegration.initialize();
        reload();

        try {
            if (Boolean.TRUE.equals(getConfig().getNested("metrics.enabled", false))) {
                MetricsLite metricsLite = new MetricsLite(this);
                if (metricsLite.isEnabled()) getLogger().info("[bStats]已允許傳送資料");
            }
        } catch (RuntimeException | LinkageError e) {
            getLogger().warning("bStats 無法啟動，傳送功能繼續運作: " + e);
        }

        this.registerEvents();
        this.registerCommandMap();
        getLogger().info("tokaPortal 已啟用，住家儲存格式: " + provider.getName());
    }

    @Override
    public void onDisable() {
        if (provider != null) { provider.close(false); provider = null; }
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Portal(), this);
        this.getServer().getPluginManager().registerEvents(new PortalHistory(), this);
        this.getServer().getPluginManager().registerEvents(new PortalWindow(), this);
    }

    private void registerCommandMap() {
        this.getServer().getCommandMap().register("back", new BackCommand());
        this.getServer().getCommandMap().register("home", new HomeCommand());
        this.getServer().getCommandMap().register("next", new NextCommand());
        this.getServer().getCommandMap().register("portal", new PortalCommand());
        this.getServer().getCommandMap().register("spawn", new SpawnCommand());
        this.getServer().getCommandMap().register("tpa", new TpaCommand());
        this.getServer().getCommandMap().register("tph", new TphCommand());
        this.getServer().getCommandMap().register("tpl", new TplCommand());
        this.getServer().getCommandMap().register("tpp", new TppCommand());
        this.getServer().getCommandMap().register("tpw", new TpwCommand());
        this.getServer().getCommandMap().register("wild", new WildCommand());
    }

    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        if (provider != null) { provider.close(false); provider = null; }
        String format = getConfig().getNested("storage.type", "yaml");
        try {
            provider = new BaseDataProvider(StorageManager.open(getDataFolder().toPath(), format), format);
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalStateException("住家資料無法開啟；請檢查設定與資料檔，勿刪除原始資料。", e);
        }
    }

    @EventHandler
    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
        if (event.getWindow() instanceof BaseForm) {
            BaseForm form = (BaseForm) event.getWindow();
            if (event.getResponse() != null) {
                form.onFormResponse(event);
            } else {
                form.onFormClose(event);
            }
        }
    }


}
