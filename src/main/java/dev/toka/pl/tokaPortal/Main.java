package dev.toka.pl.tokaPortal;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.command.CommandSender;
import dev.toka.i18n.Messages;
import dev.toka.pl.tokaPortal.command.*;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import dev.toka.pl.tokaPortal.provider.BaseDataProvider;
import dev.toka.pl.tokaPortal.provider.storage.StorageManager;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.utils.PortalHistory;
import dev.toka.pl.tokaPortal.plan.*;

public class Main extends PluginBase implements Listener {

    private static Main instance;
    private static IDataProvider provider;
    private Messages messages;
    private PlanSettings planSettings;
    private AutoCloseable plan;
    private volatile PlanSnapshot planSnapshot;
    private int planTicks;
    private dev.toka.pl.tokaPortal.provider.storage.WriteQueue writer;
    private dev.toka.pl.tokaPortal.point.PublicPoints publicPoints;
    public dev.toka.pl.tokaPortal.point.PublicPoints getPublicPoints(){return publicPoints;}
    private dev.toka.pl.tokaPortal.provider.OwnerMigration ownerMigration;
    public dev.toka.pl.tokaPortal.provider.OwnerMigration getOwnerMigration() { return ownerMigration; }
    private dev.toka.pl.tokaPortal.wild.WildService wildService;
    public dev.toka.pl.tokaPortal.wild.WildService getWildService(){return wildService;}

    public static Main getInstance() {
        return instance;
    }

    /** Text by key with ${0}, ${1} ... arguments; the key itself is returned before the plugin is enabled. */
    public static String text(String key, Object... arguments) {
        return instance == null || instance.messages == null ? key : instance.messages.text(key, arguments);
    }

    /** Every line of a multi-line text. */
    public static java.util.List<String> lines(String key, Object... arguments) {
        return instance == null || instance.messages == null ? java.util.List.of(key) : instance.messages.lines(key, arguments);
    }

    /** The values a help text needs from the config, such as the worlds and the cooldown of /wild. */
    public static Object[] helpArguments(String command) {
        if (command.equals("wild")) {
            var config = instance.getConfig();
            java.util.List<String> worlds = config.getStringList("wild.worlds");
            return new Object[]{worlds.isEmpty() ? "-" : String.join("、", worlds), config.getInt("wild.cooldown-seconds", 40)};
        }
        return new Object[0];
    }

    /** Sends the help text of a command (help.&lt;command&gt;) to the sender. */
    public static void help(CommandSender sender, String command) {
        for (String line : lines("help." + command, helpArguments(command))) sender.sendMessage(line);
    }

    public static IDataProvider getProvider() {
        return provider;
    }

