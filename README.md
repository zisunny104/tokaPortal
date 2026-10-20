# tokaPortal

玩家互傳、公共傳送點、來回傳送、隨機傳送與住家管理。

## 主要功能

- **傳送選單**：輸入 `/portal` 開啟選單，可選傳送點、SHOPBOX、線上玩家；有裝 zero 時還有區域、領地與城市通。
- **玩家互傳**：提出傳送請求，對方在 60 秒內同意即可傳送。管理員可直接傳送。
- **公共傳送點**：管理員在現在的位置建立，全體玩家都能從選單或指令前往。
- **來回傳送**：每次傳送都會記錄，可用 `/back`、`/next` 在前後位置之間移動。
- **隨機傳送**：`/wild` 在指定世界內隨機找安全落點，有冷卻時間。
- **住家**：管理員可設定、編輯、刪除及傳送住家，需安裝 zero。

## 玩家指令

| 指令 | 說明 |
| --- | --- |
| `/portal` | 開啟傳送選單；`/portal help` 查看所有指令 |
| `/tpa <玩家代號>` | 例：`/tpa Steve`，向 Steve 提出傳送請求，對方同意後傳送 |
| `/tpp [傳送點名稱]` | 例：`/tpp spawn`，傳送到公共傳送點；不寫名稱開啟選單 |
| `/spawn` | 前往名為 `spawn` 的重生點 |
| `/back`、`/next` | 返回前一個／前往下一個傳送位置 |
| `/tpl [領地名稱]` | 傳送到自己的領地，需安裝 zero |
| `/wild` | 在允許的世界內隨機傳送，並顯示冷卻 |

所有指令都可加 `help` 查看說明，例如 `/tpa help`。

## 管理指令

| 指令 | 用法 | 說明 | 權限 |
| --- | --- | --- | --- |
| `/portal point set` | `/portal point set <名稱> [類型]` | 在目前位置建立公共傳送點；類型 `point`（預設）列於「傳送點列表」，`shopbox` 列於「SHOPBOX 傳送列表」 | `tokaPortal.points.manage` |
| `/portal point del` | `/portal point del <名稱>` | 刪除傳送點（建立者或 OP） | `tokaPortal.points.manage` |
| `/portal legacy list` | `/portal legacy list [頁]` | 列出尚未對應玩家 UUID 的舊名稱住家與傳送點，清單標示類型，主控台可用 | OP |
| `/portal legacy claim` | `/portal legacy claim <名稱> <玩家>` | 把該舊名稱的住家與傳送點全部交給指定玩家，有爭議的也可用此指令處理 | OP |
| `/tpw` | `/tpw <世界名稱>` | 傳送到世界的安全出生點，世界未載入會先載入 | OP |
| `/home`、`/tph` | `/home [list｜set｜del｜edit] [名稱]`、`/tph [名稱]` | 住家管理與傳送 | OP 或 zero 最高權限 |

`tokaPortal.points.manage` 預設只有 OP。傳送點名稱不可使用 `reserved-names` 內的字。
`/tpa` 的規則：OP 或 zero 最高權限者直接傳送；zero 高權限者稍待片刻後傳送；其餘玩家需對方同意。

## 住家與傳送點擁有者 UUID

住家以玩家 UUID 為擁有者，改名後仍找得到；`ownerName` 只是最後已知名稱，玩家登入時自動更新，`creator` 欄位照寫同樣的名稱，舊版 jar 仍可讀取。
舊資料只有名稱，啟動時會在背景依序以線上玩家、選用的 tokaProfiles／tokaEconomy、伺服器玩家檔對應 UUID，結果寫在後台。
查不到的住家維持「僅名稱」，同名玩家登入時自動取回；若同名已有不同玩家的住家則視為爭議，不自動處理，請用 `/portal legacy` 指令處理。
公共傳送點採同樣規則：沒有 `owner` 的舊點依同樣順序解析並寫入 `owner`，`creator` 照寫為最後已知名稱；刪除等擁有者判斷有 `owner` 時以 UUID 為準，尚未對應的舊點仍以名稱比對。

## 設定 config.yml

變更後需重新啟動。

| key | 預設 | 說明 |
| --- | --- | --- |
| `language` | `zh-TW` | 訊息語言 |
| `reserved-names` | help、list、set、del、edit | 不可當作住家或傳送點名稱的字 |
| `wild.worlds` | `[resource]` | 可使用 `/wild` 的世界；清單留空則所有世界都不能用 |
| `wild.cooldown-seconds` | `40` | `/wild` 冷卻秒數（1–86400） |
| `wild.radius` | `375` | 以目前位置為中心的隨機範圍（格） |
| `wild.attempts` | `16` | 找不到安全落點時的重試次數 |
| `wild.show-cooldown` | `true` | 冷卻中以血條顯示進度，需安裝 tokaDisplay |
| `wild.bar-color` | `cooldown` | 冷卻血條顏色 |
| `help-lines-per-page` | `8` | `/portal help` 每頁行數 |
| `window-items-per-page` | `10` | 選單清單每頁項目數（玩家可自行調整，範圍 3–30） |
| `window-search-threshold` | `8` | 項目超過此數才顯示搜尋／排序按鈕 |
| `window-icons` | `true` | 選單按鈕是否顯示圖示（使用遊戲內建圖示） |

## 串接

| 方法／事件 | 參數 | 用途 |
| --- | --- | --- |
| `Portal.portal(player, location[, title])` | 玩家、目的地、標題 | 傳送並記錄到 `/back` 歷史；回傳是否成功 |
| `Portal.toPoint(name, player)` | 傳送點名稱、玩家 | 傳送到公共傳送點 |
| `Portal.Tpa(from, to)` | 發起玩家、目標玩家 | 依權限提出傳送請求 |
| `PlayerPortalEvent` | `getPlayer()`、`getFromInfo()`、`getToInfo()`、`getFrom()`、`getTo()` | 每次傳送前觸發，可取消 |

## 疑難排解

- **`/wild` 說只能在特定世界使用？** 請在 `wild.worlds` 加入該世界的資料夾名稱。
- **住家指令無法使用？** 住家僅限管理員，且需安裝 zero。
- **沒有 zero 能用嗎？** 可以，傳送選單、`/tpa`、傳送點、`/back`、`/wild` 都不受影響。

### Plan 選用整合（2.7.2）

預設 `plan.enabled: false`。開啟後，`plan.homes: true` 提供 UUID 玩家住家數及全服住家總數；`plan.public-points: true` 提供全服公開傳送點數。各項可分別關閉，不輸出座標、世界、住家名稱、建立者名稱或 legacy 身分對照。Plan 網頁閱覽權限由 Plan 管理。

主執行緒依 `plan.refresh-seconds`（預設30，範圍5–3600秒）發布不可變數量快照；Plan 背景 Provider 僅讀該快照，不讀玩家、世界、儲存資料或執行傳送。UUID 玩家數不包含尚未完成身分轉換的 legacy 住家，全服總數仍包含。快照可能落後一個更新週期，Plan 本身另有收集週期。

官方 `plan-api:5.7-R0.2` 僅作 provided 編譯依賴，已核對 Plan5.8 build3638 API。缺少 Plan、能力不足或停用不影響傳送功能。支援 Plan 啟用/停用及 `Main.reload()`，欄位/訊息走 tokaI18n 的 `plan` 語系區塊。一般設定修改重新啟動套用；本插件未新增 `/portal reload` 指令。

Plan 以欄位文字作識別，修改語系標籤或停用欄位不會自動清除 Plan 已保存資料，請在 Plan 管理歷史資料。

