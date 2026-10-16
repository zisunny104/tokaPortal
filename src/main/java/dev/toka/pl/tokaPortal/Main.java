package dev.toka.pl.tokaPortal;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.plugin.PluginBase;
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
    private dev.toka.pl.tokaPortal.wild.WildService wildService;
    public dev.toka.pl.tokaPortal.wild.WildService getWildService(){return wildService;}

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
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.isLinked()) {
            // zero may be enabled after this plugin; check again once all plugins are up
            getServer().getScheduler().scheduleDelayedTask(this, () -> {
                if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.isLinked()) {
                    dev.toka.pl.tokaPortal.integration.ZeroIntegration.initialize();
                }
            }, 40);
        }
        reload();

        wildService=new dev.toka.pl.tokaPortal.wild.WildService(this);
        getServer().getScheduler().scheduleRepeatingTask(this,()->wildService.render(),20);
        this.registerEvents();
        this.registerCommandMap();
    }

    @Override
    public void onDisable() {
        if(wildService!=null)wildService.close();
        if (provider != null) { provider.close(false); provider = null; }
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Portal(), this);
        this.getServer().getPluginManager().registerEvents(new PortalHistory(), this);
        this.getServer().getPluginManager().registerEvents(new PortalWindow(), this);
        this.getServer().getPluginManager().registerEvents(new CommandSuggestionUpdater(), this);
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
        String format = getConfig().getNested("storage.backend", "sqlite");
        try {
            var storage = StorageManager.openConfigured(getDataFolder().toPath(), getConfig().getAll());
            int homes = storage.load().getList("homes").size();
            provider = new BaseDataProvider(storage, format);
            getLogger().info("載入住家共 " + homes + " 個");
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

