package dev.toka.pl.tokaPortal;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.plugin.PluginBase;
import dev.toka.pl.tokaPortal.bstats.MetricsLite;
import dev.toka.pl.tokaPortal.command.*;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import dev.toka.pl.tokaPortal.provider.YamlDataProvider;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

public class Main extends PluginBase implements Listener {

    private static Main instance;
    private static IDataProvider provider;

    @Override
    public void onLoad() {
        this.getLogger().info("[prj_Toka]正在載入 傳送");
    }

    @Override
    public void onEnable() {
        this.getLogger().info("[prj_Toka]載入完成 傳送");
        instance = this;

        MetricsLite metricsLite = new MetricsLite(this);
        if (metricsLite.isEnabled()) {
            getLogger().info("[bStats]已允許傳送資料");
        }

        this.registerEvents();
        this.registerCommandMap();

        reload();
    }

    @Override
    public void onDisable() {
        this.getLogger().info("[prj_Toka]正在關閉 傳送");
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Portal(), this);
        this.getServer().getPluginManager().registerEvents(new PortalWindow(), this);
    }


    private void registerCommandMap() {
        this.getServer().getCommandMap().register("back", new BackCommand());
        this.getServer().getCommandMap().register("home", new HomeCommand());
        this.getServer().getCommandMap().register("portal", new PortalCommand());
        this.getServer().getCommandMap().register("spawn", new SpawnCommand());
        this.getServer().getCommandMap().register("tpa", new TpaCommand());
        this.getServer().getCommandMap().register("tph", new TphCommand());
        this.getServer().getCommandMap().register("tpl", new TplCommand());
        this.getServer().getCommandMap().register("tpp", new TppCommand());
        this.getServer().getCommandMap().register("tpw", new TpwCommand());
        this.getServer().getCommandMap().register("wild", new WildCommand());
    }

    public static Main getInstance() {
        return instance;
    }

    public static IDataProvider getProvider() {
        return provider;
    }

    public void reload() {
        //Utils.init(this);
        //ConfigProvider.init(this);
        //TODO:經濟串接
        //Economy.init(this,ConfigProvider.getString("PreferEconomy"));
        //TODO:增加多種存儲格式
        provider = new YamlDataProvider(this);

        //ConfigProvider.set("Provider",provider.getName());
        //ConfigProvider.set("PreferEconomy",Economy.getApi().toString());
        //getLogger().notice(Utils.translate("current.provider",provider.getName()));
        //getLogger().notice(Utils.translate("current.economy",Economy.getApi().toString()));
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
