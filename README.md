# tokaPortal

傳送插件：玩家可向其他玩家提出傳送請求、使用公共傳送點、回到先前的位置，或在資源世界隨機傳送。

## 主要功能

- **傳送選單**：輸入 `/portal` 開啟選單，可選傳送點、SHOPBOX、線上玩家；有裝 zero 時還有區域、領地與城市通。
- **玩家互傳**：向對方提出請求，對方同意後才會傳送（請求 60 秒內有效）。管理員可直接傳送。
- **公共傳送點**：管理員在現在的位置建立，全體玩家都能從選單或指令前往。
- **來回傳送**：每次傳送都會記錄，可用 `/back`、`/next` 在前後位置之間移動。
- **隨機傳送**：`/wild` 在指定世界內隨機找安全落點，有冷卻時間。
- **住家**（僅管理員，需 zero）：設定、編輯、刪除並傳送到自己的住家。

## 玩家指令

| 指令 | 說明 |
| --- | --- |
| `/portal` | 開啟傳送選單；`/portal help` 查看所有指令 |
| `/tpa <玩家代號>` | 例：`/tpa Steve`，向 Steve 提出傳送請求，對方同意後傳送 |
| `/tpp [傳送點名稱]` | 例：`/tpp spawn`，傳送到公共傳送點；不寫名稱開啟選單 |
| `/spawn` | 回到重生點（名為 `spawn` 的傳送點） |
| `/back`、`/next` | 返回前一個／前往下一個傳送位置 |
| `/tpl [領地名稱]` | 傳送到自己的領地（需 zero） |
| `/wild` | 在允許的世界內隨機傳送，並顯示冷卻 |

（圖：`/portal` 傳送選單視窗，顯示各個傳送目的按鈕）

所有指令都可加 `help` 查看說明，例如 `/tpa help`。

## 管理指令

| 指令 | 用法 | 說明 | 權限 |
| --- | --- | --- | --- |
| `/portal point set` | `/portal point set <名稱> [類型]` | 在目前位置建立公共傳送點；類型 `point`（預設）列於「傳送點列表」，`shopbox` 列於「SHOPBOX 傳送列表」 | `tokaPortal.points.manage` |
| `/portal point del` | `/portal point del <名稱>` | 刪除傳送點（建立者或 OP） | `tokaPortal.points.manage` |
| `/tpw` | `/tpw <世界名稱>` | 傳送到世界的安全出生點，世界未載入會先載入 | OP |
| `/home`、`/tph` | `/home [list｜set｜del｜edit] [名稱]`、`/tph [名稱]` | 住家管理與傳送 | OP 或 zero 最高權限 |

`tokaPortal.points.manage` 預設只有 OP。傳送點名稱不可使用 `reserved-names` 內的字。
`/tpa` 的規則：OP（或 zero 最高權限）直接傳送；zero 高權限者稍待片刻後傳送；其餘玩家需對方同意。

## 設定（`config.yml`，變更後需重新啟動）

| key | 預設 | 說明 |
| --- | --- | --- |
| `language` | `zh-TW` | 訊息語言 |
| `reserved-names` | help、list、set、del、edit | 不可當作住家或傳送點名稱的字 |
| `wild.worlds` | `[resource]` | 可使用 `/wild` 的世界；清單留空則所有世界都不能用 |
| `wild.cooldown-seconds` | `40` | `/wild` 冷卻秒數（1–86400） |
| `wild.radius` | `375` | 以目前位置為中心的隨機範圍（格） |
| `wild.attempts` | `16` | 找不到安全落點時的重試次數 |
| `wild.show-cooldown` | `true` | 冷卻中以血條顯示進度（需 tokaDisplay） |
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

目前版本 2.6.1
