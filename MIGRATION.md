# GFBS: Misc 仓库迁移与许可证准备

> 生成日期：2026-10-29（Asia/Shanghai）
> 依据：本机只读核查（`E:\ghcli\gh.exe` + 完整路径 git），未执行任何 push / commit / transfer / API 写操作。
> 本文是本次任务唯一新增的文件；本任务未改动 `README.md`、`LICENSE`、`LICENSE.LESSER`、`NOTICE`、`THIRD_PARTY_ASSETS.md`、`mods.toml`。
> 注意：核查期间检测到其他进程/代理对 `README.md`、`tools/compat_baseline.json`、`tools/compat_check.py`、`pickaxe.json` 的并发修改，因此“当前状态”和 `git status` 结果会随执行时间变化；迁移前必须以实时 `git status` / `git diff` 为准。
> 结论以本文记录的实际输出为准；`HANDOVER.md` 第 13 节中“个人远端是 private、本地工作区干净”的描述已被实测推翻。

---

## 0. 结论摘要（先看这里）

1. **三处状态互相矛盾**：本地仓库是 LGPL-3.0 且只有 1 个提交（`d891b11`）；个人远端 `tfr-cz/GFBS-Misc` 实际是 **public**，且比本地多 2 个提交，把 `LICENSE` 改成了 **MIT** 并删除了 `LICENSE.LESSER`；组织仓库 `LytharaLab/GFBS-Misc` 也是 **public + MIT**，只有一句 README。
2. **`tfr-cz` 对组织仓库没有写权限**：`permissions` 只有 `pull: true`，`push/admin/maintain/triage` 全为 `false`；`gh api user/orgs` 返回 `[]`，组织成员接口返回 HTTP 404。写权限、组织成员身份、创建仓库权限都需要组织 owner（scaffold 初始提交作者是 `Nuclear_rea`）处理。
3. **同名仓库 + MIT LICENSE 是硬阻塞**：transfer 到 `LytharaLab/GFBS-Misc` 之前，必须由 owner 把现有 scaffold 仓库改名或删除；其根目录 `LICENSE`（MIT）不能保留为项目许可证。
4. **个人远端必须先修许可证**：直接把当前个人远端 transfer 过去，会把 MIT LICENSE（且没有 `LICENSE.LESSER`）带进组织仓库，与 README 的 LGPL-3.0 声明、NTM 资产条款直接冲突。本地 `git push` 也会因 non-fast-forward 被拒。
5. **推荐方案 A（真正 transfer）**，但前置步骤是：先修个人远端许可证 → owner 腾出仓库名并邀请 `tfr-cz` → transfer → 统一到 `main`。方案 B 仅在 owner 不愿邀请/改名时使用，代价是对 public 的 `main` 强推并丢弃 scaffold 提交。
6. **阻塞解除前不要 push / transfer**。本地还有未提交改动（`pickaxe.json`）和未跟踪的 `HANDOVER.md`，必须先处理。

---

## 1. 当前状态

### 1.1 状态表（实测）

| 位置 | 地址 | 可见性 | 默认分支 | HEAD | 当前身份实际权限 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 本地 | `G:\qoder\work\gfbs\GFBS-Misc` | — | `master` | `d891b117c8b3bee148aa7ef856d4156c18ed92dd` | 工作区可写 | 工作区**不干净**；`origin/master` 跟踪引用过期（仍指向 `d891b11`） |
| 个人远端 | `https://github.com/tfr-cz/GFBS-Misc` | **public**（`visibility: PUBLIC`，`private: false`） | `master` | `63ffe9ec7e690a5cd07c63882cae363a89045ccd` | `admin/maintain/push/triage/pull` 全 `true` | 比本地多 2 个提交：MIT 改写 + 删除 `LICENSE.LESSER` |
| 组织仓库 | `https://github.com/LytharaLab/GFBS-Misc` | **public**（`visibility: public`） | `main` | `627cacedffd6beb1e9acc6f0f7470c75353dee21` | **仅 `pull: true`**；`admin/maintain/push/triage` 全 `false` | 内容仅 `README.md`(67 B) + `LICENSE`(1067 B, MIT)；无分支保护 |

补充事实：

- 个人远端 `pushed_at = 2026-10-25T14:40:02Z`，`size = 2428` KB；GitHub 许可证识别为 **MIT**。
- 组织仓库 `created_at = 2026-10-25T00:20:48Z`；初始提交 `627cace` 作者 `Nuclear_rea`；GitHub 许可证识别为 **MIT**。
- 组织 `LytharaLab` 创建于 2026-01-22，公开仓库 16 个；组织公开成员列表只有 `Convex89524`。
- 本地提交身份（仓库级配置）：`user.name = LytharaLab`，`user.email = 328635470+tfr-cz@users.noreply.github.com`。
- 本机 `gh`：2.103.2，登录账号 `tfr-cz`（id 328635470），token 存 keyring，协议 https，scopes 为 `gist, read:org, repo`（**没有 `admin:org`**）。

### 1.2 `gh.exe` 只读核查记录（命令与真实输出）

以下命令均在 `G:\qoder\work\gfbs\GFBS-Misc` 下执行，只读，无 POST/PATCH/DELETE。

**① `gh auth status`**

```text
E:\ghcli\gh.exe auth status
github.com
  ✓ Logged in to github.com account tfr-cz (keyring)
  - Active account: true
  - Git operations protocol: https
  - Token scopes: 'gist', 'read:org', 'repo'
```

**② 个人仓库**

```text
E:\ghcli\gh.exe api repos/tfr-cz/GFBS-Misc --jq '{private,default_branch,permissions}'
{"default_branch":"master","permissions":{"admin":true,"maintain":true,"pull":true,"push":true,"triage":true},"private":false}
```

```text
E:\ghcli\gh.exe repo view tfr-cz/GFBS-Misc --json isPrivate,visibility,defaultBranchRef
{"defaultBranchRef":{"name":"master"},"isPrivate":false,"visibility":"PUBLIC"}
```

**③ 组织仓库**

```text
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc --jq '{private,default_branch,permissions}'
{"default_branch":"main","permissions":{"admin":false,"maintain":false,"pull":true,"push":false,"triage":false},"private":false}
```

```text
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc --jq '{full_name,visibility,default_branch,archived,fork,owner:.owner.login}'
{"archived":false,"default_branch":"main","fork":false,"full_name":"LytharaLab/GFBS-Misc","owner":"LytharaLab","visibility":"public"}
```

