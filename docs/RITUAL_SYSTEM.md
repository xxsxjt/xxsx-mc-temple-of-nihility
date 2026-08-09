# 归零祭坛数据驱动体系

> 目标版本：Minecraft 26.2 / NeoForge 26.2.0.7-beta  
> 当前范围：七条归零材料仪式；四系催化、持续阶段和风险分支尚未实现

## 玩家闭环

玩家按《虚无圣典》搭建祭坛，将祭材放入左槽。界面会用服务端同步的同一判定结果显示产物、材料数量、最低结构等级、虚无能量成本和具体阻塞原因。状态为可执行时点击“仪式”，服务端再次校验并一次性扣除能量与材料，然后写入输出槽并播放反馈。

阻塞优先级固定为：无祭材 -> 无配方 -> 数量不足 -> 结构不足 -> 输出堵塞 -> 能量不足。界面、聊天反馈和服务端执行共用 `RitualExecutionPolicy`，避免出现界面说能做而服务端拒绝的两套规则。

## 数据定义

仪式位于 `src/main/resources/data/templenihility/recipe/ritual/`，类型为 `templenihility:nihility_ritual`。新增基础仪式不需要修改祭坛 Java 代码。

```json
{
  "type": "templenihility:nihility_ritual",
  "input": "templenihility:nihility_shard",
  "input_count": 4,
  "result": {
    "id": "templenihility:nihility_crystal",
    "count": 1
  },
  "min_tier": 1,
  "energy_cost": 3
}
```

字段约束：

- `input`：物品或标签 Ingredient。
- `input_count`：1 到 99。
- `result`：非空物品堆，数量不能超过物品自身堆叠上限。
- `min_tier`：0 到 3；0 允许未成形祭坛执行最初级归零。
- `energy_cost`：非负整数。

构建任务 `verifyRitualDefinitions` 会阻止空定义集、错误类型、越界数值、重复“输入 + 等级”身份以及祭坛重新引入硬编码配方。

## 当前仪式

| 祭材 | 数量 | 产物 | 数量 | 最低等级 | 能量 |
|---|---:|---|---:|---:|---:|
| 归尘 | 8 | 虚无残片 | 1 | 0 | 0 |
| 虚无残片 | 4 | 虚无结晶 | 1 | 1 | 3 |
| 虚无结晶 | 4 | 虚无符文 | 1 | 1 | 6 |
| 虚无符文 | 4 | 圣殿印记 | 1 | 2 | 10 |
| 圣殿印记 | 4 | 虚无核心 | 1 | 2 | 16 |
| 虚无核心 | 4 | 虚无共鸣核心 | 1 | 3 | 24 |
| 虚无遗物残片 | 8 | 圣殿印记 | 1 | 3 | 20 |

## 权威状态与同步

- `ModRecipes` 注册配方类型和序列化器。
- `NihilityRitualService` 从当前 `ServerLevel` 的 `RecipeManager` 查询定义并生成执行计划。
- `NihilityAltarBlockEntity` 只负责结构扫描和原子执行，不保存配方副本。
- `NihilityAltarMenu` 用隐藏预览槽和数据槽同步服务端判定，不让客户端自行猜配方。
- `OnDatapackSyncEvent.sendRecipes(...)` 同步该自定义配方类型；客户端在 `RecipesReceivedEvent` 中更新只读列表。
- 《虚无圣典》的仪式页和 JEI 仪式分类读取同步列表，因此数据包调整数量、等级和成本后不需要再手改两份说明。

## 验证边界

自动验证覆盖七种阻塞/成功优先级、七份数据定义、资源和双语键、Java 编译及 jar 内容。未启动客户端，也未声称已验证数据包热重载、JEI 实际显示、菜单点击、祭坛成功/失败特效或旧世界行为。

## 后续扩展

四系仪式不能只在当前定义上增加颜色字段。下一版格式需要先设计并迁移：系别、催化物、持续时间、执行阶段、中断回滚、可预览风险和系别专属结果。扩展时必须保留旧七条配方的兼容解码或提供明确迁移。
