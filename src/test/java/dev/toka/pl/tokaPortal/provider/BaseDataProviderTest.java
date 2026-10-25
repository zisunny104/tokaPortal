package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.provider.storage.HomeStorage;
import dev.toka.pl.tokaPortal.provider.storage.WriteQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseDataProviderTest {
    static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID DAVE = UUID.fromString("00000000-0000-0000-0000-00000000000d");
    static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    /** 記憶體儲存，保留最後一次存下的文件。 */
    static final class MemoryStorage implements HomeStorage {
        ConfigSection document;
        @Override public ConfigSection load() { return document; }
        @Override public void save(ConfigSection value) throws IOException { document = new ConfigSection(HomeStorage.toMap(value)); }
    }

    MemoryStorage storage;
    WriteQueue writer;
    BaseDataProvider provider;
    Map<String, UUID> online;

    static Map<String, Object> home(String name, String creator, String owner) {
        Map<String, Object> home = new LinkedHashMap<>();
        home.put("name", name);
        home.put("creator", creator);
        home.put("type", "home");
        if (owner != null) home.put("owner", owner);
        home.put("extra", "kept");
        home.put("loc", new LinkedHashMap<>(Map.of("level", "world", "x", 1, "y", 64, "z", 2, "yaw", 0, "pitch", 0)));
        return home;
    }

    @BeforeEach void setUp() {
        storage = new MemoryStorage();
        storage.document = HomeStorage.empty();
        storage.document.set("homes", new ArrayList<>(List.of(
                home("a1", "Alice", ALICE.toString()),   // 已有 UUID
                home("b1", "Bob", null),                  // legacy
                home("b2", "bob", "bad-uuid"),            // owner 不合法：視為 legacy
                home("d1", "Dave", DAVE.toString()),      // Dave 已有 UUID
                home("d2", "dave", null))));              // 同名 legacy
        dev.toka.pl.tokaPortal.utils.Utils.setLevelLookup(name -> null);
        writer = new WriteQueue((key, error) -> fail(key, error));
        online = new HashMap<>();
        provider = new BaseDataProvider(storage, writer, "test");
        provider.setOnlineLookup(name -> online.get(name));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> saved(String name) {
        assertTrue(writer.drain(5000));
        for (Object item : storage.document.getList("homes")) {
            if (((Map<String, Object>) item).get("name").equals(name)) return (Map<String, Object>) item;
        }
        return null;
    }

    @Test void renamedPlayerKeepsHomes() {
        var joined = provider.onPlayerJoin(ALICE, "Alicia");
        assertEquals(1, joined.renamed());
        assertEquals(1, provider.getHomePointsByOwner(ALICE).length);
        assertEquals("Alicia", provider.getHomePoint("a1").getOwnerName());
        // 寫出的 creator 是最後已知名稱，owner 保留，未知欄位保留
        var row = saved("a1");
        assertEquals("Alicia", row.get("creator"));
        assertEquals("Alicia", row.get("ownerName"));
        assertEquals(ALICE.toString(), row.get("owner"));
        assertEquals("kept", row.get("extra"));
    }

    @Test void playerAndUuidLookupsUseUuid() {
        assertEquals(1, provider.getHomePointsByCreator(ALICE).length);
        assertEquals(0, provider.getHomePointsByCreator(BOB).length);
        assertEquals(0, provider.getHomePointsByCreator(42).length);
        assertTrue(provider.getHomePoint("a1").isCreator(ALICE));
        assertFalse(provider.getHomePoint("a1").isCreator(BOB));
    }

    @Test void nameApiStaysCompatible() {
        // 離線：比對最後已知名稱（不分大小寫）
        assertEquals(1, provider.getHomePointsByCreator("alice").length);
        // 線上玩家以 UUID 為準：同名的新玩家拿不到舊擁有者的住家
        online.put("Alice", OTHER);
        assertEquals(0, provider.getHomePointsByCreator("Alice").length);
        online.put("Alice", ALICE);
        assertEquals(1, provider.getHomePointsByCreator("Alice").length);
        // 改名後名稱版用新名稱找得到，舊名稱找不到
        provider.onPlayerJoin(ALICE, "Alicia");
        online.clear();
        assertEquals(1, provider.getHomePointsByCreator("Alicia").length);
        assertEquals(0, provider.getHomePointsByCreator("Alice").length);
        // legacy 住家用名稱找得到（舊 API 行為）
        assertEquals(2, provider.getHomePointsByCreator("BOB").length);
        assertEquals("Bob", provider.getHomePoint("b1").getCreator());
    }

    @Test void legacyHomeBelongsToNobodyUntilClaimed() {
        assertNull(provider.getHomePoint("b2").getOwner());
        assertFalse(provider.getHomePoint("b1").isCreator(BOB));
        assertEquals(0, provider.getHomePointsByOwner(BOB).length);
    }

    @Test void joiningPlayerReclaimsLegacyHomesByName() {
        var joined = provider.onPlayerJoin(BOB, "BOB");
        assertEquals(2, joined.claimed());
        assertFalse(joined.disputed());
        assertEquals(2, provider.getHomePointsByOwner(BOB).length);
        assertEquals("BOB", saved("b1").get("creator"));
        assertEquals(BOB.toString(), saved("b2").get("owner"));
    }

    @Test void joiningPlayerDoesNotClaimDisputedName() {
        var joined = provider.onPlayerJoin(OTHER, "Dave");
        assertEquals(0, joined.claimed());
        assertTrue(joined.disputed());
        assertTrue(provider.getHomePoint("d2").isLegacy());
        assertEquals(DAVE, provider.getHomePoint("d1").getOwner());
        // 管理員可強制指定
        assertEquals(1, provider.claimLegacy("DAVE", OTHER, "Dave"));
        assertEquals(OTHER, provider.getHomePoint("d2").getOwner());
        assertEquals(0, provider.claimLegacy("dave", OTHER, "Dave"));
    }

    @Test void oldDataWithoutOwnerLoadsAndNewDataStaysReadableByOldVersions() throws IOException {
        var point = provider.getHomePoint("b1");
        assertTrue(point.isLegacy());
        assertEquals("Bob", point.getCreator());
        provider.onPlayerJoin(BOB, "Bob");
        assertTrue(writer.drain(5000));
        // 新資料仍有舊版必要的 creator 字串，可通過驗證
        HomeStorage.validate(storage.document);
        assertEquals("Bob", saved("b1").get("creator"));
        // 重新載入得到相同擁有者
        provider.reload();
        assertEquals(BOB, provider.getHomePoint("b1").getOwner());
    }

    @Test void rejectsNonStringOwner() {
        var bad = home("x", "X", null);
        bad.put("owner", 5);
        ConfigSection document = HomeStorage.empty();
        document.set("homes", new ArrayList<>(List.of(bad)));
        assertThrows(IOException.class, () -> HomeStorage.validate(document));
    }

    @Test void parseUuidIsLenient() {
        assertNull(HomePoint.parseUuid(null));
        assertNull(HomePoint.parseUuid(" "));
        assertNull(HomePoint.parseUuid("nope"));
        assertEquals(ALICE, HomePoint.parseUuid(ALICE.toString()));
    }
}
