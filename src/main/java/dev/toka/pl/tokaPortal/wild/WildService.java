package dev.toka.pl.tokaPortal.wild;
import cn.nukkit.Player;
import cn.nukkit.level.Position;
import cn.nukkit.block.BlockID;
import dev.toka.pl.tokaPortal.Main;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
public final class WildService {
 private final Main plugin;private final Cooldowns cooldowns=new Cooldowns(System::nanoTime);
 public WildService(Main plugin){this.plugin=plugin;}
 public long remainingMillis(UUID player){return cooldowns.remainingMillis(player);}
 private int seconds(){return Math.max(1,Math.min(86400,plugin.getConfig().getInt("wild.cooldown-seconds",40)));}
 public void execute(Player player){
  var worlds=plugin.getConfig().getStringList("wild.worlds");
  if(!worlds.contains(player.getLevel().getName())){player.sendMessage(Main.text("err.wild.worldOnly",worlds.isEmpty()?"-":String.join("、",worlds)));return;}
  long remaining=remainingMillis(player.getUniqueId());
  if(remaining>0){player.sendMessage(Main.text("err.wild.cooldown",(remaining+999)/1000));return;}
  var level=player.getLevel();int radius=Math.max(1,Math.min(10000,plugin.getConfig().getInt("wild.radius",375)));
  int attempts=Math.max(1,Math.min(64,plugin.getConfig().getInt("wild.attempts",16)));
  int originX=player.getFloorX(),originZ=player.getFloorZ();
  for(int i=0;i<attempts;i++){
   int x=originX+ThreadLocalRandom.current().nextInt(-radius,radius+1),z=originZ+ThreadLocalRandom.current().nextInt(-radius,radius+1);
   if(level.getChunk(x>>4,z>>4,false)==null)continue; // Never generate terrain merely to search.
   int y=level.getHighestBlockAt(x,z)+1;
   var floor=level.getBlock(x,y-1,z);int id=floor.getId();
   if(!floor.isSolid()||id==BlockID.CACTUS||id==BlockID.MAGMA||level.getBlock(x,y,z).getId()!=BlockID.AIR||level.getBlock(x,y+1,z).getId()!=BlockID.AIR)continue;
   if(!player.teleport(new Position(x+0.5,y,z+0.5,level))){player.sendMessage(Main.text("err.wild.cancelled"));return;}
   cooldowns.start(player.getUniqueId(),seconds());player.sendMessage(Main.text("msg.wild.success"));render();return;
  }
  player.sendMessage(Main.text("err.wild.noSafeSpot"));
 }
 private final java.util.Set<UUID> shown=new java.util.HashSet<>();
 private Class<?> boundType;private java.lang.reflect.Method pin,bossBar,unpin;
 private static java.lang.reflect.Method find(Class<?> type,String name,Class<?>... params){try{return type.getMethod(name,params);}catch(NoSuchMethodException missing){return null;}}
 /** tokaDisplay is reached by reflection; the methods are looked up once per display class, not on every render. */
 private void bind(Class<?> type){
  boundType=type;
  pin=find(type,"pin",Player.class,String.class,String.class,int.class,int.class,float.class,String.class);
  bossBar=find(type,"bossBar",Player.class,String.class,String.class,float.class,int.class,int.class);
  unpin=find(type,"unpin",Player.class,String.class);
 }
 /** The cooldown is one pinned line under the base information of the boss bar, and it drives the bar. */
 public void render(){
  var active=cooldowns.players();
  var display=plugin.getServer().getPluginManager().getPlugin("tokaDisplay");if(display==null||!display.isEnabled()||!plugin.getConfig().getBoolean("wild.show-cooldown",true))return;
  if(display.getClass()!=boundType)bind(display.getClass());
  if(!active.isEmpty()){
   int total=seconds();String color=plugin.getConfig().getString("wild.bar-color","cooldown");
   for(UUID uuid:active){
    Player player=plugin.getServer().getOnlinePlayers().get(uuid);if(player==null)continue;
    long left=remainingMillis(uuid);float progress=Math.max(0,Math.min(100,100f-left*100f/(total*1000f)));
    String text=Main.text("ui.wild.cooldownBar",(left+999)/1000);
    try{
     if(pin!=null)pin.invoke(null,player,"portal:wild",text,3,10,progress,color);
     else if(bossBar!=null)bossBar.invoke(null,player,"portal:wild",text,progress,100,25);
     else continue;
     shown.add(uuid);
    }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){/* Display cannot change business readiness. */}
   }
  }
  if(shown.isEmpty())return;
  for(var uuid:new java.util.ArrayList<>(shown)){
   if(active.contains(uuid))continue;
   shown.remove(uuid);Player player=plugin.getServer().getOnlinePlayers().get(uuid);if(player==null||unpin==null)continue;
   try{unpin.invoke(null,player,"portal:wild");}
   catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){/* The line goes away by itself after its time. */}
  }
 }
 public void close(){cooldowns.clear();var display=plugin.getServer().getPluginManager().getPlugin("tokaDisplay");if(display!=null&&display.isEnabled())try{display.getClass().getMethod("removeSource",String.class).invoke(null,"portal:wild");}catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){}}
}

