# GFBS : Misc（门线）开发交接

本工程是 **只保留门** 的 GFBS : Misc 线：24 种门 + 2 种装饰门框，没有货梯（`cargo_elevator`）、也没有客梯（`cabin_elevator`）。
源码由一份已构建 jar 反编译重建而来，来源与验证见 [BASELINE.md](BASELINE.md)。

## 快速开始

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot"
cd G:\qoder\work\gfbs\GFBS-Misc-doors
.\gradlew.bat build --offline --console=plain   # 产物 build\libs\gfbs-misc-0.1.0.jar
python tools\compat_check.py                    # 提交前的注册名/命名空间/挖掘 tag 回归检查
```

- 依赖：`gfbs_gltf:1.5.1`（`build.gradle` 里 local-maven 指向 `G:/qoder/work/gfbs/local-maven`）、
  `libs/gfbs_main_api_stub.jar`（`compileOnly`，给 `GfbsMainGateFollow` 编译用；运行期可选）。
- Gradle 8.8 wrapper、Java 17、Forge 47.4.21、MC 1.20.1、official mappings。
- 客户端实例：`G:\1\1\.minecraft\versions\量子科学\mods\gfbs-misc-0.1.0.jar`（本线基线就是从这来的）。

## 模块地图

| 位置 | 职责 |
| --- | --- |
| `misc/GFBSMisc` | 唯一 `@Mod`；注册入口 |
| `misc/network/MiscNetwork` | 唯一网络通道 `gfbs_misc:main`，`PROTOCOL = "2"`（改包格式必须 +1） |
| `content/DoorSpec(s)` | 门的静态规格：尺寸、部件名、皮肤、音效、时间、硬度；`large(base, dw, dh)` 生成大号门 |
| `content/DoorGeometry` | 多方块占位、亚格碰撞（含大号门的缩放反算） |
| `content/block/DoorBlock` | 方块行为：摆放/破坏、右键、邻居更新、掉落 |
| `content/block/DoorBlockEntity` | 门的状态机（0 关 / 3 开中 / 1 开 / 2 关中）、红石模式、皮肤、防爆门高度、音效调度、`DoorCoreRegistry` |
| `content/DoorConfigService` | 门调节器的服务端：`open`/`save`/`toggle`，创造或 OP + 8 格距离校验；快照位置用**玩家点的那个方块** |
| `content/DoorRedstoneMode` | `NONE / PULSE / TRIGGER / INVERTED / GFBS_NORMALLY_CLOSED / GFBS_NORMALLY_OPEN` |
| `client/DoorRenderer` | 逐门型的渲染与开合动画（vault/fire/blast/airlock/containment/water/seal/silo/vehicle/cargo/reactor/transition…） |
| `client/DoorVisual(+Cache)` | 每方块实体一份 glTF 实例组（按部件拆分），皮肤材质变体 |
| `client/DoorItemRenderer` | 物品图标渲染（`builtin/entity`），`Fit` 负责把模型缩放进槽位 |
| `client/DoorConfigScreen` | 门调节器界面（见下节） |
| `client/gui/{Palette,Draw,ModeButton}` | 新 UI 组件：调色板、绘制助手、模式按钮 |
| `client/TransitionSealAnimation` | `transition_seal` 的逐帧节点矩阵动画（`.anim` 资源） |
| `client/DoorClientSoundHooks` | 循环音效句柄管理（开/关中循环、停止） |
| `client/DoorModelAssets` / `ClientReloadListener` | `.obj`（经 glTF API 加载）模型缓存与资源重载 |
| `compat/GfbsMainGateFollow` | 装了 GFBS : Main 时，GFBS 常开/常闭门跟随 `/GFBsMain gate` |

## 内容清单

- 门 24 种：16 种原尺寸 + 8 种大号（`vault / large_vehicle / fire / sliding_blast / round_airlock / qe_containment / water / secure_access` 各带 `_large`）。
- 装饰方块 2 种：`door_frame`（门框）、`door_frame_tone`（同色系门框）。
- 中英语言键各 90 条。

## 门调节器（新 UI）

- 布局与分组：`DoorConfigScreen` 把内容分成「选项」和「GFBS 强制模式」两组（`gui.gfbs_doors.config.group.options` / `.group.forced`），
  底部显示门状态「可控 / 锁定」（`gui.gfbs_doors.state.controllable` / `.locked`）。
- 选项：6 种红石模式、手动控制开关、防爆闸门（`blast_door`）专属高度行、`强制切换状态`、`关闭`。
- 交互：`save`/`toggle` 都要创造模式或 OP，且距**快照位置**（= 玩家点击的方块）不超过 8 格；服务端按部件反查核心。
- 快照：`DoorConfigSnapshot(pos, doorId, mode, manual, extraHeight, elevator)`；`elevator` 字段在本线已无意义（没有货梯），
  属于从旧合并线残留的字段，改协议时可一并删除。
- 界面在目标方块消失（被拆/换维度）后会在下一 tick 自动关闭。

## 约定

- 注册名、命名空间、挖掘 tag、配方结果都必须过 `tools/compat_check.py`；基线在 `tools/compat_baseline.json`，
  改动后用 `python tools/compat_check.py --write-baseline` 刷新（只有全部硬检查通过才会写）。
- 不要自建第二个网络通道；新包注册进 `MiscNetwork.register()`，客户端类保持 `DistExecutor` 隔离。
- 新增门走 `DoorSpec.builder(...)` + `DoorSpecs.ALL`，同时补 blockstate / `models/block` / `models/item`（`builtin/entity`）/ 配方 / 语言键 / `mineable/pickaxe`。
- 大号门用 `DoorSpecs.large(base, dw, dh)`，不要复制渲染代码。

## 已知问题 / 待办

1. **注释与一部分局部变量名在反编译中丢失**（见 BASELINE.md）；语义等价，但可读性弱于手写源码。
2. `DoorConfigSnapshot.elevator` 是死字段（本线无货梯）。
3. 文档缺口：README / 介绍文档 / 旧 HANDOVER 里的货梯、客梯章节都不适用于本线，需要另写。
4. 本工程尚未做版本管理初始化（`git init` 由接手者执行；`.gitignore` 已就位）。
5. 反编译产物里保留了部分 `(Type)obj` 强转和 `varN` 局部名；重构时顺手清理，但不要为了"好看"改动逻辑。