**④ 组织成员身份（失败，记录 HTTP 状态）**

```text
E:\ghcli\gh.exe api -i orgs/LytharaLab/memberships/tfr-cz
HTTP/2.0 404 Not Found
{"message":"Not Found","documentation_url":"https://docs.github.com/rest/orgs/members#get-organization-membership-for-a-user","status":"404"}
# stderr: gh: This API operation needs the "admin:org" scope.
```

交叉验证（更能说明问题）：

```text
E:\ghcli\gh.exe api user/orgs          → []
E:\ghcli\gh.exe api user/memberships/orgs/LytharaLab   → HTTP 404 Not Found
E:\ghcli\gh.exe api orgs/LytharaLab/members --jq '.[].login' → Convex89524
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/collaborators/tfr-cz/permission
→ HTTP 403 {"message":"Must have push access to view collaborator permission."}
```

结论：`tfr-cz` 不是 `LytharaLab` 成员（或至少当前 token 无法证明是），且对仓库只有公开仓库的读权限。`push=false` 是决定性证据——**没有写权限**。

**⑤ 组织仓库根目录内容**

```text
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/contents --jq '.[] | {name,type,size,sha}'
{"name":"LICENSE","sha":"6c3b5244eb8cb3c6e6ee280619d5247afa6f806d","size":1067,"type":"file"}
{"name":"README.md","sha":"04d3825a4ed5d5ed6f10bbae6aa17c91e28c7fe1","size":67,"type":"file"}
```

组织仓库 README 正文（原样）：

```text
# GFBS-Misc
各种奇怪东西的聚集地，有用的或没用的
```

组织仓库 LICENSE 开头（原样）：

```text
MIT License

Copyright (c) 2026 LytharaLab

Permission is hereby granted, free of charge, to any person obtaining a copy
...
```

**⑥ 额外只读核查（本文档新增的判断依据）**

```text
# 个人远端与本地 HEAD 的差异
E:\ghcli\gh.exe api 'repos/tfr-cz/GFBS-Misc/compare/d891b11...master' --jq '{status,ahead_by,behind_by,total_commits}'
{"ahead_by":2,"behind_by":0,"status":"ahead","total_commits":2}

E:\ghcli\gh.exe api 'repos/tfr-cz/GFBS-Misc/compare/d891b11...master' --jq '.files[] | {filename,status,additions,deletions}'
{"additions":21,"deletions":674,"filename":"LICENSE","status":"modified"}
{"additions":0,"deletions":165,"filename":"LICENSE.LESSER","status":"removed"}

# 个人远端提交（ConvertFrom-Json 读取，避免 jq 转义问题）
63ffe9e  2026-10-25T14:40:01Z  Delete LICENSE.LESSER
5bda1d0  2026-10-25T14:38:18Z  Update LICENSE
d891b11  2026-10-25T13:51:09Z  Initial commit: GFBS : Misc 0.1.0 (doors + cargo/cabin elevators)

# 组织仓库提交/分支/保护
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/commits --jq ...
627cace 2026-10-25T00:20:49Z Nuclear_rea Initial commit

E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/branches --jq ...
{"name":"main","protected":false,"sha":"627cace..."}

E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/branches/main/protection
HTTP/2.0 404 Not Found   # main 没有分支保护

# GitHub 许可证识别
repos/tfr-cz/GFBS-Misc      → "license": "MIT"
repos/LytharaLab/GFBS-Misc  → "license": "MIT"
```

`git ls-remote origin` 也确认远端 HEAD 为 `63ffe9e`，而本地 `origin/master` 跟踪引用仍是 `d891b11`（未 fetch，过期）：

```text
63ffe9ec7e690a5cd07c63882cae363a89045ccd  HEAD
63ffe9ec7e690a5cd07c63882cae363a89045ccd  refs/heads/master
```

---

## 2. 阻塞点清单（谁需要做什么）

| ID | 阻塞点 | 实测证据 | 谁需要做 | 完成标准 |
| --- | --- | --- | --- | --- |
| B1 | `tfr-cz` 不是 `LytharaLab` 组织成员 | `user/orgs = []`；membership API 404；公开成员只有 `Convex89524` | 组织 owner `Nuclear_rea` 发出邀请 | 邀请被 `tfr-cz` 接受；`user/orgs` 出现 `LytharaLab` |
| B2 | `tfr-cz` 对组织仓库无写权限 | `permissions.push=false`、`admin=false` | owner 邀请入组织，并在组织设置中允许成员创建仓库；或直接授予目标仓库 Write/Admin | `permissions.push=true`（至少） |
| B3 | 组织仓库同名，且已含 **MIT LICENSE** | `repos/LytharaLab/GFBS-Misc` 已存在，`LICENSE` sha `6c3b524...`，GitHub license=MIT | owner 先改名（推荐 `GFBS-Misc-scaffold`）或删除 scaffold | `LytharaLab/GFBS-Misc` 不再存在，或不再是当前项目目标名 |
| B4 | 个人远端许可证已被改成 MIT，且删了 `LICENSE.LESSER` | 提交 `5bda1d0`、`63ffe9e`；当前 LICENSE 为 MIT，README 仍写 LGPL-3.0 | `tfr-cz`（本地仓库可写者）在 transfer 前修复 | 远端 `master` 的树恢复 `LICENSE`(GPL-3.0 全文) + `LICENSE.LESSER`(LGPL-3.0 全文)，GitHub license 不再显示 MIT |
| B5 | 本地工作区不干净且落后远端 2 个提交 | `M pickaxe.json`、`?? HANDOVER.md`；`git remote show` 显示 `local out of date` | 迁移执行者 | `git status` 干净；fetch 后 `master == origin/master` |
| B6 | 默认分支不一致（`master` vs `main`） | 个人远端 `master`；组织 `main` | 迁移执行者 + owner 决定 | 明确保留 `master` 或统一改名 `main`，并设置默认分支 |
| B7 | 可见性与资产分发合规未确认 | 个人远端已是 public；组织仓库是 public；NTM 资产为 LGPL-3.0 | owner + 维护者 | 确认 public 分发符合 NTM 资产条款；否则迁移后立即转 private。**无论哪种，都不能标 MIT** |
| B8 | 当前 token 无 `admin:org`，无法管理成员/组织设置 | `gh auth status` scopes 无 `admin:org`；membership API 提示需该 scope | owner（用自己的 token 或 GitHub UI） | 成员/权限调整不再依赖 `tfr-cz` 的 token |

