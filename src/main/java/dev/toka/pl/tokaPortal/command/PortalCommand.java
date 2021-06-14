package dev.toka.pl.tokaPortal.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.ser.portal.teleportPoint.teleportPoint;

import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.ser.portal.teleportPoint.teleportPoint.*;

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
                        if (args[1] != null) {
                            switch (args[1].toLowerCase()) {
                                case "add":
                                case "set": {
                                    if (args[2] != null) {
                                        String name = args[2];
                                        String type = "point";
                                        String info = name;
                                        Location location = player.getLocation();
                                        if (args[3] != null) {
                                            type = args[3];
                                        }

                                        if (getPoint(name) == null) {
                                            setTeleportPoint(name, pl, type, info, location);
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
                                    if (args[2] != null) {
                                        String name = args[2];
                                        teleportPoint point = getPoint(name);
                                        if (point != null) {
                                            if (point.getCreator().equals(pl) || player.isOp()) {
                                                delTeleportPoint(point);
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
                        if (args[1] != null) {
                            switch (args[1].toLowerCase()) {
                                case "add":
                                case "set": {
                                    if (args.length >= 4) {
                                        String address = args[2];
                                        int rawPort = Integer.parseInt(args[3]), port;
                                        if (rawPort < 65535 && rawPort > 1) {
                                            port = Integer.parseInt(args[3]);
                                            pli.sendText("[]暫不支持寫入的喔這樣");
                                        } else {
                                            pli.sendText("[]無效port的啦");
                                        }

                                    }
                                    break;
                                }

                                case "del": {
                                    //TODO
                                }
                            }
                        }
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