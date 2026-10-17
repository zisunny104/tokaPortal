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
    private static final dev.toka.i18n.Messages MESSAGES=new dev.toka.i18n.Messages(TokaHomeStorageTest.class.getClassLoader(),Path.of("target","no-lang-folder"),"zh-TW",line->{});
    private ConfigSection data(){ConfigSection root=HomeStorage.empty();root.set("custom",new LinkedHashMap<>(Map.of("long",9007199254740993L,"text","臺灣")));Map<String,Object> home=new LinkedHashMap<>();home.put("name","家");home.put("creator","玩家");home.put("type","home");home.put("loc",new LinkedHashMap<>(Map.of("level","world","x",1,"y",64,"z",2,"yaw",0,"pitch",0)));root.set("homes",new ArrayList<>(List.of(home)));return root;}
    private void verify(ConfigSection actual){assertEquals("家",((Map<?,?>)actual.getList("homes").getFirst()).get("name"));assertEquals(9007199254740993L,((Number)((Map<?,?>)actual.get("custom")).get("long")).longValue());}
    @Test void sharedBackendsPreserveDataAndRejectInvalidSave()throws Exception {
        for(Backend backend:Backend.values()){Path dir=folder.resolve(backend.name());try(HomeStorage store=new TokaHomeStorage(dir,backend)){store.save(data());ConfigSection bad=data();bad.set("homes",List.of("broken"));assertThrows(IOException.class,()->store.save(bad));verify(store.load());}try(HomeStorage store=new TokaHomeStorage(dir,backend)){verify(store.load());}}
    }
    @Test void newInstallDefaultsSqliteAndConfigSwitchDoesNotReadEmpty()throws Exception {
        try(HomeStorage storage=StorageManager.openConfigured(folder,new ConfigSection(),MESSAGES)){storage.save(data());}
        assertTrue(Files.isRegularFile(folder.resolve("storage/sqlite/documents.sqlite")));ConfigSection yaml=new ConfigSection();yaml.set("storage",Map.of("backend","yaml"));assertThrows(IOException.class,()->StorageManager.openConfigured(folder,yaml,MESSAGES));assertFalse(Files.exists(folder.resolve("storage/yaml")));
    }
    @Test void oldFilesOfTheLegacyFormatAreIgnored()throws Exception {
        Files.writeString(folder.resolve("data.yml"),"homes: [broken]");Files.writeString(folder.resolve("homes.db"),"not a database");
        byte[] yaml=Files.readAllBytes(folder.resolve("data.yml"));byte[] db=Files.readAllBytes(folder.resolve("homes.db"));
        try(HomeStorage storage=StorageManager.openConfigured(folder,new ConfigSection(),MESSAGES)){storage.save(data());verify(storage.load());}
        assertArrayEquals(yaml,Files.readAllBytes(folder.resolve("data.yml")));assertArrayEquals(db,Files.readAllBytes(folder.resolve("homes.db")));
    }
    @Test void theOldStorageTypeIsRefusedWithAClearMessage()throws Exception {
        ConfigSection old=new ConfigSection();old.set("storage",Map.of("type","sqlite"));
        IOException error=assertThrows(IOException.class,()->StorageManager.openConfigured(folder,old,MESSAGES));
        assertTrue(error.getMessage().contains("storage.backend"));assertTrue(error.getMessage().startsWith("§c"));assertFalse(Files.exists(folder.resolve("storage")));
    }
    @Test void theBackendThatWasUsedIsKeptAndOtherDirectoriesAreNotMistaken()throws Exception {
        try(HomeStorage storage=StorageManager.openConfigured(folder,new ConfigSection(),MESSAGES)){storage.save(data());}
        assertEquals("sqlite",Files.readString(folder.resolve("storage/active-backend.txt")).strip());
        try(HomeStorage storage=StorageManager.openConfigured(folder,new ConfigSection(),MESSAGES)){verify(storage.load());}
        Path other=Files.createDirectory(folder.resolve("other"));Files.createDirectories(other.resolve("storage/json"));
        assertThrows(IOException.class,()->StorageManager.openConfigured(other,new ConfigSection(),MESSAGES));
    }
}