> 关键结论：**所有写操作（成员邀请、改组织设置、腾仓库名、授予权限）都必须由组织 owner `Nuclear_rea` 完成**；`tfr-cz` 目前连仓库级 Write 都没有，更不可能在组织内创建/转移仓库。

---

## 3. 许可证一致性审计

### 3.1 README 的声明

`README.md` 的 `## License and credits` 段（核查时位于第 59–63 行；行号可能随后续编辑变化）：

- 代码：**GNU Lesser General Public License v3.0**，指向 `LICENSE`、`LICENSE.LESSER`、`NOTICE`、`THIRD_PARTY_ASSETS.md`。
- 门模型/贴图/音效**以及货梯几何与贴图**来自 HBM's Nuclear Tech Mod（NTM），**仍受 NTM 许可证约束**。
- `GFBS: glTF` 是 MIT，运行时依赖，**不随本模组打包**。

其他声明点：

- `gradle.properties`：`mod_license=LGPL-3.0`、`mod_version=0.1.0`。
- `src/main/resources/META-INF/mods.toml`：`license = "${mod_license}"`（打包后即 `LGPL-3.0`）。

### 3.2 LICENSE / LICENSE.LESSER 正文核对

| 文件 | 实际正文 | 行数/大小 | 判定 |
| --- | --- | --- | --- |
| `LICENSE` | **GNU GENERAL PUBLIC LICENSE Version 3**（GPL-3.0 全文，不是 LGPL 正文） | 674 行 / 35,129 B / SHA256 `605E9047A563C5C8…` | 不是 LGPL-3.0 正文；但 LGPL-3.0 本身以 GPL-3.0 为基础，需配合 `LICENSE.LESSER` 才能表达 LGPL |
| `LICENSE.LESSER` | **GNU LESSER GENERAL PUBLIC LICENSE Version 3**（LGPL-3.0 附加条款全文） | 165 行 / 7,652 B / SHA256 `E3A994D82E644B03…` | 是 LGPL-3.0 正文 |
| `src/main/resources/META-INF/LICENSE` | 与根目录 `LICENSE` 完全相同 | SHA256 `605E9047A563C5C8…` | 会打进 jar |
| `src/main/resources/META-INF/LICENSE.LESSER` | 与根目录 `LICENSE.LESSER` 完全相同 | SHA256 `E3A994D82E644B03…` | 会打进 jar |

判定：**组合是“GPL-3.0 全文 + LGPL-3.0 附加条款”**，在 LGPL 的常见打包方式里可以成立，但文件命名/README 表述容易让人（和 GitHub 许可证识别）误读为 GPL-3.0。如果希望“LICENSE 一看就是 LGPL-3.0”，需要额外说明或调整文件布局（见第 8 节，本次不改）。

### 3.3 NOTICE 核对

`NOTICE`（12 行）：

- 第 1 行标题是 **“GFBS: Doors”**，而项目名是 **“GFBS: Misc”**——名称未同步，属于文档瑕疵。
- 正确声明 LGPL-3.0，并指向 `LICENSE`、`LICENSE.LESSER`。
- 正确声明 NTM 1.0.27 为 LGPL-3.0，PHEO 门资产归原作者。
- 正确声明 `GFBS: glTF` 为 MIT、不随包分发。

### 3.4 THIRD_PARTY_ASSETS.md 覆盖情况与路径偏差

覆盖范围本身是够的：NTM 门几何、门贴图、门音效、Transition Seal、货梯几何/贴图，以及 `GFBS: glTF`（MIT，不打包）。

但**文件里写的路径与本仓库实际资源路径不一致**（像是直接抄了 NTM 源仓库路径）：

| `THIRD_PARTY_ASSETS.md` 写的路径 | 本仓库实际路径 |
| --- | --- |
| `pheodoors/*.obj` | `src/main/resources/assets/gfbs_doors/models/doors/*.obj`（`airlock_door`、`blast_door`、`vault_door` 等 15 个 OBJ；没有 `pheodoors/` 目录） |
| `doors/silo_hatch*.obj` | `src/main/resources/assets/gfbs_doors/models/doors/silo_hatch.obj`、`silo_hatch_large.obj` |
| `textures/models/pheodoors/**` | 实际贴图在 `src/main/resources/assets/gfbs_doors/models/doors/tex/*.png`（不是 `textures/models/...`） |
| `textures/models/doors/*.png` | 同上，`models/doors/tex/*.png` |
| `sounds/block/door/*.ogg`、`sounds/block/vault*.ogg` | 实际为 `assets/gfbs_doors/sounds/door/*.ogg`（21 个）与 `assets/gfbs_doors/sounds/block/*.ogg`（4 个：`reactor_start/stop`、`vault_scrape_new`、`vault_thud_new`） |
| Transition Seal（`models/doors/seal.dae`） | 实际为 `models/doors/transition_seal.obj` + `.mtl` + `.anim`；`.dae` 是 NTM 源文件，不在本仓库 |
| Cargo elevator geometry/texture | 实际为 `models/machines/elevator.obj`、`.mtl`、`models/machines/tex/elevator.png` |

这些偏差不影响“资产属于 NTM、受 LGPL-3.0 约束”的法律结论，但会削弱可审计性，迁移时建议顺手修正（见第 8 节）。

### 3.5 未被 NOTICE / THIRD_PARTY_ASSETS 覆盖的已跟踪二进制

- `libs/gfbs_main_api_stub.jar`（9,714 B）是**已跟踪**文件，`build.gradle` 注明它是 GFBS: Main 公开命令 API 的 compile-only 切片。
- 但 `THIRD_PARTY_ASSETS.md` 没有提到它，`NOTICE` 也没有。它虽然只是编译桩，仍是第三方代码衍生物，迁移前应补一行说明（许可证归属 GFBS: Main）或从仓库移除、改为本地生成。
- 另外 `gradle/wrapper/gradle-wrapper.jar` 是 Gradle 官方分发物，通常无需单列，但严格审计时可在 NOTICE 里补一句。

### 3.6 组织仓库现有 MIT LICENSE 的具体冲突点

组织仓库当前只有一个提交 `627cace`，两个文件：

