# tokaPortal

玩家互傳、公共傳送點、來回傳送、隨機傳送與住家管理。

## 主要功能

- **傳送選單**：輸入 `/portal` 開啟選單，可選傳送點、SHOPBOX、線上玩家；領地由 tokaLand 軟依賴 API 提供。tokaLand/API 未就緒時領地傳送停用。
- **玩家互傳**：提出傳送請求，對方在 60 秒內同意即可傳送。管理員可直接傳送。
- **公共傳送點**：管理員在現在的位置建立，全體玩家都能從選單或指令前往。
- **來回傳送**：每次傳送都會記錄，可用 `/back`、`/next` 在前後位置之間移動。
- **隨機傳送**：`/wild` 在指定世界內隨機找安全落點，有冷卻時間。
- **住家**：管理員可設定、編輯、刪除及傳送住家，需安裝 tokaRegions。

## 玩家指令

| 指令 | 說明 |
| --- | --- |
| `/portal` | 開啟傳送選單；`/portal help` 查看所有指令 |
| `/tpa <玩家代號>` | 例：`/tpa Steve`，向 Steve 提出傳送請求；不寫名稱開啟玩家選單 |
| `/tpahere <玩家代號>` | 請對方傳送到你身邊，流程與 `/tpa` 相同，方向相反 |
| `/tpaccept`、`/tpdeny` | 同意／拒絕別人對你的傳送請求 |
| `/tpcancel` | 取消你自己發出的傳送請求 |
| `/dnd [on｜off]` | 勿擾：開啟時別人的傳送請求直接被拒絕，不寫參數切換；只存在記憶體，離線即解除 |
| `/tpauto [on｜off｜default]` | 自動同意別人的 `/tpa`、`/tpahere`：`on` 自動同意、`off` 每次確認、`default` 跟隨伺服器預設（`config.yml` 的 `tpa.auto-accept.default`，預設 false）；不寫參數看目前狀態；勿擾開啟時仍會拒絕；設定存在 `tpa-auto.txt` |
| `/tpr <區域名稱或代碼>` | 傳送到該區域的傳送點，等同 tpr 告示牌；區域要先用 `/region settp` 設定傳送點 |
| `/tpp [傳送點名稱]` | 例：`/tpp spawn`，傳送到公共傳送點；不寫名稱開啟選單 |
| `/spawn` | 前往名為 `spawn` 的重生點 |
| `/back`、`/next` | 返回前一個／前往下一個傳送位置 |
| `/tpl [領地名稱或 UUID]` | 傳送到可使用的領地；`/land list` 查 UUID，名稱重複時請用 UUID |
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
| `/home`、`/tph` | `/home [list｜set｜del｜edit] [名稱]`、`/tph [名稱]` | 住家管理與傳送 | OP 或管理員身分 |

`tokaPortal.points.manage` 預設只有 OP。傳送點名稱不可使用 `reserved-names` 內的字。
## 玩家傳送（/tpa）

請求不會彈出視窗打斷對方：對方螢幕頂端出現倒數條（與重啟、清理倒數相同的做法，有 tokaDisplay 用其訊息列，否則用動作列），寫明誰想傳送，以及 `/tpaccept` 同意、`/tpdeny` 拒絕。
倒數結束請求作廢，並通知發送者；雙方任一方離線、插件停用或重新載入，請求與倒數條都會結束。每位玩家同時只有一個待處理的請求，也只能發出一個；不能對自己發。
設定 `tpa.request.window: true` 可另外彈出同意視窗（舊做法）。

勿擾中的玩家收不到請求，發送者只會看到「對方勿擾中」。一般玩家每次最多開 `dnd.max-minutes` 分鐘，到時自動關閉；有 `tokaportal.dnd.unlimited` 不限時間。勿擾不影響自己發請求。

| 層級 | 條件（任一） | 行為 |
| --- | --- | --- |
| 一般 | 其餘玩家 | 請求，對方同意才傳送，受勿擾影響 |
| 強制 | 權限 `tokaportal.tpa.force`，或贊助者身分 `toka.identity.sponsor` | 不徵求同意；對方頂端倒數 `tpa.force.delay-seconds` 秒，期間可用 `/tpdeny` 阻止，否則傳送；不受勿擾阻擋 |
| 靜默 | 權限 `tokaportal.tpa.silent`，或 OP／管理員身分 `toka.identity.admin` | 立刻傳送，不通知對方、沒有倒數、不受勿擾影響；抵達後隱身 `tpa.silent.invisible-seconds` 秒 |

