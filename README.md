# Data2Perms

Data2Perms 是一個 Paper 外掛，用來把「其他 YAML 檔中的玩家數值資料」同步成 LuckPerms 權限節點。

核心用途：
- 從指定 YAML 檔案某個 section 讀取 `UUID -> 整數`。
- 依照設定轉成權限節點，例如：`residence.max.36`。
- 可附帶 LuckPerms contexts（例如 `world=survival`）。
- 可在寫入前清理同前綴舊節點（可控範圍）。

---

## 1. 系統需求

- Java 21
- Paper 1.21.8（開發目標版本）
- LuckPerms（需安裝，且可由 Bukkit Services 提供 API）

---

## 2. 專案結構

```text
src/main/java/tw/origincraft/data2Perms/
├─ Main.java
└─ manager/
   ├─ CommandManager.java      # /data2perms 指令入口（sync/reload）
   ├─ DataManager.java         # 載入 config mappings 與來源 YAML
   └─ LuckPermsManager.java    # 套用/清理 LuckPerms 節點

src/main/resources/
├─ plugin.yml                  # 外掛描述、指令、權限
└─ config.yml                  # mappings 設定範本
```

---

## 3. 建置與啟動開發伺服器

### 建置

```bash
./gradlew build
```

產物通常在：

```text
build/libs/
```

### 本機啟動 Paper（開發用）

```bash
./gradlew runServer
```

此任務會使用 `build.gradle.kts` 指定的 Minecraft 版本（1.21.8）。

---

## 4. 安裝到伺服器

1. 先安裝 LuckPerms 到你的 Paper 伺服器。
2. 將本專案建置出的 Data2Perms jar 放到 `plugins/`。
3. 啟動伺服器一次，產生預設 `plugins/Data2Perms/config.yml`。
4. 編輯 `config.yml` 的 `mappings`。
5. 執行 `/data2perms reload` 重新載入設定。
6. 執行 `/data2perms sync` 實際同步資料到 LuckPerms。

> 若啟動時抓不到 LuckPerms provider，外掛會自動停用。

---

## 5. config.yml 設定說明

`mappings` 是一個陣列，每個元素描述一組「來源資料 -> 權限前綴」規則。

### 範例

```yaml
mappings:
  - file: "plugins/SomePlugin/data.yml"
    section: "PlayerMaxResidences"
    permission: "residence.max"
    contexts:
      residence: "yes"
      world: "survival"
    delete_scope: "same_contexts"
```

### 欄位

- `file`（必填）
  - 來源 YAML 檔案路徑。
  - 以伺服器工作目錄解析（通常是伺服器根目錄）。
- `section`（必填）
  - 來源 YAML 中要讀取的 section 名稱。
- `permission`（必填）
  - 權限前綴。
  - 若來源值為 `36`，最後寫入節點會是：`<permission>.36`。
- `contexts`（選填）
  - 物件格式（key/value 字串）。
  - 會被附加到 LuckPerms node 上。
- `delete_scope`（選填，預設 `none`）
  - `none`：不刪舊節點。
  - `same_contexts`：只刪「同前綴 + 完全相同 contexts」舊節點。
  - `all_prefix`：刪除該玩家所有同前綴舊節點。

---

## 6. 來源資料格式要求

被讀取的 `section` 內必須是：

- key：玩家 UUID（字串）
- value：正整數（`> 0`）

例如：

```yaml
PlayerMaxResidences:
  550e8400-e29b-41d4-a716-446655440000: 12
  9c858901-8a57-4791-81fe-4c455b099bc9: 36
```

不合法資料（UUID 錯誤、非整數、<= 0、null）會被跳過並記錄 warning。

---

## 7. 指令與權限

### 指令

- `/data2perms reload`
  - 重新載入 Data2Perms 的 `config.yml`。
- `/data2perms sync`
  - 執行同步：讀取所有 mappings，逐筆寫入 LuckPerms。

### 權限

- `data2perms.admin`
  - 預設 `op`。
  - 無此權限者無法執行上述指令。

---

## 8. 同步流程（實際行為）

執行 `/data2perms sync` 時：

1. 讀取 `config.yml` 的全部 mappings。
2. 逐一開啟來源 YAML，抓取對應 section。
3. 將合法的 `UUID -> value` 轉為待寫入項目。
4. 對每位玩家：
   - 非同步載入 LuckPerms user。
   - 按 `delete_scope` 清理舊節點（如有設定）。
   - 寫入新節點 `permission.value`（附 contexts）。
   - 儲存 user。
5. 全部 futures 完成後，回報成功/失敗數。

---

## 9. 常見問題排查

- **啟動顯示 LuckPerms not found**
  - 確認 LuckPerms 已安裝並正常啟動。
- **sync 顯示 No mappings found or all files missing**
  - 檢查 `config.yml` 是否有 `mappings`。
  - 檢查 `file` 路徑是否正確、檔案存在。
- **某些玩家沒被同步**
  - 看 console warning：可能是 UUID 格式錯、值不是正整數。
- **權限節點重複累積**
  - 調整 `delete_scope` 為 `same_contexts` 或 `all_prefix`。

---

## 10. 授權

本專案採用 `LICENSE` 所示授權條款。