1. `LICENSE`（sha `6c3b5244eb8cb3c6e6ee280619d5247afa6f806d`，1067 B）
   - 正文第一行 `MIT License`，版权行 `Copyright (c) 2026 LytharaLab`。
   - GitHub 已据此把整个 `LytharaLab/GFBS-Misc` 识别为 **MIT**。
2. `README.md`（sha `04d3825a4ed5d5ed6f10bbae6aa17c91e28c7fe1`，67 B）
   - 只有一句中文标语，**没有任何许可证说明**；因此 GitHub 只能依据 `LICENSE` 判定为 MIT。

冲突点具体到文件/条目：

- 若把本项目内容推进/transfer 进这个仓库而**保留**该 `LICENSE`：LGPL-3.0 代码会被以 MIT 条款对外呈现——MIT 比 LGPL 宽松，等于对下游额外授权，同时**错误标注了 NTM 资产**（NTM 资产是 LGPL-3.0，不是 MIT）。
- 该 MIT `LICENSE` 删掉后，仓库根目录必须换成 `LICENSE`（GPL-3.0 全文）+ `LICENSE.LESSER`（LGPL-3.0 附加条款）+ `NOTICE` + `THIRD_PARTY_ASSETS.md`；`README.md` 必须换成/合并本项目的 README 许可证段落。
- 组织仓库 `main` 无分支保护（protection API 404），所以技术上可强推；但这不解决许可证问题。

### 3.7 个人远端的 MIT 改写（迁移前必须处理）

`tfr-cz/GFBS-Misc` 当前 `master` 比本地多 2 个提交：

| 提交 | 说明 | 影响 |
| --- | --- | --- |
| `5bda1d0` | `Update LICENSE`（+21 / −674） | 把根目录 `LICENSE` 从 GPL-3.0 全文改成 MIT（`Copyright (c) 2026 tfr-cz`） |
| `63ffe9e` | `Delete LICENSE.LESSER`（−165） | 删除了 LGPL-3.0 附加条款 |

但该远端的 `README.md` 没有同步修改，仍写“code is licensed under the GNU Lesser General Public License v3.0”。结果：

- GitHub 识别：`MIT`；
- 文件声明：`LGPL-3.0`；
- NTM 资产：LGPL-3.0，却被 MIT LICENSE 覆盖。

这是**目前最严重的许可证不一致**。若现在 transfer，会把这两个提交和 MIT LICENSE 一起带进组织仓库；若现在直接 `git push`，会因为本地 `d891b11` 不是 `63ffe9e` 的祖先而被拒绝（non-fast-forward）。**必须先修复个人远端，再谈迁移。**

---

## 4. 方案 A：真正 transfer（保留提交历史与 URL 重定向）

### 4.1 前置条件（全部满足才执行）

- [ ] owner `Nuclear_rea` 已把 `LytharaLab/GFBS-Misc` 改名为 `GFBS-Misc-scaffold`（或删除）。
- [ ] `tfr-cz` 已接受组织邀请，且组织允许成员创建仓库（Organization settings → Member privileges → Repository creation）。
- [ ] 个人远端 `tfr-cz/GFBS-Misc` 的 `master` 已恢复 LGPL-3.0（`LICENSE` + `LICENSE.LESSER`），GitHub 不再显示 MIT。
- [ ] 本地工作区干净（`pickaxe.json` 已提交或 stash，`HANDOVER.md` 已决定去留）。
- [ ] `./gradlew build --offline` 通过，`build/libs/gfbs-misc-0.1.0.jar` 存在。
- [ ] 已创建备份 tag（见 9.1）。

命令统一在 PowerShell 7 下执行；先设好变量：

```powershell
$git = 'C:\Users\Administrator\.qoder\bin\git\cmd\git.exe'
$gh  = 'E:\ghcli\gh.exe'
Set-Location 'G:\qoder\work\gfbs\GFBS-Misc'
```

> 所有 `git push` 都必须带证书参数：`-c http.sslBackend=schannel -c http.schannelCheckRevoke=false`。`gh api` 走 Go 的 TLS，不需要这两个参数。

### 4.2 步骤 A-0：备份与本地清理

```powershell
# 0.1 保存未提交改动（pickaxe tag 改动 + HANDOVER.md）；迁移完成后再 pop
& $git stash push -u -m 'pre-migration wip: pickaxe tags + HANDOVER.md'

# 0.2 同步并打本地备份 tag（d891b11 是当前正确许可证的提交）
& $git fetch origin
& $git tag -f backup/pre-migration-20261029 d891b11
& $git tag -f backup/tfr-cz-master-mit 63ffe9e

# 0.3 把“迁移前个人远端状态”推到远端作为后悔药（可选，但建议）
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push origin refs/tags/backup/tfr-cz-master-mit
```

### 4.3 步骤 A-1：修复个人远端许可证（二选一，推荐 A-1a）

**A-1a（推荐，保留历史，用恢复提交覆盖 MIT）**

```powershell
& $git reset --hard origin/master                 # 到 63ffe9e（当前 MIT 状态）
& $git checkout d891b11 -- LICENSE LICENSE.LESSER # 从正确提交恢复两个许可证文件
& $git commit -m 'Restore LGPL-3.0 license files (revert MIT relicensing)'
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push origin master
```

结果：历史中仍能看到 `5bda1d0` / `63ffe9e`，但 `HEAD` 树恢复为 LGPL-3.0，GitHub 许可证识别应变为 GPL-3.0/LGPL-3.0，README 与文件重新一致。

**A-1b（可选，重写历史，丢弃两个 MIT 提交）**

```powershell
& $git reset --hard d891b11
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push --force-with-lease=master:63ffe9e origin master
```

结果：远端历史回到干净的单提交 `d891b11`；但这是对 public 仓库的 history rewrite，且会丢掉 `backup/pre-migration-20261029` 之外的审计线索。**仅当确定不需要保留 MIT 提交痕迹时使用。**

### 4.4 步骤 A-2：owner 腾出仓库名

以下命令需要 **owner `Nuclear_rea` 的 token**（`tfr-cz` 的 token 会 403/404），或在 GitHub UI 操作：

```powershell
# 推荐：改名而不是删除，保留 scaffold 的 MIT LICENSE 作为独立空仓库
& $gh api -X PATCH repos/LytharaLab/GFBS-Misc -f name=GFBS-Misc-scaffold
& $gh api -X PATCH repos/LytharaLab/GFBS-Misc-scaffold -f archived=true
```

如果确定不要 scaffold：