    @Override
    public void onEnable() {
        instance = this;
        writer = new dev.toka.pl.tokaPortal.provider.storage.WriteQueue((key, error) -> getLogger().error(text(key), error));
        saveDefaultConfig();
        messages = new Messages(getClass().getClassLoader(), getDataFolder().toPath(), getConfig().getString("language", "zh-TW"), line -> getLogger().warning(line));
        dev.toka.pl.tokaPortal.integration.ZeroIntegration.initialize(false);
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.isLinked()) {
            // zero may be enabled after this plugin; check again once all plugins are up
            getServer().getScheduler().scheduleDelayedTask(this, () -> {
                if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.isLinked()) {
                    dev.toka.pl.tokaPortal.integration.ZeroIntegration.initialize(true);
                }
            }, 40);
        }
        reload();
        ownerMigration = new dev.toka.pl.tokaPortal.provider.OwnerMigration(this);
        // 其他插件（tokaEconomy、tokaProfiles）啟用後再解析名稱
        getServer().getScheduler().scheduleDelayedTask(this, () -> { if (provider != null) ownerMigration.start(provider, publicPoints); }, 20);

        wildService=new dev.toka.pl.tokaPortal.wild.WildService(this);
        getServer().getScheduler().scheduleRepeatingTask(this,()->wildService.render(),20);
        this.registerEvents();
        this.registerCommandMap();
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            if (planSettings != null && planSettings.enabled() && (planTicks += 20) >= planSettings.refreshTicks()) {
                planTicks = 0;
                publishPlanSnapshot();
            }
        }, 20);
    }

    @Override
    public void onDisable() {
        closePlan(); planSnapshot = null;
        if(wildService!=null)wildService.close();
        if (ownerMigration != null) ownerMigration.stop();
        drainWrites();
        if (publicPoints != null) { try {publicPoints.close();} catch(java.io.IOException e){getLogger().error(text("log.points.closeFailed"),e);} publicPoints=null;}
        if (provider != null) { provider.close(false); provider = null; }
        if (writer != null) writer.shutdown();
    }

    /** 關閉儲存前等背景寫入完成，逾時只記 log。 */
    private void drainWrites() {
        if (writer != null && !writer.drain(10_000)) getLogger().warning(text("log.writer.timeout"));
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Portal(), this);
        this.getServer().getPluginManager().registerEvents(new PortalHistory(), this);
        dev.toka.pl.tokaPortal.form.FormListener.register(this);
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
        closePlan(); planSnapshot = null;
        saveDefaultConfig();
        reloadConfig();
        if (messages != null) messages.language(getConfig().getString("language", "zh-TW"));
        drainWrites();
        if (publicPoints != null) { try {publicPoints.close();} catch(java.io.IOException e){getLogger().error(text("log.points.closeFailed"),e);} publicPoints=null;}
        if (provider != null) { provider.close(false); provider = null; }
        String format = getConfig().getNested("storage.backend", "sqlite");
        try {
            var storage = StorageManager.openConfigured(getDataFolder().toPath(), getConfig().getAll(), messages);
            provider = new BaseDataProvider(storage, writer, format);
            var backend=dev.toka.storage.Backend.valueOf(format.toUpperCase(java.util.Locale.ROOT));
            publicPoints=new dev.toka.pl.tokaPortal.point.PublicPoints(getDataFolder().toPath().resolve("storage").resolve(format).resolve("public-points"),backend,writer);
            getLogger().info(text("log.points.loaded", publicPoints.points().size()));
            int homes = storage.load().getList("homes").size();
            getLogger().info(text("log.homes.loaded", homes));
            planSettings = PlanSettings.parse(getConfig().getAll());
            publishPlanSnapshot();
            refreshPlan();
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalStateException(text("err.storageOpen"), e);
        }
    }

    private void publishPlanSnapshot() {
        if (planSettings == null || !planSettings.enabled()) { planSnapshot = null; return; }
        if (provider instanceof BaseDataProvider base && publicPoints != null) {
            planSnapshot = new PlanSnapshot(planSettings.homes() ? base.planOwnerCounts() : java.util.Map.of(),
                    planSettings.homes() ? base.planTotalCount() : 0, planSettings.publicPoints() ? publicPoints.planCount() : 0);
        }
    }
    private void closePlan() {
        if (plan == null) return;
        try { plan.close(); }
        catch (Exception | LinkageError e) { getLogger().warning(text("plan.unavailable", e.getClass().getSimpleName())); }
        finally { plan=null; }
    }
    private void refreshPlan() {
        closePlan();
        var plugin=getServer().getPluginManager().getPlugin("Plan");
        if(planSettings==null || !planSettings.enabled() || plugin==null || !plugin.isEnabled())return;
        try {
            PlanExtension extension=new PlanExtension(planSettings,()->planSnapshot,Main::text);
            if(extension.register()){plan=extension;getLogger().info(text("plan.ready"));}
            else getLogger().warning(text("plan.unsupported"));
        } catch(RuntimeException | LinkageError e){getLogger().warning(text("plan.unavailable",e.getClass().getSimpleName()));}
    }
    @EventHandler public void onPlanEnable(cn.nukkit.event.plugin.PluginEnableEvent event) {
        if("Plan".equals(event.getPlugin().getName()))refreshPlan();
    }
    @EventHandler public void onPlanDisable(cn.nukkit.event.plugin.PluginDisableEvent event) {
        if("Plan".equals(event.getPlugin().getName()))closePlan();
    }

    /** 登入：更新住家與傳送點的最後已知名稱，並取回同名的 legacy 住家與傳送點。 */
    @EventHandler
    public void onPlayerJoin(cn.nukkit.event.player.PlayerJoinEvent event) {
        if (provider == null) return;
        var player = event.getPlayer();
        var joined = provider.onPlayerJoin(player.getUniqueId(), player.getName());
        if (joined.claimed() > 0) getLogger().info(text("log.homes.claimed", player.getName(), joined.claimed()));
        if (joined.disputed()) getLogger().warning(text("log.homes.joinDispute", player.getName()));
        if (publicPoints == null) return;
        var points = publicPoints.onPlayerJoin(player.getUniqueId(), player.getName());
        if (points.claimed() > 0) getLogger().info(text("log.points.claimed", player.getName(), points.claimed()));
        if (points.disputed()) getLogger().warning(text("log.points.joinDispute", player.getName()));
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

