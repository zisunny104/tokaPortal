package dev.toka.pl.tokaPortal;

import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginBase;
import dev.toka.pl.tokaPortal.command.*;
import dev.toka.pl.tokaPortal.bstats.MetricsLite;

public class Main extends PluginBase implements Listener {

    private static Main instance;

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
        this.getServer().getCommandMap().register("portal", new PortalCommand());
        this.getServer().getCommandMap().register("spawn", new SpawnCommand());
        this.getServer().getCommandMap().register("tpa", new TpaCommand());
        this.getServer().getCommandMap().register("tpl", new TplCommand());
        this.getServer().getCommandMap().register("tpp", new TppCommand());
        this.getServer().getCommandMap().register("tpw", new TpwCommand());
        this.getServer().getCommandMap().register("wild", new WildCommand());
    }

    public static Main getInstance() {
        return instance;
    }
}