```powershell
& $gh api -X DELETE repos/LytharaLab/GFBS-Misc
```

> 注意：`gh api` 的 `-X PATCH/DELETE` 是写操作。当前 `tfr-cz` 既不是成员也没有 admin，执行会失败；必须由 owner 执行或由 owner 在网页上操作。

### 4.5 步骤 A-3：owner 邀请 `tfr-cz` 并开放创建仓库权限

```powershell
# owner 执行
& $gh api -X PUT orgs/LytharaLab/memberships/tfr-cz -f role=direct_member
```

组织设置（只能 UI）：

- Organization settings → Member privileges → **Repository creation**：允许 members 创建仓库（public/private 按需要勾选）。
- 如果组织启用了 SSO/2FA 策略，需先满足。

`tfr-cz` 接受邀请后自检：

```powershell
& $gh api user/orgs --jq '.[].login'          # 期望出现 LytharaLab
& $gh api orgs/LytharaLab/memberships/tfr-cz  # 期望 200 且 state=active
```

### 4.6 步骤 A-4：执行 transfer

```powershell
& $gh api -X POST repos/tfr-cz/GFBS-Misc/transfer -f new_owner=LytharaLab
```

- 返回通常为 `202 {"message":"..."}`；若返回 422，按响应体排查（常见：目标已存在、无组织创建仓库权限、未接受邀请）。
- transfer 需要源仓库 admin（`tfr-cz` 有）和目标组织权限（owner 已配置）。
- transfer 会保留 issues/stars/watchers（本仓库基本为空），旧 URL 自动重定向到新仓库。

### 4.7 步骤 A-5：更新 remote、统一分支到 `main`

```powershell
& $git remote set-url origin https://github.com/LytharaLab/GFBS-Misc.git
& $git remote -v
& $git fetch origin

# 组织约定 main；把本地 master 改名并推上去
& $git branch -m master main
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push -u origin main

# 先切默认分支，再删旧的 master
& $gh api -X PATCH repos/LytharaLab/GFBS-Misc -f default_branch=main
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push origin --delete master
```

> 如果 transfer 后 `tfr-cz` 的 `permissions.push` 变成 `false`（transfer 不保证保留 admin），先让 owner 授权：
>
> ```powershell
> & $gh api -X PUT repos/LytharaLab/GFBS-Misc/collaborators/tfr-cz -f permission=admin
> ```

### 4.8 步骤 A-6：验收

```powershell
& $gh api repos/LytharaLab/GFBS-Misc --jq '{private,default_branch,permissions,license:.license.spdx_id}'
& $gh api repos/LytharaLab/GFBS-Misc/contents --jq '.[] | {name,size}'
& $gh api repos/LytharaLab/GFBS-Misc/branches --jq '.[] | {name,protected,sha:.commit.sha}'
```

期望：

- `default_branch = main`；
- 根目录有 `LICENSE`（GPL-3.0 全文，35 KB 级）、`LICENSE.LESSER`、`NOTICE`、`THIRD_PARTY_ASSETS.md`、`README.md`；
- **没有** 1 KB 的 MIT `LICENSE`；
- `license.spdx_id` 不再是 `MIT`（可能是 `GPL-3.0` 或 `LGPL-3.0`，取决于 GitHub 识别；两者都不等于 MIT 即可接受，但建议在 README 里明确写 LGPL-3.0）。

最后恢复本地 WIP：

```powershell
& $git stash list
& $git stash pop
```

---

## 5. 方案 B：直接把本地提交推到组织 `main`

适用条件：owner **不愿意**邀请入组织/腾仓库名，但愿意把 `tfr-cz` 加为目标仓库的 **Admin** 协作者。代价：必须对 public 的 `main` 做 `--force-with-lease`，scaffold 的 `627cace` 会从 `main` 消失（需先建备份分支）。

### 5.1 前置条件

- [ ] owner 已把 `tfr-cz` 加为 `LytharaLab/GFBS-Misc` 的 **Admin**（或至少 Write + 分支保护已关闭；当前无保护）。
- [ ] 已接受协作者邀请（不是组织成员也可以做 outside collaborator）。
- [ ] 已创建 scaffold 备份分支/tag（见 5.2）。
- [ ] 本地工作区干净、build 通过、jar 存在。
- [ ] 已确认最终许可证用本地的 LGPL-3.0 组合，不保留 MIT LICENSE。

### 5.2 步骤 B-0：备份组织 scaffold

```powershell
$git = 'C:\Users\Administrator\.qoder\bin\git\cmd\git.exe'
$gh  = 'E:\ghcli\gh.exe'
Set-Location 'G:\qoder\work\gfbs\GFBS-Misc'

# 本地保存 scaffold 提交对象
& $git fetch https://github.com/LytharaLab/GFBS-Misc.git main
& $git tag -f backup/org-scaffold 627cacedffd6beb1e9acc6f0f7470c75353dee21

# 在组织仓库上建备份分支（需要 Write；建议在获得权限后第一时间执行）
& $gh api -X POST repos/LytharaLab/GFBS-Misc/git/refs `
    -f ref=refs/heads/backup/scaffold-627cace `
    -f sha=627cacedffd6beb1e9acc6f0f7470c75353dee21
```

### 5.3 步骤 B-1：owner 授予 Admin 协作权限

```powershell
# owner 执行（用 owner 的 token 或 UI：Settings → Collaborators → Add people）
& $gh api -X PUT repos/LytharaLab/GFBS-Misc/collaborators/tfr-cz -f permission=admin
```

`tfr-cz` 接受邀请：

```powershell
& $gh api user/repository_invitations --jq '.[] | [.id, .repository.full_name] | @tsv'
# 记下邀请 id 后：
& $gh api -X PATCH user/repository_invitations/<INVITATION_ID>
```

验收权限：

```powershell
& $gh api repos/LytharaLab/GFBS-Misc --jq '.permissions'
# 期望 push=true（admin=true 更稳）
```

### 5.4 步骤 B-2：本地清理并确认许可证

```powershell
& $git stash push -u -m 'pre-migration wip: pickaxe tags + HANDOVER.md'
& $git status
& $git log --oneline -1
# 确认工作树 LICENSE 是 35 KB 的 GPL-3.0 全文、LICENSE.LESSER 存在：
Get-Item LICENSE,LICENSE.LESSER | Select-Object Name,Length
```

