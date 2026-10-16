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
  var worlds=plugin.getConfig().exists("wild.worlds")?plugin.getConfig().getStringList("wild.worlds"):java.util.List.of("ZiYuan");
  if(!worlds.contains(player.getLevel().getName())){player.sendMessage("[傳送]僅可在指定資源世界使用");return;}
  long remaining=remainingMillis(player.getUniqueId());
  if(remaining>0){player.sendMessage("[傳送]請等待 "+((remaining+999)/1000)+" 秒後再使用");return;}
  var level=player.getLevel();int radius=Math.max(1,Math.min(10000,plugin.getConfig().getInt("wild.radius",375)));
  int attempts=Math.max(1,Math.min(64,plugin.getConfig().getInt("wild.attempts",16)));
  int originX=player.getFloorX(),originZ=player.getFloorZ();
  for(int i=0;i<attempts;i++){
   int x=originX+ThreadLocalRandom.current().nextInt(-radius,radius+1),z=originZ+ThreadLocalRandom.current().nextInt(-radius,radius+1);
   if(level.getChunk(x>>4,z>>4,false)==null)continue; // Never generate terrain merely to search.
   int y=level.getHighestBlockAt(x,z)+1;
   var floor=level.getBlock(x,y-1,z);int id=floor.getId();
   if(!floor.isSolid()||id==BlockID.CACTUS||id==BlockID.MAGMA||level.getBlock(x,y,z).getId()!=BlockID.AIR||level.getBlock(x,y+1,z).getId()!=BlockID.AIR)continue;
   if(!player.teleport(new Position(x+0.5,y,z+0.5,level))){player.sendMessage("[傳送]傳送被取消，沒有扣除冷卻");return;}
   cooldowns.start(player.getUniqueId(),seconds());player.sendMessage("[傳送]隨機傳送成功");render();return;
  }
  player.sendMessage("[傳送]暫時找不到安全落點，沒有扣除冷卻，請稍後再試");
 }
 public void render(){
  var active=cooldowns.players();
  var display=plugin.getServer().getPluginManager().getPlugin("tokaDisplay");if(display==null||!display.isEnabled()||!plugin.getConfig().getBoolean("wild.show-cooldown",true))return;
  for(UUID uuid:active){
   Player player=plugin.getServer().getOnlinePlayers().get(uuid);if(player==null)continue;
   long left=remainingMillis(uuid);float progress=Math.max(0,Math.min(100,100f-left*100f/(seconds()*1000f)));
   try{display.getClass().getMethod("bossBar",Player.class,String.class,String.class,float.class,int.class,int.class).invoke(null,player,"portal:wild","[隨機傳送]冷卻剩餘 "+((left+999)/1000)+" 秒",progress,100,25);}
   catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){/* Display cannot change business readiness. */}
  }
 }
 public void close(){cooldowns.clear();var display=plugin.getServer().getPluginManager().getPlugin("tokaDisplay");if(display!=null&&display.isEnabled())try{display.getClass().getMethod("removeSource",String.class).invoke(null,"portal:wild");}catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){}}
}

