# 第三方授權與來源

查核日期：2026-10-03。本文件記錄已查得的上游條款與本專案的使用方式，並非完整法律合規認證。

## tokaPortal 原專案

來源：https://github.com/zisunny104/tokaPortal

查核時原倉庫未提供 LICENSE。本次依專案維護者指示為專案程式碼加入 MIT 授權；第三方程式碼仍受原有條款約束。

## Nukkit PetteriM1 Edition

- 上游：https://github.com/PetteriM1/NukkitPetteriM1Edition
- 授權：https://github.com/PetteriM1/NukkitPetteriM1Edition/blob/release/LICENSE
- 查得授權：GNU GPL version 3。
- 使用版本：官方發行版 4511；本機 Maven 座標 `cn.nukkit:Nukkit:PM1E-4511`。
- 使用方式：伺服器核心與 provided 依賴，未將核心及其內部依賴打包至 tokaPortal JAR。

查核透過 GitHub license API 與 LICENSE 全文完成。GPLv3 與插件連結的影響不能單憑 provided 或分開打包排除。
依 FSF 對插件的說明，函式呼叫與共用資料結構可能構成結合程式；散布時應依實際組合評估 GPL 義務。
本專案選用 MIT 並不授予任何 GPL 上游程式的重新授權權利。

參考：https://www.gnu.org/licenses/gpl-faq.en.html#GPLPlugins

## PowerNukkit

- 上游：https://github.com/PowerNukkit/PowerNukkit
- 授權：https://github.com/PowerNukkit/PowerNukkit/blob/master/LICENSE
- 查得授權：GNU GPL version 3。
- 使用方式：原 pom 的 `org.powernukkit:powernukkit:1.5.2.1-PN`，目前已移除。

此查核核對官方倉庫 LICENSE，未對歷史 1.5.2.1-PN 的所有檔案做逐一來源追溯。

## bStats-Metrics

- 上游：https://github.com/Bastian/bStats-Metrics
- 查得授權：MIT。
- 授權來源：https://github.com/Bastian/bStats-Metrics/blob/cb92d2653c5aa478599c5638c446371d247d6123/LICENSE
- 上游聲明：Copyright (c) 2021 Bastian Oppermann。
- 授權全文：[licenses/bstats-MIT.txt](licenses/bstats-MIT.txt)。
- 對應檔案：`src/main/java/dev/toka/pl/tokaPortal/bstats/MetricsLite.java`。

本專案包含舊版 Nukkit 移植程式，後續修改包括改用公開服務註冊 API，以避開 Java 21 不相容反射。
已確認 bStats-Metrics 客戶端上游的 MIT 條款，但原專案未記錄此 Nukkit 移植版本的來源 URL 或完整修改歷史，無法確認所有移植修改的來源。
保留上游聲明不等於此來源缺口已完成查核；取得移植來源後仍需核對是否有額外條款或著作權聲明。

建置產物包含 `META-INF/licenses/bstats-MIT.txt` 與本文件，不會用 tokaPortal 的著作權聲明取代上游聲明。

## zero

僅確認程式以反射使用 `prj.toka.zero` API。尚未取得 zero 的實際 JAR、原始碼或授權文件，授權未定。
未將 zero 程式碼或 JAR 包入 tokaPortal；未來拆分或複製 zero 程式碼前，需先核對其授權及第三方來源。

## SQLite JDBC

- 上游：https://github.com/xerial/sqlite-jdbc/tree/3.53.4.0
- 使用版本：`org.xerial:sqlite-jdbc:3.53.4.0`，包含於插件 JAR。
- 授權：Apache License 2.0；原 Zentus 程式碼適用 BSD-2-Clause，SQLite 核心為公有領域。
- 授權與聲明：[LICENSE](licenses/sqlite-jdbc-LICENSE.txt)、[Zentus LICENSE](licenses/sqlite-jdbc-LICENSE.zentus.txt)、[NOTICE](licenses/sqlite-jdbc-NOTICE.txt)。

上述文件亦放入 JAR 的 `META-INF/licenses/`。驅動依上游方式包含原生函式庫；未修改驅動程式碼。