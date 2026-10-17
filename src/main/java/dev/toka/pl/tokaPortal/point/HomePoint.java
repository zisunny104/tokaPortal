package dev.toka.pl.tokaPortal.point;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.utils.Utils;

import java.util.UUID;

/**
 * 住家。擁有者以 UUID（owner）為準；ownerName 是最後已知名稱，只用來顯示與比對舊資料。
 * 舊資料只有 creator（玩家名稱）：owner 為 null 的是 legacy 住家，待同名玩家登入取回。
 * 寫出時 creator 一律照寫為最後已知名稱，舊版 jar 仍讀得懂。
 */
public class HomePoint implements BasePoint {
    private final int id;
    private final String name, type, level;
    private final Location loc;
    private UUID owner;
    private String ownerName;
    private ConfigSection original = new ConfigSection();

    /** 只有名稱的 legacy 住家（舊 API）。 */
    public HomePoint(int id, String name, String type, String creator, Location loc) {
        this(id, name, type, null, creator, loc);
    }

    public HomePoint(int id, String name, String type, UUID owner, String ownerName, Location loc) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.ownerName = ownerName;
        this.level = loc.getLevel().getName();
        this.loc = loc;
    }

    public HomePoint(int id, ConfigSection data) {
        this.original = new ConfigSection(data);
        this.id = id;

        this.name = data.getString("name");
        this.type = data.getString("type");
        this.owner = parseUuid(data.getString("owner", ""));
        String shown = data.getString("ownerName", "");
        this.ownerName = shown.isBlank() ? data.getString("creator") : shown;
        Location loc = Utils.parseLocation(data.getSection("loc"));
        this.level = data.getSection("loc").getString("level");
        this.loc = loc;
    }

    /** 不是合法 UUID 字串時回傳 null（視為沒有 owner）。 */
    public static UUID parseUuid(String text) {
        if (text == null || text.isBlank()) return null;
        try { return UUID.fromString(text.strip()); }
        catch (IllegalArgumentException e) { return null; }
    }

    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    /** 最後已知的擁有者名稱（相容舊 API）。 */
    @Override
    public String getCreator() {
        return ownerName;
    }

    /** 擁有者 UUID；legacy 住家為 null。 */
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isLegacy() {
        return owner == null;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /** 把 legacy 住家交給玩家（名稱同時更新為其目前名稱）。 */
    public void claim(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    /** Player 與 UUID 以 UUID 比對；字串為名稱（不分大小寫），只作為舊 API 相容。legacy 住家不屬於任何 Player／UUID。 */
    @Override
    public boolean isCreator(Object creator) {
        if (creator instanceof Player player) creator = player.getUniqueId();
        if (creator instanceof UUID uuid) return uuid.equals(owner);
        if (creator instanceof String text) return ownerName != null && ownerName.equalsIgnoreCase(text);
        return false;
    }

    public ConfigSection getRawData() {
        ConfigSection data = new ConfigSection(original);
        data.set("name", name);
        data.set("type", type);
        data.set("creator", ownerName);
        if (owner != null) data.set("owner", owner.toString());
        data.set("ownerName", ownerName);
        ConfigSection oldPosition = original.getSection("loc");
        ConfigSection position = oldPosition == null ? new ConfigSection() : new ConfigSection(oldPosition);
        position.set("x", loc.x);
        position.set("y", loc.y);
        position.set("z", loc.z);
        position.set("yaw", loc.yaw);
        position.set("pitch", loc.pitch);
        position.set("level", level);
        data.set("loc", position);
        return data;
    }
}
