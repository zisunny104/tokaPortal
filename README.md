# tokaPortal

整體拆分現況只維護於 [拆分主文件](https://github.com/zisunny104/zero/blob/master/docs/ARCHITECTURE.md)；本 README 維護本專案的設定、API 與使用限制。

**tokaPortal 2.3.0** 是 **Nukkit 伺服器的傳送插件**，安裝於伺服器端，提供玩家傳送功能，使用 **Java 21**。目前已驗證 PM1E 4511；其他 Nukkit 分支的相容性仍待驗證。

玩家可以向其他人提出傳送請求，透過 `/back` 和 `/next` 回到先前的位置；管理員也能傳送到指定世界。輸入 `/portal` 即可開啟傳送選單。

公共傳送點、商店目的地與隨機傳送由 tokaPortal 自行管理；搭配相容的 zero 插件，還能使用住家、領地與區域功能。沒有安裝 zero 時，基本傳送仍可使用，選單只會顯示可用項目。

## 安裝

目前已驗證的環境為 Java 21 與 [Nukkit PetteriM1 Edition 發行版 4511](https://github.com/PetteriM1/NukkitPetteriM1Edition/releases/tag/4511)。其他 Nukkit 分支尚未驗證。

1. 停止伺服器，確認啟動伺服器的 Java 版本為 21。
2. 將 `tokaPortal-2.3.0.jar` 放入伺服器的 `plugins/` 目錄。
3. 需要住家或領地等功能時，一併安裝相容的 zero 插件。
4. 啟動伺服器，確認 tokaPortal 已啟用，再於遊戲內輸入 `/portal`。

更新既有安裝時，先備份 `plugins/tokaPortal/`，移除舊版 tokaPortal JAR，再放入新版本。請保留資料目錄與 `data.yml`；從 1.x 升級也使用相同步驟，並先確認 Java 與核心版本符合需求。

## 使用方式

所有指令都在遊戲內執行。下表中的 `<參數>` 為必填，`[參數]` 為選填。

### 基本傳送

這些功能不需要 zero。

| 指令 | 用途 |
| --- | --- |
| `/portal` | 開啟傳送選單 |
| `/tpa <玩家名稱>` | 向線上玩家提出傳送請求 |
| `/tpw <世界名稱>` | 傳送至世界的安全出生點，僅限 OP；必要時嘗試載入世界 |
| `/back` | 返回前一個傳送位置 |
| `/next` | 前往下一個傳送位置 |
| `/portal help` | 顯示傳送紀錄指令提示 |

指令輸入介面會提示 `/tpw` 的世界名稱與 `/tpa` 的線上玩家名稱；世界清單包含已載入及 `worlds/` 內可辨識的世界，也可手動輸入名稱。玩家進出或世界載入、卸載時會更新提示。

例如，輸入 `/tpa Steve` 後，對方會收到同意或拒絕的選單。一般玩家必須取得對方同意；未安裝 zero 時，OP 可直接傳送。若有相容的 zero 玩家權限功能，則依其權限規則處理。

`/back` 與 `/next` 需要已有傳送紀錄。沒有可前往的位置，或紀錄中的世界尚未載入時，插件會顯示原因。

### zero 整合功能

zero 必須提供本專案使用的 `prj.toka.zero` 功能介面；同名插件不一定相容。

| 指令 | 用途 | 所需 zero 功能 |
| --- | --- | --- |
| `/spawn` | 前往名稱為 `spawn` 的傳送點；須先建立該傳送點 | 無 |
| `/tpp [名稱]` | 前往指定傳送點；省略名稱時開啟傳送選單 | 無 |
| `/portal point set <名稱> [類型]` | 在目前位置建立傳送點，預設類型為 `point` | 無；需要 tokaPortal.points.manage |
| `/portal point del <名稱>` | 管理員刪除傳送點 | 無；需要 tokaPortal.points.manage |
| `/tpl [領地名稱]` | 前往指定領地；省略名稱時開啟領地選單 | 領地 |
| `/home [操作] [名稱]` | 查看或管理住家，操作可用 `list`、`set`、`del`、`edit` | 玩家與區域 |
| `/tph [住家名稱]` | 前往自己的住家；省略名稱時開啟住家列表 | 玩家與區域 |
| `/wild` | 在 `wild.worlds` 設定的世界（預設 `resource`）隨機傳送，須符合冷卻條件；冷卻、範圍與血條顏色都在設定檔 | tokaDisplay（選用，顯示冷卻） |

傳送選單使用 Portal 的公共點與商店目的地，區域與城市通入口仍來自 zero。插件會分別檢查各項功能，缺少或不相容的項目會隱藏；直接執行相關指令時，會說明停用原因。zero 停用或功能呼叫失敗時，其他可用功能仍會繼續運作，既有住家資料也會保留。

若 zero 在 `plugin.yml` 宣告的名稱不是 `zero`，請將其實際名稱加入 tokaPortal 的 `softdepend`，確保載入順序正確。

`/home` 會提示 `list`、`set`、`del`、`edit`；`/tph` 與 `/home del` 只提示自己的住家。新增或刪除住家後會更新清單，`/home set` 的新名稱可自由輸入。提示的選取方式依 Bedrock 客戶端而定。

### 使用限制

住家指令僅限 OP 或 zero 最高權限玩家使用，只能管理及傳送至自己的住家，並須在允許的區域新增住家。住家更名與跨伺服器目的地管理尚未提供。

目前沒有可供權限插件配置的獨立權限節點。住家允許區域等限制仍寫在原始碼中；`/wild` 可使用的世界、冷卻與範圍已改為設定檔（`wild.*`）。

## 資料與統計

設定檔位於 `plugins/tokaPortal/config.yml`。住家資料由共用的 tokaStorage 儲存，**預設 SQLite**，也可選 JSON、YAML，資料放在 `plugins/tokaPortal/storage/<格式>/`：

```yaml
storage:
  backend: sqlite # sqlite、json 或 yaml
```

啟動時日誌會寫「載入住家共 N 個」。第一次使用的格式會記錄在 `storage/active-backend.txt`，之後只改設定不會換格式，避免用空資料開始；要換格式請停服並用 tokaStorage 的搬遷工具。

**2.2.0 起不再支援舊格式**：舊的 `storage.type`（`data.yml`、`homes.db`）與離線匯入工具都已移除，設定檔若還是 `storage.type` 會拒絕啟動並提示改成 `storage.backend`；插件也不再檢查舊檔，舊檔放著不會影響啟動。需要從舊格式匯入時，請用標籤 `v2.1.5` 的版本。

資料缺漏、格式錯誤或轉換失敗時，插件會停止啟動並留下錯誤訊息，避免覆寫原始資料。更新或搬移前請備份完整資料目錄；傳送點、領地與區域資料由 zero 管理，須另外備份。

### 資料收集

本插件不收集、也不傳送任何使用統計資料。

## 開發與建置

需要 JDK 21、Maven 3.9 與 PowerShell。在專案根目錄執行：

```powershell
.\build.ps1
```

首次建置需連線下載依賴。若 JDK 或 Maven 安裝位置與腳本預設值不同，可透過 `-JdkHome` 與 `-Maven` 指定路徑。

可部署的 JAR：`output/tokaPortal-2.3.0.jar`。建置腳本會執行測試；`output/` 與建置快取不納入 Git。

## 多語系

所有玩家訊息、表單、指令說明與後台文字都放在 `lang/zh-TW.yml`（共用套件 tokaI18n，內嵌於 JAR）。`config.yml` 的 `language` 選擇語言，目前只提供繁體中文 `zh-TW`。伺服器擁有者可把同名檔放進 `plugins/tokaPortal/lang/` 覆蓋個別文字。每個指令都可用 `/<指令> help` 查看說明。key 分區與待確認事項見 [多語系說明](docs/I18N.md)。

## 驗證範圍與問題回報

已完成 Java 21.0.11 與 PM1E 4511 的建置及隔離伺服器測試，涵蓋無 zero 時啟動、部分功能可用、功能呼叫失敗、住家資料儲存與重載，以及傳送事件取消。

部分情境使用測試替身驗證，尚未搭配實際 zero 插件與遊戲客戶端完成端對端測試。

回報問題時，請提供版本資訊、重現步驟與相關日誌；若有使用 zero，也請附上它的版本。分享資料檔前，請移除不希望公開的資訊。

本次安全修正與檢核範圍見 [安全檢核](SECURITY_REVIEW.md)。

## 授權

本專案採用 [MIT License](LICENSE)，授權全文位於專案根目錄的 `LICENSE`。散布時須保留著作權與授權聲明。

第三方元件的授權、來源查核範圍與 GPL 核心散布注意事項，請參閱 [第三方授權聲明](THIRD_PARTY_NOTICES.md)。JAR 亦附有相關授權文件。


## 2.2.2 儲存交接

新安裝使用 tokaStorage 0.1.3，預設 SQLite，另支援 JSON／YAML（`storage.backend: sqlite｜json｜yaml`）；library 私有打包，不需另裝插件。既有 storage.type 保留舊格式，改 backend 前須停服明確匯入。請先閱讀 [儲存與離線搬遷](docs/storage-2.2.2.md)。

16 個測試與三後端的 PM1E 4511 載入、儲存、重載、刪除測試通過。與 zero 1.7.8、tokaWorlds 1.0.2 的住家更名、引用連動及模擬失敗還原也通過；沒有真玩家或正式 MySQL 驗證。住家名稱／擁有者尚未完成 Profiles UUID 搬遷，不宣稱已全部解耦。



## 2.2.2 公共傳送點接管

公共點與 `/spawn`、`/tpp`、`/portal point` 不再依賴 zero。`/wild` 的計時一直由 Portal 擁有；本版移除已過時的 zero WILD、POINTS、POINT_MANAGEMENT 相容性檢查。領地、區域、城市通及住家授權仍保留選用整合。

公共點使用 tokaStorage **0.1.3**，與住家共用 `storage.backend`（預設 `sqlite`，另支援 `json`、`yaml`），位置為 `storage/<backend>/public-points/`。SQLite driver 獨立封裝，避免與核心或其他插件的 JDBC 版本混載。啟用 backend 後不能只改設定切換，避免意外讀到空資料。

`/portal point set <名稱> [類型]` 與 `del` 需要 `tokaPortal.points.manage`，預設 OP，可交由權限插件配置。世界未載入時拒絕傳送，不自動產生新世界。沒有 zero 也能建立與使用公共點。

交接工具只放在 **zero**；Portal 不讀取 zero 資料庫，也沒有 legacy 匯入指令。請先備份，再由 zero 的交接工具核對、寫入、讀回確認並關閉舊功能，確保只有一個管理者。新資料不會覆蓋不同的既有目的地。

### 公開資料 API

從 `Main.getInstance().getPublicPoints()` 取得 `PublicPoints`：

```java
int replaceIfEmptyOrEqual(String json) throws IOException;
String snapshotJson();
boolean verify(String json);
String findJson(String name); // 不存在回傳 null
boolean createJson(String rowJson) throws IOException;
boolean replaceJson(String rowJson) throws IOException; // 只更新既有名稱
boolean create(JsonObject row) throws IOException;
boolean delete(String name) throws IOException;
Map<Object, teleportPoint> points();
```

這些是一般批次資料／CRUD API；不自行連線舊系統。`replaceIfEmptyOrEqual` 只接受空目的地或內容完全一致的資料，寫入後讀回比對。比對忽略點排列順序，名稱唯一、數值有限；API 同步持久化，請不要在高頻事件中呼叫大量寫入。世界名稱可先保存，實際傳送要求世界已載入。

```json
{"schema":1,"points":[{"name":"spawn","creator":"admin","type":"point","info":"出生點","world":"chuklok","x":128.0,"y":151.0,"z":128.0,"yaw":0.0,"pitch":0.0}]}
```

點以 `name` 識別。`creator` 是來源名稱標記，並不代表授予權限；讀寫權限由呼叫端負責。type 保留 `point`、`shopbox` 等原值，不改寫成權限組。API 驗證失敗會拋出例外，原資料不會因衝突被合併或清空。
