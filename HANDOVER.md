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
- 防护服 4 件：`hazmat_helmet` / `hazmat_chest` / `hazmat_legs` / `hazmat_boots`（见下节）。
- 中英语言键各 94 条。

## 防护服（HBM 风格四件套）

按 HBM 动力装甲的做法实现，映射到 1.20.1 的护甲渲染管线：

| HBM（1.7.10） | 本工程（1.20.1） |
| --- | --- |
| 每个套装一个 `ModelArmorXxx extends ModelBiped`，按槽位 `type` 画不同部件 | `HazmatArmorModel extends HumanoidModel`，四个槽位各一个 `LayerDefinition`（`helmet/chest/legs/boots`） |
| `setRotationAngles` 里 `copyPropertiesFromBiped` 从玩家模型偷姿势 | Forge 的 `IClientItemExtensions#getGenericArmorModel` 自动 `copyProperties`（姿势 + 部件可见性），我们只返回自己的模型 |
| 每槽一套贴图（helmet/chest/arm/leg） | `HazmatArmorItem#getArmorTexture` 每槽返回 `textures/armor/hazmat_<piece>.png` |
| OBJ 命名组 + 逐组 bindTexture | `CubeListBuilder.addBox` 的盒子几何 + 256×256 贴图的八个纯色 64×64 色区（primary/shadow/trim/dark/accent/glass/metal/lamp） |
| 高亮层二次全亮渲染 | 暂未做（头盔灯的盒子留在 `lamp` 色区，后续可加 `RenderType.eyes` 覆盖层） |

实现要点：

