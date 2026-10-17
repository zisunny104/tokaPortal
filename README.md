# tokaPortal

**tokaPortal 2.5.1** 是 Nukkit 伺服器的傳送插件：玩家可提出傳送請求、用 `/back`、`/next` 回到先前位置，也有公共傳送點與隨機傳送；管理員可傳送到指定世界。輸入 `/portal` 開啟傳送選單。住家、領地與區域功能需另裝相容的 zero，沒有 zero 時基本傳送仍可使用。

## 安裝

- 需要 Java 21；已驗證 Nukkit PM1E 4511，其他分支尚未驗證。
- 將 `tokaPortal-2.5.1.jar` 放入 `plugins/`，重新啟動伺服器。
- 選用相依（softdepend）：zero（住家、領地、區域）、tokaDisplay（`/wild` 冷卻血條）。
- 使用共用套件 tokaStorage（資料儲存）與 tokaI18n（語系），皆已內嵌於 JAR。

## 指令

| 指令 | 用途 |
| --- | --- |
| `/portal` | 開啟傳送選單；`/portal help` 查看說明 |
| `/tpa <玩家>` | 向線上玩家提出傳送請求 |
| `/tpw <世界>` | 傳送至世界的安全出生點（僅 OP） |
| `/back`、`/next` | 前往上一個／下一個傳送位置 |
| `/spawn` | 前往名為 `spawn` 的傳送點 |
| `/tpp [名稱]` | 前往傳送點；省略名稱開啟選單 |
| `/portal point set <名稱> [類型]`、`/portal point del <名稱>` | 建立／刪除公共傳送點 |
| `/wild` | 在 `wild.worlds` 世界隨機傳送，有冷卻 |
| `/tpl [領地]`、`/home [list｜set｜del｜edit] [名稱]`、`/tph [名稱]` | 領地與住家（需 zero） |

## 權限

| 權限 | 說明 | 預設 |
| --- | --- | --- |
| `tokaPortal.points.manage` | 建立、刪除公共傳送點 | OP |

住家指令僅限 OP 或 zero 最高權限玩家使用。

## 設定（`config.yml`）

| key | 預設 | 說明 |
| --- | --- | --- |
| `language` | `zh-TW` | 語言 |
| `reserved-names` | help、list、set、del、edit | 不可作為住家／傳送點名稱的字 |
| `storage.backend` | `sqlite` | `sqlite`、`json`、`yaml`；啟用後不可只改設定切換 |
| `wild.worlds` | `[resource]` | 可使用 `/wild` 的世界 |
| `wild.cooldown-seconds` | `40` | 冷卻秒數（1–86400） |
| `wild.radius` | `375` | 隨機範圍（格） |
| `wild.attempts` | `16` | 找不到安全落點的重試次數 |
| `wild.show-cooldown` | `true` | 冷卻血條（需 tokaDisplay） |
| `wild.bar-color` | `cooldown` | 冷卻血條顏色 |
| `help-lines-per-page` | `8` | help 每頁行數 |
| `window-items-per-page` | `10` | 選單清單每頁項目數（範圍 `window-items-min` 3 至 `window-items-max` 30） |
| `window-search-threshold` | `8` | 超過幾項才顯示搜尋按鈕 |

資料放在 `plugins/tokaPortal/storage/<格式>/`。本插件不收集任何使用統計。

## 版本紀錄

- 2.5.1：建置腳本改用 2.5.1 JAR 名稱；文件整理。
- 2.5.0：清單支援分頁、搜尋、排序與每頁數量；空清單會提示。
- 2.4.1：視窗以按鈕編號取代標題；清單加入返回按鈕。
- 2.4.0：`/portal help` 分頁列出傳送指令與功能狀態。
- 2.3.1：`/portal help` 可由後台執行。

授權：MIT。
