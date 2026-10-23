package dev.toka.pl.tokaPortal.integration;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import com.google.gson.*;
import dev.toka.pl.tokaPortal.Main;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/** Optional tokaLand bridge. JSON keeps this plugin independent of tokaLand's class loader and model. */
public final class LandIntegration {
    public record Target(String id,String name,String world,double x,double y,double z,float yaw,float pitch) {}
    public record Lookup(String status,Target target) {}
    private static boolean warned;

    private LandIntegration() {}

    public static boolean available() { return plugin() != null; }

    /** Empty Optional means tokaLand/API is unavailable; a present empty list means it is ready with no targets. */
    public static Optional<List<Target>> targets(Player player) {
        JsonObject reply=invoke("queryTeleportTargetsJson",new Class<?>[]{Player.class},player);
        if(reply==null||!reply.has("status")||!"READY".equals(reply.get("status").getAsString()))return Optional.empty();
        return Optional.of(readTargets(reply));
    }

    public static Lookup lookup(Player player,String idOrName) {
        JsonObject reply=invoke("queryTeleportTargetJson",new Class<?>[]{Player.class,String.class},player,idOrName);
        if(reply==null)return new Lookup("UNAVAILABLE",null);
        String status=reply.has("status")?reply.get("status").getAsString():"UNAVAILABLE";
        List<Target> rows=readTargets(reply);
        return new Lookup(status,rows.isEmpty()?null:rows.getFirst());
    }

    private static List<Target> readTargets(JsonObject reply) {
        if(!reply.has("targets")||!reply.get("targets").isJsonArray())return List.of();
        List<Target> out=new ArrayList<>();
        for(JsonElement item:reply.getAsJsonArray("targets"))try{
            JsonObject row=item.getAsJsonObject();
            out.add(new Target(row.get("id").getAsString(),row.get("name").getAsString(),row.get("world").getAsString(),
                    row.get("x").getAsDouble(),row.get("y").getAsDouble(),row.get("z").getAsDouble(),
                    row.get("yaw").getAsFloat(),row.get("pitch").getAsFloat()));
        }catch(RuntimeException ignored){}
        return List.copyOf(out);
    }

    private static JsonObject invoke(String methodName,Class<?>[] parameterTypes,Object... args) {
        Plugin plugin=plugin();if(plugin==null)return null;
        try{
            Class<?> api=Class.forName("dev.toka.land.Main",false,plugin.getClass().getClassLoader());
            Method method=api.getMethod(methodName,parameterTypes);
            if(!Modifier.isStatic(method.getModifiers())||method.getReturnType()!=String.class)
                throw new NoSuchMethodException(methodName+" has an incompatible signature");
            return JsonParser.parseString((String)method.invoke(null,args)).getAsJsonObject();
        }catch(ReflectiveOperationException|LinkageError|RuntimeException failure){
            if(!warned){warned=true;Main.getInstance().getLogger().warning(Main.text("log.land.unavailable",failure.getClass().getSimpleName()));}
            return null;
        }
    }

    private static Plugin plugin() {
        Plugin plugin=Server.getInstance().getPluginManager().getPlugin("tokaLand");
        return plugin!=null&&plugin.isEnabled()?plugin:null;
    }
}