> 注意：本地方 `master` 是 `d891b11`，它**不包含**个人远端那两个 MIT 提交——这正是方案 B 想要的结果。不要先 `pull`/`reset` 到 `63ffe9e`，否则会把 MIT 带进来。

### 5.5 步骤 B-3：强推到组织 `main`

```powershell
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push `
    --force-with-lease=main:627cacedffd6beb1e9acc6f0f7470c75353dee21 `
    https://github.com/LytharaLab/GFBS-Misc.git master:main
```

- `--force-with-lease=main:<sha>` 防止在备份分支创建后有人又改了 `main`。
- 若 git 版本对 URL push 的 lease 解析报错，先确认 `main` 仍是 `627cace`，再用 `--force`（风险更高，不推荐）。
- 推完后 `main` 的历史 = `d891b11`（单提交），scaffold 的 `627cace` 只存在于备份分支。

### 5.6 步骤 B-4：切换 remote

```powershell
& $git remote rename origin personal
& $git remote add origin https://github.com/LytharaLab/GFBS-Misc.git
& $git fetch origin
& $git branch --set-upstream-to=origin/main master
& $git remote -v
```

### 5.7 步骤 B-5：验收

```powershell
& $gh api repos/LytharaLab/GFBS-Misc --jq '{private,default_branch,permissions,license:.license.spdx_id}'
& $gh api repos/LytharaLab/GFBS-Misc/contents --jq '.[].name'
& $gh api repos/LytharaLab/GFBS-Misc/commits --jq '.[].sha' | Select-Object -First 3
```

期望：根目录为本项目文件；无 MIT `LICENSE`；`main` 首个提交为 `d891b11`（scaffold 提交只留在 `backup/scaffold-627cace`）。

确认无误后可删除备份分支（保留 tag 也可以）：

```powershell
& $gh api -X DELETE repos/LytharaLab/GFBS-Misc/git/refs/heads/backup/scaffold-627cace
```

> 删除前务必确认不再需要回滚；这是写操作。

---

## 6. 推荐方案与理由

**推荐方案 A（真正 transfer）**，理由：

1. **保留提交历史与 URL 重定向**：`tfr-cz/GFBS-Misc` 的旧链接会自动跳到组织仓库，外部引用不会断；方案 B 需要强推并丢弃 scaffold 提交，历史被重写。
2. **许可证隔离更干净**：scaffold 的 MIT LICENSE 留在改名后的 `GFBS-Misc-scaffold`（可归档），不会与 LGPL-3.0 + NTM 资产混在同一个默认分支上；方案 B 虽然也能用本地树覆盖，但 scaffold 的 MIT 提交仍留在仓库对象/备份分支里，容易误读。
3. **不需要对 public 的 `main` 强推**：方案 B 的 `--force-with-lease` 一旦 lease 失效或备份分支没建好，回滚成本高；方案 A 的回滚是反向 transfer，风险低。
4. **长期维护成本低**：组织仓库成为唯一 canonical 仓库，个人仓库 transfer 后只剩重定向，不会出现“两个仓库各自提交”的分叉。
5. **阻塞成本可接受**：A 比 B 多的要求只是“邀请进组织 + 允许创建仓库”，而 B 同样需要 owner 授予 Admin；两者都卡在 owner，A 的一次性配置更值得。

**选择 B 的条件**（仅当）：owner 明确不愿让 `tfr-cz` 进组织或不愿改 scaffold 名，但愿意加 Admin 协作者；且接受强推 public `main`、丢弃 scaffold 提交。即便如此，也必须先按 5.2 建好备份分支。

**无论 A/B，迁移前都必须先做 A-1（修复个人远端 MIT）**；否则 A 会把 MIT 带进组织，B 虽然用本地树覆盖，但个人远端仍是 MIT + README 矛盾，后续容易再次误推。

---

## 7. 推送前检查清单

### 7.1 实测结果（2026-10-29）

| 检查项 | 结果 | 证据/说明 |
| --- | --- | --- |
| 工作区干净 | ❌ **不通过** | `M src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`（+16 行，给 16 个门/货梯方块补 pickaxe tag）；`?? HANDOVER.md`；核查期间另有其他并发改动：`M README.md`、`?? tools/compat_baseline.json`、`?? tools/compat_check.py`（非本任务产生，迁移前需一并处理） |
| `./gradlew build --offline` 通过 | ⏸ **本次未执行**（只读文档任务，避免产生构建写操作） | 必须在真正推送前执行并确认 `BUILD SUCCESSFUL`；`gradle.properties` 的 `mod_version=0.1.0` |
| `build/libs/gfbs-misc-0.1.0.jar` 存在 | ✅ **通过** | 3,180,958 B；但 `build/libs` 里还有 `1.1.0/1.1.1/1.1.2` 三个旧 jar（已 ignore），建议先 `clean` 避免误认 |
| `run/` 未被跟踪 | ✅ **通过** | `git ls-files` 无 `run/`；`.gitignore:3` 覆盖 |
| `.gradle/` 未被跟踪 | ✅ **通过** | `git ls-files` 无 `.gradle/`；`.gitignore:1` 覆盖 |
| `build/` 未被跟踪 | ✅ **通过** | `git ls-files` 无 `build/`；`.gitignore:2` 覆盖（含 `build/libs/*.jar`） |
| `libs/gfbs_gltf-*.jar` 未被跟踪 | ✅ **通过** | 实际存在 `libs/gfbs_gltf-1.5.1.jar`（2,618,363 B），但被 `.gitignore:13` 忽略，未跟踪 |
| `.mcdev/` 未被跟踪 | ⚠️ **间接通过** | 目录内有 6 个 `launch-*.log`，被 `.gitignore:6` 的 `*.log` 忽略；但 `.mcdev/` 本身**不在 .gitignore**，未来出现非 `.log` 文件（如 runs/snapshot）会被误提交 |
| 已跟踪 jar 清单 | ⚠️ **需知情** | 只有 `gradle/wrapper/gradle-wrapper.jar` 和 `libs/gfbs_main_api_stub.jar`（编译桩，9,714 B）；后者未在 `THIRD_PARTY_ASSETS.md` 说明 |
| 本地与远端同步 | ❌ **不通过** | 本地 `d891b11`，个人远端 `63ffe9e`（落后 2 个提交）；`git remote show origin` 显示 `local out of date` |
| 提交身份 | ✅ **通过** | `user.name = LytharaLab`，`user.email = 328635470+tfr-cz@users.noreply.github.com` |

