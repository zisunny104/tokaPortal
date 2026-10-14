package dev.toka.pl.tokaPortal.provider.storage;

import cn.nukkit.utils.ConfigSection;
import com.google.gson.*;
import dev.toka.storage.*;
import java.io.IOException;
import java.math.*;
import java.nio.file.Path;
import java.util.*;

/** Keeps the existing homes document and unknown fields, using the shared versioned store. */
public final class TokaHomeStorage implements HomeStorage {
    private final DocumentStore store;
    public TokaHomeStorage(Path directory,Backend backend)throws IOException {
        if(backend==Backend.SQLITE)try{Class.forName("org.sqlite.JDBC",true,TokaHomeStorage.class.getClassLoader());}catch(ClassNotFoundException error){throw new IOException("SQLite driver 不可用。",error);}
        store=DocumentStore.open(new StoreConfig(backend,directory,1));
    }
    @Override public ConfigSection load()throws IOException {
        Optional<JsonObject> document=store.load("homes");
        return document.isEmpty()?HomeStorage.empty():HomeStorage.validate(plain(document.orElseThrow()));
    }
    @Override public void save(ConfigSection document)throws IOException {
        ConfigSection validated=HomeStorage.validate(document);
        store.save("homes",HomeStorage.JSON.toJsonTree(HomeStorage.toMap(validated)).getAsJsonObject());
    }
    @Override public void close()throws IOException {store.close();}
    private static Object plain(JsonElement value) {
        if(value.isJsonNull())return null;
        if(value.isJsonObject()){Map<String,Object> map=new LinkedHashMap<>();value.getAsJsonObject().entrySet().forEach(e->map.put(e.getKey(),plain(e.getValue())));return map;}
        if(value.isJsonArray()){List<Object> list=new ArrayList<>();value.getAsJsonArray().forEach(e->list.add(plain(e)));return list;}
        JsonPrimitive p=value.getAsJsonPrimitive();if(p.isString())return p.getAsString();if(p.isBoolean())return p.getAsBoolean();
        String number=p.getAsString();return number.matches("-?[0-9]+")?new BigInteger(number):new BigDecimal(number);
    }
}
