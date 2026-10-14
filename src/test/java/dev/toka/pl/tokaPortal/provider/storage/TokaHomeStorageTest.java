package dev.toka.pl.tokaPortal.provider.storage;
import cn.nukkit.utils.ConfigSection;
import dev.toka.storage.Backend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class TokaHomeStorageTest {
    @TempDir Path folder;
    private ConfigSection data(){ConfigSection root=HomeStorage.empty();root.set("custom",new LinkedHashMap<>(Map.of("long",9007199254740993L,"text","臺灣")));Map<String,Object> home=new LinkedHashMap<>();home.put("name","家");home.put("creator","玩家");home.put("type","home");home.put("loc",new LinkedHashMap<>(Map.of("level","world","x",1,"y",64,"z",2,"yaw",0,"pitch",0)));root.set("homes",new ArrayList<>(List.of(home)));return root;}
    private void verify(ConfigSection actual){assertEquals("家",((Map<?,?>)actual.getList("homes").getFirst()).get("name"));assertEquals(9007199254740993L,((Number)((Map<?,?>)actual.get("custom")).get("long")).longValue());}
    @Test void sharedBackendsPreserveDataAndRejectInvalidSave()throws Exception {
        for(Backend backend:Backend.values()){Path dir=folder.resolve(backend.name());try(HomeStorage store=new TokaHomeStorage(dir,backend)){store.save(data());ConfigSection bad=data();bad.set("homes",List.of("broken"));assertThrows(IOException.class,()->store.save(bad));verify(store.load());}try(HomeStorage store=new TokaHomeStorage(dir,backend)){verify(store.load());}}
    }
    @Test void newInstallDefaultsSqliteAndConfigSwitchDoesNotReadEmpty()throws Exception {
        try(HomeStorage storage=StorageManager.openConfigured(folder,new ConfigSection())){storage.save(data());}
        assertTrue(Files.isRegularFile(folder.resolve("storage/sqlite/documents.sqlite")));ConfigSection yaml=new ConfigSection();yaml.set("storage",Map.of("backend","yaml"));assertThrows(IOException.class,()->StorageManager.openConfigured(folder,yaml));assertFalse(Files.exists(folder.resolve("storage/yaml")));
    }
    @Test void existingLegacyConfigKeepsDataUntilExplicitImport()throws Exception {
        try(HomeStorage storage=new YamlHomeStorage(folder.resolve("data.yml"))){storage.save(data());}
        byte[] source=Files.readAllBytes(folder.resolve("data.yml"));ConfigSection legacy=new ConfigSection();legacy.set("storage",Map.of("type","yaml"));try(HomeStorage storage=StorageManager.openConfigured(folder,legacy)){verify(storage.load());}
        assertThrows(IOException.class,()->StorageManager.openConfigured(folder,new ConfigSection()));assertArrayEquals(source,Files.readAllBytes(folder.resolve("data.yml")));
    }
    @Test void legacyImportEachSourceEachTargetPreservesOriginalAndBackup()throws Exception {
        for(String source:List.of("yaml","sqlite"))for(Backend target:Backend.values()){
            Path dir=Files.createDirectory(folder.resolve(source+target));Path sourcePath=dir.resolve(source.equals("yaml")?"data.yml":"homes.db");try(HomeStorage storage=source.equals("yaml")?new YamlHomeStorage(sourcePath):new SqliteHomeStorage(sourcePath)){storage.save(data());}
            byte[] before=Files.readAllBytes(sourcePath);var result=LegacyHomeImport.migrate(dir,source,target,folder.resolve("backups"));assertArrayEquals(before,Files.readAllBytes(sourcePath));assertArrayEquals(before,Files.readAllBytes(result.backup()));
            ConfigSection config=new ConfigSection();config.set("storage",Map.of("backend",target.name().toLowerCase(Locale.ROOT)));try(HomeStorage storage=StorageManager.openConfigured(dir,config)){verify(storage.load());}
            assertThrows(IOException.class,()->LegacyHomeImport.migrate(dir,source,target,folder.resolve("backups")));
            ConfigSection old=new ConfigSection();old.set("storage",Map.of("type",source));assertThrows(IOException.class,()->StorageManager.openConfigured(dir,old));
        }
    }
    @Test void brokenSourceAndExistingTargetRemainUntouched()throws Exception {
        Files.writeString(folder.resolve("data.yml"),"homes: [broken]");byte[] original=Files.readAllBytes(folder.resolve("data.yml"));assertThrows(IOException.class,()->LegacyHomeImport.migrate(folder,"yaml",Backend.SQLITE,folder.resolve("backups")));assertArrayEquals(original,Files.readAllBytes(folder.resolve("data.yml")));assertFalse(Files.exists(folder.resolve("storage/sqlite")));
        Files.createDirectories(folder.resolve("storage/sqlite"));Files.writeString(folder.resolve("storage/sqlite/keep"),"original");assertThrows(IOException.class,()->LegacyHomeImport.migrate(folder,"yaml",Backend.SQLITE,folder.resolve("backups")));assertEquals("original",Files.readString(folder.resolve("storage/sqlite/keep")));
    }
}
