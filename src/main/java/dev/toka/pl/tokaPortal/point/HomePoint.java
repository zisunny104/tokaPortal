package dev.toka.pl.tokaPortal.point;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.utils.Utils;

public class HomePoint implements BasePoint {
    private final int id;
    private final String name, type, creator, level;
    private final Location loc;
    private ConfigSection original = new ConfigSection();

    public HomePoint(int id, String name, String type, String creator, Location loc) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creator = creator;
        this.level = loc.getLevel().getName();
        this.loc = loc;
    }

    public HomePoint(int id, ConfigSection data) {
        this.original = new ConfigSection(data);
        this.id = id;

        this.name = data.getString("name");
        this.type = data.getString("type");
        this.creator = data.getString("creator");
        Location loc = Utils.parseLocation(data.getSection("loc"));
        this.level = data.getSection("loc").getString("level");
        this.loc = loc;
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

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public boolean isCreator(Object creator) {
        if (creator instanceof Player) {
            creator = ((Player) creator).getName();
        }
        if (creator instanceof String) {
            return this.creator.equals(creator);
        }
        return false;
    }

    public ConfigSection getRawData() {
        ConfigSection data = new ConfigSection(original);
        data.set("name", name);
        data.set("type", type);
        data.set("creator", creator);
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
