import cn.nukkit.plugin.PluginBase;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.Main;
public final class StorageProbe extends PluginBase {
    @Override public void onEnable(){getServer().getScheduler().scheduleDelayedTask(this,this::probe,2);}
    private void probe(){
        try {
            var portal=getServer().getPluginManager().getPlugin("tokaPortal");if(portal==null||!portal.isEnabled())throw new IllegalStateException("Portal not enabled");
            String expected=System.getProperty("probe.backend");if(!Main.getProvider().getName().equals(expected))throw new IllegalStateException("Wrong backend");
            Main.getProvider().addHomePoint("儲存抽查", "測試玩家",new Location(8,80,8,0,0,getServer().getDefaultLevel()));
            Main.getInstance().reload();if(Main.getProvider().getHomePoint("儲存抽查")==null)throw new IllegalStateException("Reload lost home");
            Main.getProvider().delHomePoint(Main.getProvider().getHomePoint("儲存抽查"));Main.getInstance().reload();
            if(Main.getProvider().getHomePoint("儲存抽查")!=null)throw new IllegalStateException("Delete did not persist");
            getLogger().info("PORTAL_STORAGE_BOOT_PASS Backend="+expected);
        }catch(Throwable error){getLogger().error("PORTAL_STORAGE_BOOT_FAIL",error);}
        finally{getServer().shutdown();new Thread(()->{try{Thread.sleep(1500);}catch(InterruptedException ignored){}System.exit(0);}).start();}
    }
}
