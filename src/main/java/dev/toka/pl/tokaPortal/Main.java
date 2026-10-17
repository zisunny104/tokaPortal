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

public class Main extends PluginBase implements Listener {

    private static Main instance;
    private static IDataProvider provider;
    private Messages messages;
    private dev.toka.pl.tokaPortal.provider.storage.WriteQueue writer;
    private dev.toka.pl.tokaPortal.point.PublicPoints publicPoints;
    public dev.toka.pl.tokaPortal.point.PublicPoints getPublicPoints(){return publicPoints;}
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

        wildService=new dev.toka.pl.tokaPortal.wild.WildService(this);
        getServer().getScheduler().scheduleRepeatingTask(this,()->wildService.render(),20);
        this.registerEvents();
        this.registerCommandMap();
    }

    @Override
    public void onDisable() {
        if(wildService!=null)wildService.close();
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
        dev.toka.forms.FormListener.register(this);
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
        if (messages != null) messages.language(getConfig().getString("language", "zh-TW"));
        drainWrites();
        if (publicPoints != null) { try {publicPoints.close();} catch(java.io.IOException e){getLogger().error(text("log.points.closeFailed"),e);} publicPoints=null;}
        if (provider != null) { provider.close(false); provider = null; }
        String format = getConfig().getNested("storage.backend", "sqlite");
        try {
            var storage = StorageManager.openConfigured(getDataFolder().toPath(), getConfig().getAll());
            provider = new BaseDataProvider(storage, writer, format);
            var backend=dev.toka.storage.Backend.valueOf(format.toUpperCase(java.util.Locale.ROOT));
            publicPoints=new dev.toka.pl.tokaPortal.point.PublicPoints(getDataFolder().toPath().resolve("storage").resolve(format).resolve("public-points"),backend,writer);
            getLogger().info(text("log.points.loaded", publicPoints.points().size()));
            int homes = storage.load().getList("homes").size();
            getLogger().info(text("log.homes.loaded", homes));
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalStateException(text("err.storageOpen"), e);
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

