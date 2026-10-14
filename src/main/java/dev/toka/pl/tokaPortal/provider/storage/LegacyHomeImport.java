package dev.toka.pl.tokaPortal.provider.storage;
import cn.nukkit.utils.ConfigSection;
import dev.toka.storage.Backend;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Offline import only. Read from a copy so even legacy SQLite schema checks cannot mutate the source. */
public final class LegacyHomeImport {
    private LegacyHomeImport(){}
    public record Result(Path target,Path backup,int homes){}
    public static Result migrate(Path folder,String legacy,Backend backend,Path backups)throws IOException {
        folder=folder.toAbsolutePath().normalize();legacy=legacy.toLowerCase(Locale.ROOT);
        if(!Set.of("yaml","sqlite").contains(legacy))throw new IOException("來源僅支援 legacy yaml/sqlite。");
        Path source=folder.resolve(legacy.equals("yaml")?"data.yml":"homes.db");
        if(!Files.isRegularFile(source)||Files.isSymbolicLink(source))throw new IOException("找不到有效的來源住家資料。");
        for(String suffix:List.of("-wal","-shm","-journal"))if(Files.exists(source.resolveSibling(source.getFileName()+suffix)))throw new IOException("來源有未關閉的 SQLite journal；請完整停服。");
        Path root=folder.resolve("storage"),target=root.resolve(backend.name().toLowerCase(Locale.ROOT)),marker=root.resolve("active-backend.txt");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)||Files.exists(marker))throw new IOException("目的地或已啟用格式紀錄存在，停止匯入。");
        Path backupRoot=backups.toAbsolutePath().normalize();if(backupRoot.startsWith(root))throw new IOException("備份必須在 storage 之外。");
        Files.createDirectories(backupRoot);Path backup=backupRoot.resolve(source.getFileName()+".backup-"+UUID.randomUUID());
        Files.copy(source,backup);if(!Arrays.equals(hash(source),hash(backup)))throw new IOException("原始備份比對失敗。");
        Files.createDirectories(root);Path stage=root.resolve(".legacy-import-"+UUID.randomUUID());Files.createDirectory(stage);
        Path readCopy=stage.resolve(legacy.equals("yaml")?"source.yml":"source.db");Files.copy(backup,readCopy);
        try {
            ConfigSection document;
            try(HomeStorage reader=legacy.equals("yaml")?new YamlHomeStorage(readCopy):new SqliteHomeStorage(readCopy)){document=reader.load();}
            Files.delete(readCopy);
            try(HomeStorage destination=new TokaHomeStorage(stage,backend)){destination.save(document);verify(document,destination.load());}
            try(HomeStorage destination=new TokaHomeStorage(stage,backend)){verify(document,destination.load());}
            if(!Arrays.equals(hash(source),hash(backup)))throw new IOException("匯入期間來源內容改變，停止。");
            HomeStorage.writeAtomic(stage.resolve("legacy-import.ok"),("Source="+source+"\nBackup="+backup+"\nHomes="+document.getList("homes").size()+"\n").getBytes(StandardCharsets.UTF_8));
            Files.move(stage,target); // Refuse overwrite; do not weaken into replacing existing world/player data.
            HomeStorage.writeAtomic(marker,backend.name().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return new Result(target,backup,document.getList("homes").size());
        }catch(IOException|RuntimeException error){if(Files.isDirectory(stage))Files.writeString(stage.resolve("IMPORT_FAILED.txt"),"Do not activate this staging directory. Source and backup retained.\n");throw error;}
    }
    private static void verify(ConfigSection expected,ConfigSection actual)throws IOException {
        // JSON preserves scalar spellings here, avoiding approximate double equality during verification.
        String a=HomeStorage.JSON.toJson(HomeStorage.toMap(expected)),b=HomeStorage.JSON.toJson(HomeStorage.toMap(actual));
        if(!a.equals(b))throw new IOException("住家讀回驗證失敗。");
    }
    private static byte[] hash(Path path)throws IOException {
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(var input=Files.newInputStream(path)){byte[] bytes=new byte[65536];int n;while((n=input.read(bytes))>=0)digest.update(bytes,0,n);}return digest.digest();}catch(NoSuchAlgorithmException error){throw new AssertionError(error);}
    }
    public static void main(String[] args)throws IOException {
        if(args.length!=4)throw new IllegalArgumentException("Usage: <plugin-folder> <legacy-yaml|sqlite> <target-sqlite|json|yaml> <backup-directory>; stop server first");
        Result result=migrate(Path.of(args[0]),args[1],Backend.valueOf(args[2].toUpperCase(Locale.ROOT)),Path.of(args[3]));
        System.out.println("HOME_IMPORT_PASS Homes="+result.homes()+" Backup="+result.backup()+" Target="+result.target());
    }
}
