# 基线来源与重建记录（BASELINE）

本工程**不是**从源码仓库克隆来的，而是从一份已构建的 jar 反编译重建出来的。后续开发以此工程为准。

## 来源

| 项 | 值 |
| --- | --- |
| 源 jar | `G:\1\1\.minecraft\versions\量子科学\mods\gfbs-misc-0.1.0.jar` |
| SHA-256 | `3be7e7bb563646317c53f9b50adc6d109b1652d249e1208a2a803b9435de31fd` |
| 大小 / 条目 | 3,349,114 B / 302 条目（50 个 class，252 个资源） |
| mod 版本 | `gfbs_misc` 0.1.0，Forge 47.4.21，MC 1.20.1，official mappings |
| 与旧工程的关系 | 与 `../GFBS-Misc`（HEAD `11f6cc5`）**不是同一条线**：本线删除了 `cargo_elevator` 与整个 `cabin_elevator`，新增 `door_frame_tone`（同色系门框）与 `client/gui` 门调节器 UI |

## 重建流程

1. **反编译**：Vineflower 1.10.1（`java -jar vineflower.jar -dgs=0 -ind="    " <jar> <out>`）→ 41 个 `.java`。
   jar 是 Forge 生产格式：MC **类名是官方名、成员是 SRG**（`m_/_f_`）。直接反编译会得到 `m_60913_` 这类标识符。
2. **SRG → 官方成员名**：用 ForgeGradle 缓存里的
   `~/.gradle/caches/forge_gradle/minecraft_user_repo/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/srg_to_official_1.20.1.tsrg`
   （TSRG2）建立 `m_*`/`f_*` → 官方名 的字典（34,980 方法 + 32,368 字段，1:1 无冲突），对反编译源码做词边界替换。
   没有用 ForgeAutoRenamingTool：其 bundled joptsimple 的本地化资源缺失，`--help`/正常参数都会抛 `MissingResourceException`。
3. **泛型修复**：反编译丢失了类型实参（`DeferredRegister`、`RegistryObject`、`Set`/`List`/`Map`、`Iterable`、
   `BlockEntityRenderer<T>` 等），共 52 处声明按上下文补回；`DoorSpec` 里那段
   `Arrays.stream((Object[]) ...)` 改为 `Arrays.stream(b.extraDimensions).map(int[]::clone)`。
4. **资源**：直接从 jar 解出（JSON/PNG 无损），共 246 个资源文件 + `pack.mcmeta`。
5. **构建脚手架**：从 `../GFBS-Misc` 复制 `build.gradle`、`settings.gradle`、`gradle.properties`、wrapper、
   `libs/`（`gfbs_main_api_stub.jar`）与 `META-INF/mods.toml` 模板；`build.gradle` 里的 local-maven 指向
   `G:/qoder/work/gfbs/local-maven`（提供 `gfbs_gltf:1.5.1`）。

## 重建验证

| 检查 | 结果 |
| --- | --- |
| `gradlew build --offline` | 通过 |
| class 集合 vs 源 jar | 50 / 50，完全一致（无缺、无多） |
| 资源集合 vs 源 jar | 252 / 252，完全一致 |
| 往返反编译对比 | 新 jar 再反编译 → 与原 jar 反编译结果仅 5 个文件有差异，全部是编译器/反编译器噪声（多余的强转、`var14`→`var141` 之类的局部名），**无逻辑差异** |
| `python tools/compat_check.py` | 全绿（24 个门 id、26 条配方、26 条 pickaxe 条目、单通道单 `@Mod`） |

## 已经丢失/不能还原的东西

- 原作者的**注释与 Javadoc 全部丢失**（当前源码里的注释是我们补写的少量说明）。
- 部分**局部变量名**（`var7`/`var12` 这类）与匿名类结构可能与原始写法不同；语义等价，但读起来不如手写源码。
- 原始工程里的文档（HANDOVER/介绍文档/README）、`tools/` 之外的脚本、`.git` 历史都不在 jar 里，无法还原。

## 重建脚本（可复查）

| 文件 | 作用 |
| --- | --- |
| `F:\DSH\work\decompiler\vineflower.jar` | 反编译器（1.10.1，从 Maven Central 取） |
| `F:\DSH\work\srg_official_dict.json` | 由 TSRG2 生成的替换字典 |
| `F:\DSH\work\quantum_src` | 原始反编译产物（SRG 名） |
| `F:\DSH\work\quantum_src_official` | 替换成官方成员名后的产物 |
| `F:\DSH\work\quantum_baseline` | 从 jar 解出的资源 |
| `F:\DSH\work\patch_generics.py` | 52 处泛型补丁脚本（幂等，可重复执行） |