強制與靜默只用在 `/tpa`；`/tpahere` 所有人都需對方同意。`tokaportal.tpa.silent` 預設 OP，`tokaportal.tpa.force`、`tokaportal.dnd.unlimited` 預設無人；身分群組要用節點授權時，把節點加進該群組即可。
所有傳送都經過 `Portal.portal(...)`，`PlayerPortalEvent` 照常觸發，其他插件的限制仍有效。

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
| `tpa.request.seconds` | `20` | 請求等待秒數，限 15–30 |
| `tpa.request.window` | `false` | 是否另外彈出同意視窗 |
| `tpa.force.delay-seconds` | `10` | 強制傳送的倒數秒數，限 3–60 |
| `tpa.silent.invisible-seconds` | `30` | 靜默傳送抵達後隱身秒數，限 1–600 |
| `dnd.max-minutes` | `30` | 一般玩家每次勿擾最長分鐘數 |
| `help-lines-per-page` | `8` | `/portal help` 每頁行數 |
| `window-items-per-page` | `10` | 選單清單每頁項目數（玩家可自行調整，範圍 3–30） |
| `window-search-threshold` | `8` | 項目超過此數才顯示搜尋／排序按鈕 |
| `window-icons` | `true` | 選單按鈕是否顯示圖示（使用遊戲內建圖示） |

## 串接

| 方法／事件 | 參數 | 用途 |
| --- | --- | --- |
| `Portal.portal(player, location[, title])` | 玩家、目的地、標題 | 傳送並記錄到 `/back` 歷史；回傳是否成功 |
| `Portal.toLandClaim(player, claimId)` | 玩家、tokaLand 穩定 ID | 重新驗證本人 UUID 權限，再透過 Portal 事件傳送；供 tokaSigns 等選用整合呼叫 |
| `Portal.toPoint(name, player)` | 傳送點名稱、玩家 | 傳送到公共傳送點 |
| `Portal.Tpa(from, to)` | 發起玩家、目標玩家 | 依發起者層級提出請求、強制或靜默傳送 |
| `PlayerPortalEvent` | `getPlayer()`、`getFromInfo()`、`getToInfo()`、`getFrom()`、`getTo()` | 每次傳送前觸發，可取消 |

## 疑難排解

- **`/wild` 說只能在特定世界使用？** 請在 `wild.worlds` 加入該世界的資料夾名稱。
- **住家指令無法使用？** 住家僅限管理員，且需安裝 tokaRegions。
- **領地清單只顯示部分資料？** tokaLand 只列出已確認為本人 UUID 擁有的領地；未完成身分對照的資料等待玩家登入後由 tokaProfiles 取回。
- **沒有 tokaRegions 能用嗎？** 可以，傳送選單、`/tpa`、傳送點、`/back`、`/wild` 都不受影響。

### Plan 選用整合

預設自動接入 Plan，提供 UUID 玩家住家數、全服住家總數及公開傳送點數。可用 `plan.fields` 選擇欄位或全部關閉，不輸出座標、世界、住家名稱、建立者名稱或 legacy 身分對照。Plan 網頁閱覽權限由 Plan 管理。

主執行緒依 `plan.refresh-seconds`（預設30，範圍5–3600秒）發布不可變數量快照；Plan 背景 Provider 僅讀該快照，不讀玩家、世界、儲存資料或執行傳送。UUID 玩家數不包含尚未完成身分轉換的 legacy 住家，全服總數仍包含。快照可能落後一個更新週期，Plan 本身另有收集週期。

官方 `plan-api:5.7-R0.2` 僅作 provided 編譯依賴。缺少 Plan、能力不足或停用不影響傳送功能。支援 Plan 啟用/停用及 `Main.reload()`，欄位/訊息走 tokaI18n 的 `plan` 語系區塊。Plan 模式與欄位可用下列管理指令即時套用；其餘設定仍依原有重新載入流程。

Plan 以欄位文字作識別，修改語系標籤或停用欄位不會自動清除 Plan 已保存資料，請在 Plan 管理歷史資料。



### Plan 模式與管理指令

預設 `plan.mode: auto`、`plan.fields: all`。`auto` 偵測到可用 Plan 就啟用；`enable` 未就緒時提示；`disable` 一定不發布。all 僅包含本插件核准的摘要欄位，沒有憑證、Discord ID 或私人座標。

管理員可用 `/portal plan status`、`/portal plan mode <auto|enable|disable>`、`/portal plan fields <all|none|欄位...>`；變更保存設定且即時套用，保留其他設定及註解。舊 `plan.enabled`／逐項布林仍相容，明確設定的新 mode／fields 優先。

API（主執行緒）：`planStatusJson()`、`configurePlan(String mode, String[] fields)`；null 表示該項沿用。