### 7.2 `git status` / `git log` 实测

```text
$ C:\Users\Administrator\.qoder\bin\git\cmd\git.exe status
On branch master
Your branch is up to date with 'origin/master'.

Changes not staged for commit:
  modified:   src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json

Untracked files:
  HANDOVER.md

$ C:\Users\Administrator\.qoder\bin\git\cmd\git.exe log --oneline -3
d891b11 Initial commit: GFBS : Misc 0.1.0 (doors + cargo/cabin elevators)

$ C:\Users\Administrator\.qoder\bin\git\cmd\git.exe branch -vv
* master d891b11 [origin/master] Initial commit: ...

$ C:\Users\Administrator\.qoder\bin\git\cmd\git.exe remote -v
origin  https://github.com/tfr-cz/GFBS-Misc.git (fetch)
origin  https://github.com/tfr-cz/GFBS-Misc.git (push)
```

注意：`git status` 显示 “up to date with origin/master” 是**因为 `origin/master` 跟踪引用没有 fetch**；`git ls-remote origin` 的真实远端 HEAD 是 `63ffe9e`。`git status` 之前必须 `git fetch origin`。

上面的输出是任务开始时（约 22:03）的结果。写入本文前（约 22:09）再次执行 `git status --short`，已出现其他并发改动：

```text
 M README.md
 M src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json
?? HANDOVER.md
?? MIGRATION.md
?? tools/compat_baseline.json
?? tools/compat_check.py
```

其中 `README.md`、`tools/compat_baseline.json`、`tools/compat_check.py` 不是本任务产生的（本任务只新增 `MIGRATION.md`）。迁移执行前必须以实时 `git status` 为准，并决定这些并发改动是提交、stash 还是排除。

`pickaxe.json` 的改动内容（实际上是一处内容修复，迁移前应提交或 stash）：

```text
+ gfbs_doors:transition_seal
+ gfbs_doors:vault_door
+ gfbs_doors:sliding_blast_door
+ gfbs_doors:fire_door
+ gfbs_doors:silo_hatch
+ gfbs_doors:silo_hatch_large
+ gfbs_doors:secure_access_door
+ gfbs_doors:large_vehicle_door
+ gfbs_doors:qe_containment
+ gfbs_doors:qe_sliding_door
+ gfbs_doors:round_airlock_door
+ gfbs_doors:sliding_seal_door
+ gfbs_doors:water_door
+ gfbs_doors:cargo_door
+ gfbs_doors:blast_door
+ gfbs_doors:cargo_elevator
```

### 7.3 `.gitignore` 审计

当前内容（13 行）：

```text
.gradle/
build/
run/
out/
logs/
*.log
.idea/
*.iml
.vscode/
.settings/
.classpath
.project
libs/gfbs_gltf-*.jar
```

覆盖验证（`git check-ignore -v`）：

```text
.gitignore:6:*.log        .mcdev/launch-server-1793280766959.log
.gitignore:3:run/         run/
.gitignore:2:build/       build/libs/gfbs-misc-0.1.0.jar
.gitignore:1:.gradle/     .gradle/
.gitignore:13:libs/gfbs_gltf-*.jar  libs/gfbs_gltf-1.5.1.jar
```

缺口/建议（本次不改 `.gitignore`，仅记录）：

- **`.mcdev/` 整体未忽略**：目前靠 `*.log` 兜底；建议加一行 `.mcdev/`。
- `*.hprof`、`hs_err_pid*.log`（后者已被 `*.log` 覆盖）、`Thumbs.db`、`.DS_Store` 可视需要补。
- `libs/gfbs_main_api_stub.jar` 被有意跟踪（`build.gradle` 的 `compileOnly files(...)` 需要），不能加 `libs/*.jar` 这种宽泛规则，否则构建会坏。
- `out/` 已覆盖 IntelliJ 输出；`run/` 覆盖 Forge 运行目录；`build/` 覆盖 jar/classes。

### 7.4 推送前最后执行的命令

```powershell
$git = 'C:\Users\Administrator\.qoder\bin\git\cmd\git.exe'
Set-Location 'G:\qoder\work\gfbs\GFBS-Misc'

& $git fetch origin
& $git status --short --branch
& $git log --oneline -3
& $git ls-files | Select-String '^(run/|\.gradle/|build/|\.mcdev/)'   # 期望无输出
& $git ls-files '*.jar'                                              # 期望仅 wrapper + main_api_stub
Test-Path 'build\libs\gfbs-misc-0.1.0.jar'                           # 期望 True
.\gradlew build --offline                                            # 期望 BUILD SUCCESSFUL
```

---

## 8. 许可证处理建议（组织仓库）

### 8.1 必须做的替换

1. **删除/替换组织仓库根目录的 MIT `LICENSE`**（sha `6c3b524...`，1067 B）。
2. 放入本项目的：
   - `LICENSE`（GPL-3.0 全文，与本地一致，35,129 B）；
   - `LICENSE.LESSER`（LGPL-3.0 附加条款，7,652 B）；
   - `NOTICE`（修正标题为 “GFBS: Misc”）；
   - `THIRD_PARTY_ASSETS.md`（建议同时修正第 3.4 节的路径）。
3. **`README.md` 必须包含明确的许可证段落**（可直接用本地 README 第 56–60 行），说明：
   - 代码为 LGPL-3.0；
   - 门模型/贴图/音效来自 NTM 1.0.27，受 LGPL-3.0 约束；
   - `GFBS: glTF` 为 MIT、运行时依赖、不打包。
4. 组织仓库的 GitHub 许可证识别必须是 **LGPL-3.0**（或至少不是 MIT）。替换后可用 `gh api repos/LytharaLab/GFBS-Misc --jq '.license.spdx_id'` 复查；若 GitHub 仍显示 GPL-3.0，建议在 README 顶部加一行 “License: LGPL-3.0” 并在 `LICENSE` 首行加说明（LGPL-3.0 = GPL-3.0 + 附加条款），避免读者误判。

### 8.2 绝对不能做的事

