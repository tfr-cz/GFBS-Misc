# GFBS : Misc (doors-only line)

Minecraft **Forge 1.20.1** mod（`modid = gfbs_misc`）：NTM 风格的可配置门，外加一套按 HBM 手法移植的 T-51b 动力装甲。

这是从 `量子科学` 实例里那份 jar 反编译重建出来的**门类线**（没有货梯/客梯），在此之上继续开发护甲与渲染。

## 内容

- **门**：24 种门 + `door_frame` / `door_frame_tone` 两种门框，全部 glTF/OBJ 模型，带开关动画、红石模式、手动控制、防爆闸门高度等可配置项（游戏内用 `door_configurator` 右键门打开调节界面）。
- **潜行防护服**：四件套，128–256 像素手绘盒子模型 + 旧化贴图，玻璃视窗为真半透明（glTF 渲染）。
- **T-51b 动力装甲**：从 NTM 导入的 OBJ 网格，走 glTF 渲染管线；含移动速度、踩踏/起跳/落地音、10 格硬着陆冲击波、伤害类型百分比防护。

## 构建

需要 JDK 17。

```bash
./gradlew build --offline        # 产出 build/libs/gfbs-misc-0.1.0.jar
./gradlew test --offline         # 模型资源解析 + 抗性公式单测
python tools/compat_check.py     # 资源/命名空间/注册一致性自检
```

### 依赖

- `gfbs_gltf:1.5.1`（glTF 渲染库，本模组只编译期依赖）默认从**本机**的
  `file:///G:/qoder/work/gfbs/local-maven` 解析。换机器时用
  `-Pgfbs.localMaven=file:///你的/路径` 覆盖这个仓库地址。
- `libs/gfbs_main_api_stub.jar` 是 GFBS : Main 命令 API 的编译期桩，`gfbs_main` 本身为可选依赖。

## 文档

- [`HANDOVER.md`](HANDOVER.md) —— 模块地图、内容清单、渲染/尺寸约定、已知问题
- [`BASELINE.md`](BASELINE.md) —— 这一线从 jar 反编译重建的来源、校验与丢失信息
- [`tools/`](tools) —— `compat_check.py`（资源自检）、`import_hbm_power_armor.py`（从 NTM jar 导入 T-51b）、`audit_model.py` / `fix_model_overlaps.py`（盒子模型共面/穿模检查）

## 第三方素材

`src/main/resources/assets/gfbs_doors/models/armor/power/` 与 `sounds/armor/` 下的 OBJ/贴图/音效提取自
**HBM's Nuclear Tech Mod**（`HBM-NTM-.1.0.27_X5778_H261`），版权归其作者。本仓库仅作个人开发用途，
如需公开分发请先确认 NTM 的授权条款。
