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
    private PlanControl planControl;
    private volatile PlanSnapshot planSnapshot;
    private int planTicks;
    private dev.toka.pl.tokaPortal.provider.storage.WriteQueue writer;
    private dev.toka.pl.tokaPortal.point.PublicPoints publicPoints;
    public dev.toka.pl.tokaPortal.point.PublicPoints getPublicPoints(){return publicPoints;}
    private dev.toka.pl.tokaPortal.provider.OwnerMigration ownerMigration;
    public dev.toka.pl.tokaPortal.provider.OwnerMigration getOwnerMigration() { return ownerMigration; }
    private final java.util.Map<String, cn.nukkit.command.Command> commands = new java.util.LinkedHashMap<>();
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
        reload();
        ownerMigration = new dev.toka.pl.tokaPortal.provider.OwnerMigration(this);

        wildService=new dev.toka.pl.tokaPortal.wild.WildService(this);
        getServer().getScheduler().scheduleRepeatingTask(this,()->wildService.render(),20);
        this.registerEvents();
        this.registerCommandMap();
        dev.toka.pl.tokaPortal.tpa.TpaFlow.start(this);
        getServer().getScheduler().scheduleRepeatingTask(this, () -> {
            if (planSettings != null && planSettings.enabled() && (planTicks += 20) >= planSettings.refreshTicks()) {
                planTicks = 0;
                publishPlanSnapshot();
            }
        }, 20);
    }

    @Override
    public void onDisable() {
        // The commands were put into the map by hand, so the server does not remove them when the plugin goes.
        var map = getServer().getCommandMap();
        for (var command : commands.values()) {
            map.getCommands().entrySet().removeIf(entry -> entry.getValue() == command);
            command.unregister(map);
        }
        commands.clear();
        dev.toka.pl.tokaPortal.form.FormListener.clear();
        dev.toka.pl.tokaPortal.tpa.TpaFlow.shutdown();
        CommandSuggestionUpdater.reset();
        closePlan(); planSnapshot = null;
        if(wildService!=null)wildService.close();
        if (ownerMigration != null) ownerMigration.stop();
        drainWrites();
        if (publicPoints != null) { try {publicPoints.close();} catch(java.io.IOException e){getLogger().error(text("log.points.closeFailed"),e);} publicPoints=null;}
        if (provider != null) { provider.close(false); provider = null; }
        if (writer != null) writer.shutdown();
        if (instance == this) instance = null;
    }

    /** Sends the command parameters again, after names they offer have changed. */
    public void refreshCommandData() {
        getServer().getOnlinePlayers().values().stream().filter(cn.nukkit.Player::isOnline).forEach(cn.nukkit.Player::sendCommandData);
    }

    /** Re-reads the config, the texts and the data, and renews what was built from them. */
    public void reloadAll() {
        dev.toka.pl.tokaPortal.tpa.TpaFlow.cancelAll();
        reload();
        commands.forEach((name, command) -> {
            command.setDescription(text("cmd." + name + ".desc"));
            command.setUsage(text("cmd." + name + ".usage"));
        });
        refreshCommandData();
    }

    /** 關閉儲存前等背景寫入完成，逾時只記 log。 */
    private void drainWrites() {
        if (writer != null && !writer.drain(10_000)) getLogger().warning(text("log.writer.timeout"));
    }

    /** Every teleport is written in the log and in the command channel of Discord (when tokaDiscordLink is there). */
    @cn.nukkit.event.EventHandler(priority = cn.nukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleported(dev.toka.pl.tokaPortal.event.PlayerPortalEvent event) {
        String line = event.toString();
        getLogger().info(line);
        var discord = getServer().getPluginManager().getPlugin("tokaDiscordLink");
        if (discord == null || !discord.isEnabled()) return;
        try {
            Class.forName("dev.toka.discord.api.DiscordLink", true, discord.getClass().getClassLoader())
                    .getMethod("sendMessage", String.class, String.class).invoke(null, cn.nukkit.utils.TextFormat.clean(line), "cmd");
        } catch (ReflectiveOperationException | LinkageError unavailable) { /* Discord is optional */ }
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new Portal(), this);
        this.getServer().getPluginManager().registerEvents(new PortalHistory(), this);
        dev.toka.pl.tokaPortal.form.FormListener.register(this);
        this.getServer().getPluginManager().registerEvents(new CommandSuggestionUpdater(), this);
    }

    private void registerCommandMap() {
        register("back", new BackCommand());
        register("home", new HomeCommand());
        register("next", new NextCommand());
        register("portal", new PortalCommand());
        register("spawn", new SpawnCommand());
        register("tpa", new TpaCommand());
        register("tpahere", new TpaHereCommand());
        register("tpaccept", new TpAcceptCommand());
        register("tpdeny", new TpDenyCommand());
        register("tpcancel", new TpCancelCommand());
        register("dnd", new DndCommand());
        register("tph", new TphCommand());
        register("tpl", new TplCommand());
        register("tpp", new TppCommand());
        register("tpw", new TpwCommand());
        register("wild", new WildCommand());
    }

    private void register(String name, cn.nukkit.command.Command command) {
        getServer().getCommandMap().register(name, command);
        commands.put(name, command);
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
        if(planControl!=null)planControl.registered(false);
        if (plan == null) return;
        try { plan.close(); }
        catch (Exception | LinkageError e) { getLogger().warning(text("log.plan.unavailable", e.getClass().getSimpleName())); }
        finally { plan=null; }
    }
    private void refreshPlan() {
        closePlan();
        if(planControl==null)planControl=new PlanControl(this,this::refreshPlan,()->java.util.Set.of("homes","public-points"),(key,args)->text(key,args),"portal");
        planSettings=PlanSettings.parse(getConfig().getAll());publishPlanSnapshot();
        var plugin=getServer().getPluginManager().getPlugin("Plan");
        if(!planControl.shouldRegister())return;
        try {
            PlanExtension extension=new PlanExtension(planSettings,()->planSnapshot,Main::text);
            if(extension.register()){plan=extension;planControl.registered(true);getLogger().info(text("log.plan.ready"));}
            else getLogger().warning(text("log.plan.unsupported"));
        } catch(RuntimeException | LinkageError e){getLogger().warning(text("log.plan.unavailable",e.getClass().getSimpleName()));}
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


    public String planStatusJson(){return planControl.statusJson();}
    public boolean configurePlan(String mode,String[] fields){return planControl.configure(mode,fields);}
    public boolean planCommand(CommandSender sender,String[] args){return planControl!=null && planControl.command(sender,args,"tokaPortal.admin");}
}
