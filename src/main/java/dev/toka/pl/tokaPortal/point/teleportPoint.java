package dev.toka.pl.tokaPortal.point;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import com.google.gson.JsonObject;
import dev.toka.pl.tokaPortal.Main;
import java.util.Map;
/** Immutable destination backed by the Portal public-point repository. */
public final class teleportPoint {
 private final JsonObject row;
 teleportPoint(JsonObject row){this.row=row.deepCopy();}
 public String getName(){return row.get("name").getAsString();}
 public String getInfo(){return row.get("info").getAsString();}
 public String getType(){return row.get("type").getAsString();}
 public String getCreator(){return row.get("creator").getAsString();}
 public Location getLocation(){var level=Server.getInstance().getLevelByName(row.get("world").getAsString());if(level==null)return null;return new Location(row.get("x").getAsDouble(),row.get("y").getAsDouble(),row.get("z").getAsDouble(),row.get("yaw").getAsDouble(),row.get("pitch").getAsDouble(),level);}
 public static Map<Object,teleportPoint> getPoints(){return Main.getInstance().getPublicPoints().points();}
 public static teleportPoint getPoint(String name){return getPoints().get(name);}
 public static boolean setTeleportPoint(String name,String creator,String type,String info,Location location){
  JsonObject row=new JsonObject();row.addProperty("name",name);row.addProperty("creator",creator);row.addProperty("type",type);row.addProperty("info",info);row.addProperty("world",location.getLevel().getName());row.addProperty("x",location.x);row.addProperty("y",location.y);row.addProperty("z",location.z);row.addProperty("yaw",location.yaw);row.addProperty("pitch",location.pitch);
  try{return Main.getInstance().getPublicPoints().create(row);}catch(java.io.IOException e){throw new IllegalStateException("Public point save failed",e);}
 }
 public static boolean delTeleportPoint(teleportPoint point){try{return Main.getInstance().getPublicPoints().delete(point.getName());}catch(java.io.IOException e){throw new IllegalStateException("Public point save failed",e);}}
}
