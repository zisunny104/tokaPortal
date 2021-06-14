package dev.toka.pl.tokaPortal.point;

import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import dev.toka.pl.tokaPortal.utils.Utils;

import static dev.toka.pl.tokaPortal.utils.Utils.encodeLocation;

public class HomePoint implements BasePoint {
    private IDataProvider provider;

    int id = -1, x, y, z, yaw, pitch;
    String name, type, creator, level;
    Location loc;

    public HomePoint() {

    }

    public HomePoint(IDataProvider provider, int id, String name, String type, String creator, Location loc) {
        this.provider = provider;
        this.id = id;
        this.name = name;
        this.type = type;
        this.creator = creator;
        this.x = (int) loc.x;
        this.y = (int) loc.y;
        this.z = (int) loc.z;
        this.yaw = (int) loc.yaw;
        this.pitch = (int) loc.getPitch();
        this.level = loc.getLevelName();
        this.loc = loc;
    }

    public HomePoint(IDataProvider provider, int id, ConfigSection data) {
        this.id = id;
        this.provider = provider;

        this.name = data.getString("name");
        this.level = data.getString("type");
        Location loc = Utils.parseLocation(data.getSection("loc"));
        this.x = (int) loc.x;
        this.y = (int) loc.y;
        this.z = (int) loc.z;
        this.yaw = (int) loc.yaw;
        this.pitch = (int) loc.getPitch();
        this.level = loc.getLevelName();
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

    public ConfigSection getRawData() {
        return new ConfigSection() {
            {
                set("name", name);
                set("type", type);
                set("loc", encodeLocation(loc));
            }
        };
    }
}
