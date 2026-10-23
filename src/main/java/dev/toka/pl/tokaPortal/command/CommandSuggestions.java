package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.data.*;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.point.HomePoint;

import java.io.File;
import java.util.*;

final class CommandSuggestions {
    private CommandSuggestions() {}

    private static final long FOLDER_SCAN_TTL_MILLIS = 10_000L;
    private static List<String> folderNames = List.of();
    private static long folderScanAt;

    /** The world folders on disk, scanned at most once per interval because every refresh asks once per operator. */
    private static synchronized List<String> worldFolders(Server server) {
        long now = System.currentTimeMillis();
        if (now - folderScanAt < FOLDER_SCAN_TTL_MILLIS && folderScanAt != 0) return folderNames;
        List<String> found = new ArrayList<>();
        File[] folders = new File(server.getDataPath(), "worlds").listFiles(File::isDirectory);
        if (folders != null) for (File folder : folders) {
            if (server.isLevelGenerated(folder.getName())) found.add(folder.getName());
        }
        folderNames = found;
        folderScanAt = now;
        return found;
    }

    /** Forgets the scanned folders so a reloaded plugin starts fresh. */
    static synchronized void reset() {
        folderNames = List.of();
        folderScanAt = 0;
    }

    static List<String> worldNames() {
        Server server = Server.getInstance();
        Set<String> names = new TreeSet<>();
        server.getLevels().values().forEach(level -> names.add(level.getName()));
        names.addAll(worldFolders(server));
        return new ArrayList<>(names);
    }

    static List<String> playerNames(Player player) {
        return Server.getInstance().getOnlinePlayers().values().stream()
                .filter(other -> other != player).map(Player::getName).sorted().toList();
    }

    static List<String> homeNames(Player player) {
        if (Main.getProvider() == null) return List.of();
        return Arrays.stream(Main.getProvider().getHomePointsByCreator(player))
                .map(HomePoint::getName).sorted().toList();
    }

    /** A parameter that offers fixed words; the words must be lower case. */
    static CommandParameter fixed(String label, boolean optional, String enumName, String... values) {
        return CommandParameter.newEnum(label, optional, new CommandEnum(enumName, values));
    }

    /** The overload that completes the word help. */
    static CommandParameter[] help() {
        return new CommandParameter[]{fixed(Main.text("param.action"), false, "TokaPortalHelp", "help")};
    }

    static List<String> pointNames() {
        if (Main.getInstance().getPublicPoints() == null) return List.of();
        return Main.getInstance().getPublicPoints().points().keySet().stream().map(String::valueOf).sorted().toList();
    }

    static CommandParameter names(String label, boolean optional, String enumName, List<String> values) {
        // 客戶端只接受小寫候選字，含大寫的名稱不送，避免客戶端記錄錯誤。
        List<String> accepted = values.stream().filter(value -> value.equals(value.toLowerCase(Locale.ROOT))).toList();
        return CommandParameter.newEnum(label, optional, new CommandEnum(enumName, accepted));
    }

    static CommandDataVersions parameters(CommandDataVersions data, CommandParameter[]... alternatives) {
        if (data == null) return null;
        for (CommandData version : data.versions) {
            // 每位玩家使用獨立的參數表，避免住家清單混入其他玩家的指令封包。
            version.overloads = new LinkedHashMap<>();
            for (int i = 0; i < alternatives.length; i++) {
                CommandOverload overload = new CommandOverload();
                overload.input.parameters = alternatives[i];
                version.overloads.put("choice" + i, overload);
            }
        }
        return data;
    }
}
