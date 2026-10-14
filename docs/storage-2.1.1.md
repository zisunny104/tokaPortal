# tokaPortal 2.1.1 共用儲存

新安裝預設 `storage.backend: sqlite`，使用 tokaStorage 0.1.1。可選 `sqlite`、`json`、`yaml`，住家與未知附加欄位維持既有 document，不搬玩家 Profile 資料。

既有設定只有 `storage.type: yaml/sqlite` 時維持原 `data.yml`／`homes.db`；這是 deprecated 相容路徑，不強制切換。Main 的設定入口會拒絕只改格式而自動轉換。舊 `StorageManager.open` 方法保留供既有 API／測試使用，其舊自動轉換行為不是新設定入口。

新儲存位於 `plugins/tokaPortal/storage/<backend>/`；`active-backend.txt` 限制靜默改設定，匯入後不得回讀過期 legacy 資料。已存在 legacy 檔時，未明確匯入就不建立空的共用住家資料。保存失敗不更新記憶體候選住家。

## 明確離線匯入

完整停止伺服器後，在 Linux 執行（依實際檔名調整）：

```bash
java -cp 'plugins/tokaPortal-2.1.1.jar:Nukkit-PM1E-4511.jar' \
  dev.toka.pl.tokaPortal.provider.storage.LegacyHomeImport \
  plugins/tokaPortal yaml sqlite backups/tokaPortal
```

來源 `yaml` 指 `data.yml`，`sqlite` 指 `homes.db`。目標可 `sqlite/json/yaml`。Windows classpath 分隔符使用 `;`。

來源讀取自副本，原檔保留；先建立原檔備份與 SHA-256 比對，再寫入獨立 staging、讀回、關閉重開驗證，最後移到不存在的目標。拒絕覆蓋既有目標、已啟用 backend、未關閉 SQLite journal。失敗 staging 與 backup 保留，不自動刪除。這不是線上搬遷、跨插件 transaction 或整張世界備份。

成功後保留 backup，將舊 config 的 storage 改為：

```yaml
storage:
  backend: sqlite
```

再完整啟動。不要保留同時生效的 `storage.type` 與另一套住家寫入程式。其他共用 backend 互轉仍需離線工具與明確核對，這個 CLI 僅處理 legacy→tokaStorage；沒有自動跨 backend 回退。

## 建置與驗證

依賴 `dev.toka:tokaStorage:0.1.1`，先在 tokaStorage 專案 build/install 到相同 Maven repository。`build.ps1 -Repository <cache>` 可選依賴快取；SQLite 原生 resources 與 service descriptor 保留，SnakeYAML relocate 避免核心版本衝突。output 不進版控。

測試包括三種共用 backend 讀寫、來源 YAML/SQLite 到三種目標共六組、原檔／備份位元組保留、附加欄位與 >2^53 整數、invalid save、既有設定保持、新安裝 SQLite、靜默改 backend 拒絕、失敗不覆蓋。未在 VPS、Linux、磁碟滿或玩家連線中驗證；新核心載入也需正式前抽查。

JSON／YAML 受 tokaStorage 16 MiB文件上限，寫入仍同步；不能每個玩家移動事件呼叫。此版沒有替換 zero 的 DbLib／MySQL，也沒有新增 Profiles 硬依賴。
