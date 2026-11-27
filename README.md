# 虚无圣殿（Temple of Nihility）

**Temple of Nihility** 是一款面向 **Minecraft 26.1.2 / NeoForge 26.1.2.124**（主分支）并同步提供 **Minecraft 26.2 / NeoForge 26.2.0.93** 并行构建的内容模组，围绕「虚无 / 虚空 / 混沌 / 深渊」四系构筑打造，包含三阶自然生成圣殿、五阶归尘生物与圣者请战首领战、38 件 32×32 八帧动态像素装备与饰品、无刷屏虚无能量 HUD、归零祭坛仪式、虚空宝库多方块仓储网络与传输核心。

## 版本与分支

| 分支 | Minecraft | Loader | Loader 版本 | Mod 版本 | 发布产物 |
|---|---|---|---|---|---|
| `main` / `release/neoforge-26.1.2`（主用稳定版） | `26.1.2` | NeoForge | `26.1.2.124+` | `1.0.0-beta.5` | `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.5.jar` |
| `release/neoforge-26.2`（26.2 并行版） | `26.2` | NeoForge | `26.2.0.93+` | `1.0.0-beta.5` | `temple-of-nihility-neoforge-26.2-1.0.0-beta.5.jar` |

> 下载时请从 [GitHub Releases](https://github.com/xxsxjt/xxsx-mc-temple-of-nihility/releases) 选择与你的 Minecraft 版本一致的 JAR（`Latest` 默认指向 **NeoForge 26.1.2** 稳定版）。

## 核心内容

| 模块 | 内容说明 |
|---|---|
| **四系装备与动态美术** | 7 件虚无兵器/工具、4 件虚无护甲、27 件四系 Curios 饰品（共 38 件）均采用 32×32 八帧动态像素贴图（零半透明脏边），含第一/第三人称挥动连招与校准握持 |
| **专属虚无能量 HUD** | 锚定快捷栏右侧（自动避让原版攻击指示器），0-tick 实时显示能量、低能警示、4 件套反击/混沌过载倒计时、混沌手甲 1–5 连击豆与魂盾；高频闪避/背刺/饰品触发原地聚合为微提示（`相位闪避 ×N`）+ 2px 屏边微闪 + 音效节流，彻底告别聊天与 ActionBar 刷屏 |
| **四系武器主动技** | 普攻均回复 +4 虚无能量：归零之刃（存在削减/蓄力归零斩）、断界巨刃（霸体三段断界式）、归墟匕首（1.45× 背刺/右键缺位步穿墙引爆）、虚无战斧（右键破界碎甲） |
| **2 件/4 件虚无护甲** | 2 件套每秒 +2 能量、加速负面状态衰减、上限 +60；4 件套上限 +140、每秒自动修复虚无装备、25% 相位闪避（返还 +12 能量并激活 4 秒 1.55× 破甲归零反击） |
| **三阶圣殿与首领战** | 自然生成三阶虚无圣殿、五阶归尘生物（均含刷怪蛋）、大令使机制与圣者请战首领战（含 Boss 血条、动态边界、抹除线、缺失区域与虚无终式） |
| **归零祭坛与仪式** | 0–III 级多方块祭坛仪式，支持归尘 → 残片 → 结晶 → 符文 → 封印 → 核心 → 共鸣核心逐级进阶，已接入 JEI 仪式分类 |
| **虚空宝库与传输** | 多方块联网仓储（单块 243 格，支持升级、搜索、排序、3×3 合成、所有权保护与 NeoForge 独立区块常加载）；支持便携虚空终端（默认 `O` 键）与物品/流体/能量传输核心 |
| **游戏内指南** | 书 + 空寂石/残片/归尘 任意合成《虚无圣典》，内置中英双语完整玩法、祭坛结构与联动说明 |

## 依赖与联动

### NeoForge 26.1.2（主用稳定版）
- **Java**: `25`
- **Minecraft**: `26.1.2`
- **NeoForge**: `26.1.2.124+`
- **必需**: Curios API `15.0.0+26.1.2`
- **可选**: JEI `29.53.0.112+`、Waystones `26.1.2.16+`

### NeoForge 26.2（并行版）
- **Java**: `25`
- **Minecraft**: `26.2`
- **NeoForge**: `26.2.0.93+`
- **必需**: Curios API `15.0.0+` / `16.0.0+26.2`
- **可选**: JEI `30.7.0.40+`、Waystones `26.2.0.3+`

## 本地构建

需要 **JDK 25**（Gradle 9.2.1 + ModDevGradle 2.0.148）：

```bash
./gradlew clean test build --no-daemon
```

构建产物输出于 `build/libs/`：
- `main` / `release/neoforge-26.1.2`：`build/libs/temple-of-nihility-neoforge-26.1.2-1.0.0-beta.5.jar`
- `release/neoforge-26.2`：`build/libs/temple-of-nihility-neoforge-26.2-1.0.0-beta.5.jar`

## 开源协议（License）

Temple of Nihility 采用 **GNU Affero General Public License v3.0 only (`AGPL-3.0-only`)** 开源协议发布（除非单个文件或第三方组件另有说明）。
完整协议文本见根目录 [LICENSE](LICENSE)。