- 一件护甲 = 一个 `ArmorItem`（`HazmatArmorItem`）+ 一套材质（`HazmatArmorMaterial`：护甲 2/7/5/2、耐久基数 20、韧性 0.5、皮革修）。
- 模型只声明**七个标准部件名**（head/hat/body/right_arm/left_arm/right_leg/left_leg），未用到的留空；槽位填哪些盒子见 `HazmatArmorModel`。
- 细节靠**叠壳**而不是一个大盒子：底壳定轮廓 → 略凸出的加强筋/包边 → 五金件（带扣、铆钉、滤毒罐、气瓶阀门）压在最外面，小件 0.3–1.5 单位。当前共 115 个盒子（头 33 / 胸 42 / 腿 20 / 靴 20）。
- 模型实例按槽位缓存（`HazmatArmorModels`），共享给所有玩家——因为每次渲染 Forge 都会把玩家姿势+可见性重新拷进来。
- 贴图坐标（256×256，八色区）：`(0,0)=primary`、`(64,0)=shadow`、`(128,0)=trim`、`(192,0)=dark`、`(0,64)=accent`、`(64,64)=glass`、`(128,64)=metal`、`(192,64)=lamp`；盒子 UV 展开尺寸（`2*(w+d) × (h+d)`）必须落在 64×64 色区内，目前最大 47.8×19.1（领圈 / 胸壳）。色区现在画成**旧化贴片**（叠层 value noise 做污渍、漂白、流痕、划痕，金属加锈），由 `F:\DSH\work\gen_hazmat_textures.py` 按固定种子生成；盒子从 texOffs 起采样、六个面落在 unwrap 不同子区，所以噪点会自然变成“每个面脏得不一样”。**不能**放进色区的是位置性图案（接缝、logo、边缘磨损）——它们会按不可预测的偏移落到某个面上。
- 预览：`build/reports/hazmat-preview.png`（由 `F:\DSH\work\preview_hazmat.py` 直接解析模型源码生成的正交示意图，只用于比例自查，不是实机渲染）。
- 尺寸：整体比早期版本收过一圈（`F:\DSH\work\shrink_hazmat.py` 按部位、按锚点缩放：头/胸 XZ 0.92、Y 0.96；袖臂 XZ 0.90；裤/靴 XZ 0.94；靴子 Y 不动，否则鞋底会离地）。
- 结合部设计（避免可避免的穿模）：**没有护颈环**。躯干上沿抬到 y -1.34，兜帽下沿收在 -1.26——同色相交 0.08，头转动时是兜帽在肩上滑动，看起来是布料压在肩部，而不是一道环。兜帽下缘原本是**空壳**（只有侧墙和背墙）。现在头部恢复显示（见下），头本身（±4）就填在兜帽内腔（±4.23）里，所以内衬去掉了；兜帽墙延伸到 y 0.35（颈根），后墙与侧墙**斜接**（x ±4.23），避免同向共面闪烁。脊柱背板从 y 0.96 抬到 -1.34，颈根到气瓶之间不再有凹口。
- **玻璃透明 + 露出脸**：护甲层用的是 `RenderType.armorCutoutNoCull`（alpha 测试，做不出半透明），所以头盔贴图里 `glass` 色区的 alpha 置 0（整块面被 alpha 测试丢弃），由 `HazmatVisorLayer` 用单独的 `textures/armor/hazmat_visor.png`（同 256×256 布局，只有 glass 格画了 ALPHA≈116 的青色玻璃 + 边缘倒角 + 斜向高光）以 `RenderType.entityTranslucent` 再画一次。图层通过 `EntityRenderersEvent.AddLayers` 追加到 default / slim 两个玩家渲染器上，所以在护甲层之后、按半透明排序。
- **头部不隐藏**：`HazmatBodyReplacement` 只隐藏 `hat`（第二层被膨胀 0.5，比兜帽内腔宽，会从兜帽里穿出来），基础头部保留，脸从玻璃透出来。因为头部可见，`skinlayers3d` 的 3D 头发（也是按外层贴图膨胀生成）会穿出兜帽，所以戴头盔时用 `ThirdPartySkinLayers` 把它的网格清空；脱下时 `setCurrentSkin(null)` 让它重建。
- 结合部设计（续）：肩部袖筒拆成**肩段**（x 4.9 宽、z ±2.7，全在躯干前后表面之内）和**小臂段**（全尺寸，深度与肩段同为 ±2.72）；大腿顶端的髋段只到 y 0.7（世界 12.7，正好在腰带下沿 12.8 之下），不再伸进腰带里 2 格。
- 手套比袖筒内缩 0.08（x/z 都缩）——它原来和袖筒共用同一组侧面，两层共面就是手腕处那种一闪一闪的来源。
- 共面检查：`tools/audit_model.py`（= `F:\DSH\workudit_hazmat.py`）报同一个 mesh 里**同向**共面的面对（背面剔除会吃掉反向的那一类，所以只查同向）；`tools/fix_model_overlaps.py`（= `F:\DSH\workix_overlaps.py`）把撞车盒子里体积较小的那个面内移 0.06 直到清零；两个脚本都可以在仓库根直接跑。改完几何必须跑这两个，`problems: 0` 才提交。
- **HBM 路线：护甲即本体**（`HazmatBodyReplacement`，原来叫 `HazmatSkinLayers`）。穿上某件就隐藏玩家模型对应部位：头盔→`head`+`hat`；胸甲→`body`+左右臂+`jacket`+左右袖；护腿→左右腿+左右裤。**只隐藏、从不置回 true**，恢复靠每帧 `setModelProperties` 自己重设。靴子不隐藏任何部件（只穿靴子时隐藏整条腿会让靴子悬空）。
- 为什么隐藏本体不会把套装一起隐藏：Forge 的护甲渲染是 `getParentModel().copyPropertiesTo(护甲层的双足模型)` → `setPartVisibility(slot)` → `getArmorModelHook()` → `copyModelProperties(护甲层双足模型, 我们的模型)`。可见性来源是 `setPartVisibility`，**不是**玩家模型的部件，所以玩家本体隐藏与套装无关。
- 副作用（都接受，因为本线不再考虑与其他东西共存）：皮肤双层、`skinlayers3d` 的 3D 皮肤自动关闭（它的网格以基础部件可见为前提）；披风/鞘翅仍会盖在套装外面（1.20.1 里 cape/ear 由各自的 layer 和模型部件渲染，不在 `PlayerModel` 上）；僵尸等非玩家生物穿这套仍会与本体互穿（本逻辑只挂在 `RenderPlayerEvent`）。
- 第一人称（`HazmatFirstPersonArm`）：挂 `RenderArmEvent`，穿胸甲时取消原版并渲染我们自己的手臂子树（袖+手套+腕环+肩甲），姿态用 `setupAnim(player,0,0,0,0,0)` + 手臂姿态归零复刻原版第一人称；否则屏幕角落会露出一只裸手。

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
3. 文档缺口：README / 介绍文档 / 旧 HANDOVER 里的货梯、客梯章节都不适用本线，需要另写。
4. 本工程尚未做版本管理初始化（`git init` 由接手者执行；`.gitignore` 已就位）。
5. 反编译产物里保留了部分 `(Type)obj` 强转和 `varN` 局部名；重构时顺手清理，但不要为了"好看"改动逻辑。
6. 防护服目前是**纯几何粗模**：颜色靠贴图象限纯色，没有高光/磨损细节；头盔灯没有自发光层；只有玩家能显示（其他生物没接渲染层）。