- **不能把 NTM 资产标成 MIT**。NTM 1.0.27 是 LGPL-3.0，门模型/贴图/音效是 ported assets，必须保留 NTM 的许可证与 attribution（`NOTICE` + `THIRD_PARTY_ASSETS.md`）。
- 不能删除 `NOTICE` 或 `THIRD_PARTY_ASSETS.md` 来“简化”仓库。
- 不能把 `GFBS: glTF` 的 MIT 说成是整个项目的许可证；它只是运行时依赖，且不随包分发（`libs/gfbs_gltf-*.jar` 已 ignore，不应提交）。
- 不能把组织仓库 scaffold 的 MIT LICENSE 留在默认分支上“兼容”两种许可证——MIT 与 LGPL-3.0 对下游的授权不同，混用会造成实际授权不清。

### 8.3 建议顺手修（不阻塞迁移）

- `NOTICE` 标题 `GFBS: Doors` → `GFBS: Misc`。
- `THIRD_PARTY_ASSETS.md` 的路径改为本仓库实际路径（见 3.4 表）。
- 在 `THIRD_PARTY_ASSETS.md` 增加一行 `libs/gfbs_main_api_stub.jar` 的说明（GFBS: Main 公开 API 的 compile-only 切片，归属 GFBS: Main）。
- 如果决定公开分发，确认 NTM 资产再分发条款；不确定就先把组织仓库设为 private，等确认后再 public。个人远端目前已经是 public，这一点无法“回到未公开”，只能后续修正许可证表述。

---

## 9. 回滚步骤

### 9.1 迁移前备份（两套方案都先做）

```powershell
$git = 'C:\Users\Administrator\.qoder\bin\git\cmd\git.exe'
Set-Location 'G:\qoder\work\gfbs\GFBS-Misc'

& $git fetch origin
& $git tag -f backup/pre-migration-20261029 d891b11          # 正确 LGPL 的单提交
& $git tag -f backup/tfr-cz-master-mit 63ffe9e               # 迁移前个人远端（MIT 状态）
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push origin refs/tags/backup/tfr-cz-master-mit
```

### 9.2 方案 A 回滚

1) transfer 反向（需要 `tfr-cz` 对组织仓库有 admin，或 owner 执行）：

```powershell
& $gh api -X POST repos/LytharaLab/GFBS-Misc/transfer -f new_owner=tfr-cz
```

2) 若已把 `master` 改名 `main`，先恢复 `master` 并切默认分支：

```powershell
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push origin main:master
& $gh api -X PATCH repos/LytharaLab/GFBS-Misc -f default_branch=master
```

3) 恢复本地 remote 指向个人仓库：

```powershell
& $git remote set-url origin https://github.com/tfr-cz/GFBS-Misc.git
& $git fetch origin
```

4) 如需把个人远端恢复到迁移前状态（含 MIT 提交）：

```powershell
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push --force origin backup/tfr-cz-master-mit:master
```

> 注意：反向 transfer 需要目标组织仓库的 admin 权限；如果 `tfr-cz` 在 transfer 后没有 admin，必须由 owner 操作。旧 URL 的重定向在反向 transfer 后可能保留一段时间，属正常。

### 9.3 方案 B 回滚

1) 把组织 `main` 恢复到 scaffold 提交：

```powershell
& $git -c http.sslBackend=schannel -c http.schannelCheckRevoke=false push --force `
    https://github.com/LytharaLab/GFBS-Misc.git backup/org-scaffold:main
```

2) 恢复默认分支/远端：

```powershell
& $git remote set-url origin https://github.com/tfr-cz/GFBS-Misc.git
& $git remote remove personal   # 如果之前把 origin 改名成了 personal
& $git fetch origin
```

3) 如果已经删除了 `backup/scaffold-627cace` 分支，仍可用本地 tag `backup/org-scaffold` 或 sha `627cace...` 推送恢复（需要 Write）。

---

## 10. 未决问题（需 owner / 维护者确认）

1. `Nuclear_rea` 与 `Convex89524` 谁是组织 owner？transfer/邀请/改名由谁执行？
2. 组织是否允许 member 创建仓库（Repository creation 设置）？当前 token 读不到（`orgs/LytharaLab` 的 `members_can_create_*` 均为 `null`）。
3. 组织仓库迁移后是否保留 `GFBS-Misc-scaffold`（建议归档）还是直接删除？
4. 目标默认分支用 `main`（组织约定）还是 `master`（个人仓库沿用）？
5. 项目是否需要保持 public？NTM 资产的分发条款是否已确认？若不确定，迁移后先 private。
6. `HANDOVER.md` 是否纳入版本库（目前未跟踪）？其中包含本机路径/GitHub 状态，公开仓库需评估是否合适。
7. `pickaxe.json` 的 tag 修复是否作为一个独立提交进入迁移历史？
8. `libs/gfbs_main_api_stub.jar` 保留在仓库还是改为构建时生成/本地放置？

---

## 11. 附：本次核查使用的只读命令清单

```powershell
E:\ghcli\gh.exe --version
E:\ghcli\gh.exe auth status
E:\ghcli\gh.exe api repos/tfr-cz/GFBS-Misc --jq '{private,default_branch,permissions}'
E:\ghcli\gh.exe repo view tfr-cz/GFBS-Misc --json isPrivate,visibility,defaultBranchRef
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc --jq '{private,default_branch,permissions}'
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc --jq '{full_name,visibility,default_branch,archived,fork,owner:.owner.login}'
E:\ghcli\gh.exe api -i orgs/LytharaLab/memberships/tfr-cz
E:\ghcli\gh.exe api user/orgs
E:\ghcli\gh.exe api user/memberships/orgs/LytharaLab
E:\ghcli\gh.exe api orgs/LytharaLab/members --jq '.[].login'
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/contents --jq '.[] | {name,type,size,sha}'
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/commits --jq ...
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/branches --jq ...
E:\ghcli\gh.exe api repos/LytharaLab/GFBS-Misc/branches/main/protection
E:\ghcli\gh.exe api 'repos/tfr-cz/GFBS-Misc/compare/d891b11...master' --jq ...
E:\ghcli\gh.exe api 'repos/tfr-cz/GFBS-Misc/commits?per_page=5'

C:\Users\Administrator\.qoder\bin\git\cmd\git.exe status
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe log --oneline -3
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe branch -vv
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe remote -v
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe ls-remote origin
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe remote show origin
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe ls-files
C:\Users\Administrator\.qoder\bin\git\cmd\git.exe check-ignore -v .mcdev/... run/ build/... libs/gfbs_gltf-1.5.1.jar
```

以上命令全部为读操作；本次未执行任何 `git push`、`git commit`、`gh api -X POST/PATCH/DELETE`。
