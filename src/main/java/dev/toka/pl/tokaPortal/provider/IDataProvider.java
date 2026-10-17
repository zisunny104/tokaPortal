package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.point.HomePoint;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface IDataProvider {

    /** creator：Player、UUID，或名稱字串（線上玩家或已知擁有者轉成 UUID，查不到存為 legacy）。 */
    int addHomePoint(String name, Object creator, Location loc);

    HomePoint getHomePoint(int id);

    HomePoint getHomePoint(String name);

    /** Player／UUID 以 UUID 比對；字串是名稱，先查 UUID，再比對最後已知名稱與 legacy 住家。 */
    HomePoint[] getHomePointsByCreator(Object creator);

    HomePoint[] getHomePointsByOwner(UUID owner);

    /** 尚無 UUID 的 legacy 住家，依名稱分組。 */
    List<LegacyGroup> legacyGroups();

    /** 套用名稱（小寫）對 UUID 的解析結果；同名已有不同 UUID 的住家視為爭議，不處理。 */
    Assigned assignOwners(Map<String, UUID> resolved);

    /** 管理員指定：把該名稱（不分大小寫）的 legacy 住家全部交給玩家。 @return 取回的住家數 */
    int claimLegacy(String name, UUID owner, String ownerName);

    /** 玩家登入：更新最後已知名稱，並取回同名的 legacy 住家（有爭議不處理）。 */
    Joined onPlayerJoin(UUID id, String name);

    boolean delHomePoint(Object val);

    IDataProvider save();

    IDataProvider close();

    IDataProvider close(boolean save);

    IDataProvider reload();

    IDataProvider reload(boolean save);

    String getName();

    record LegacyGroup(String name, int homes, boolean disputed) {}

    record Assigned(int assigned, int disputed, List<String> disputedNames) {}

    record Joined(int renamed, int claimed, boolean disputed) {}
}
