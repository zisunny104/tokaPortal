package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint;

import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint.*;

public class PortalCommand extends Command {

    public PortalCommand() {
        super("portal", "傳送系統指令");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            String pl = player.getName();
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "tpp":
                    case "point": {
                        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(player,
                                dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.POINTS,
                                dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.POINT_MANAGEMENT)) return false;
                        if (args.length < 2) { player.sendMessage("/portal point <set|del> <名稱> [類型]"); return false; }
                        if (args[1] != null) {
                            switch (args[1].toLowerCase()) {
                                case "add":
                                case "set": {
                                    if (args.length > 2) {
                                        String name = args[2];
                                        String type = "point";
                                        String info = name;
                                        Location location = player.getLocation();
                                        if (args.length > 3) {
                                            type = args[3];
                                        }

                                        if (getPoint(name) == null) {
                                            if (!setTeleportPoint(name, pl, type, info, location)) {
                                                player.sendMessage("[傳送]傳送點建立失敗，整合功能已關閉。");
                                                return false;
                                            }
                                            pli.sendText("[傳送]傳送點 %name 建立成功"
                                                    .replace("%name", name));
                                        } else {
                                            pli.sendText("[傳送]傳送點建立失敗(重名)");
                                        }
                                    } else {
                                        pli.sendText("[傳送]傳送點建立失敗 請輸入有效名稱");
                                    }
                                    break;
                                }

                                case "del": {
                                    if (args.length > 2) {
                                        String name = args[2];
                                        teleportPoint point = getPoint(name);
                                        if (point != null) {
                                            if (point.getCreator().equals(pl) || player.isOp()) {
                                                if (!delTeleportPoint(point)) {
                                                    player.sendMessage("[傳送]傳送點刪除失敗，整合功能已關閉。");
                                                    return false;
                                                }
                                                pli.sendText("[傳送]傳送點 %name 成功移除"
                                                        .replace("%name", name));
                                            } else {
                                                pli.sendText("[傳送]傳送點刪除失敗 權限不足");
                                            }
                                        } else {
                                            pli.sendText("[傳送]傳送點刪除失敗 傳送點不存在");
                                        }
                                    } else {
                                        pli.sendText("[傳送]傳送點刪除失敗 請輸入有效名稱");
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    case "server": {
                        player.sendMessage("[傳送]尚未提供跨伺服器目的地管理功能。");
                        break;
                    }

                    case "help": {
                        sender.sendMessage("/back - 返回前次傳送點\n/next - 前往下個傳送點");
                        break;
                    }
                }
            } else {
                PortalWindow.sendPortalMainWindow(player);
            }
            return true;
        } else {
            sender.sendMessage("僅供遊戲內使用");
            return false;
        }
    }
}
