package com.witcombat.manager.domain

// 升级链：低级 → 高级
val STATUS_UPGRADE = mapOf(
    "麻痹" to "眩晕",
    "寒冷" to "冻结",
    "困倦" to "睡眠",
    "停顿" to "束缚"
)

// 标记视为这四个状态（用于升级判断）
val MARK_SYNONYMS = listOf("停顿", "震颤", "寒冷", "困倦")

// 回合结束一次
val END_OF_TURN_STATUSES = setOf(
    "脆弱", "失能", "失能后效",
    "麻痹", "眩晕",
    "寒冷", "冻结",
    "困倦",
    "沉默", "战栗",
    "束缚",
    "目盲",
    "睡眠"
)

// 攻击一次
val END_OF_ATTACK_BUFFS = setOf(
    "伤害强化", "精准", "暴击", "穿透", "隐匿"
)

// X为0时
val COUNTER_BUFFS = setOf(
    "护盾", "屏障", "抵抗", "元素屏障"
)

// 受到一次治疗
val END_OF_HEAL_BUFFS = setOf("亲和")

// 受到一次治疗效果
val END_OF_HEAL_EFFECT_DEBUFFS = setOf("禁疗")

// 生效一次
val END_OF_ACTIVATION = setOf("迅捷", "迟缓")

// 无公用结束条件
val NO_END_BUFFS = setOf("嘲讽", "被嘲讽", "迷彩", "免疫", "浮空")

// 执行一次移动预备后结束
val END_OF_MOVE_PREP = setOf("停顿")

// 全部可施加的状态
val POSITIVE_BUFFS = listOf(
    "伤害强化", "精准", "魅影", "嘲讽", "被嘲讽",
    "迅捷", "护盾", "屏障", "隐匿", "迷彩",
    "抵抗", "元素屏障", "暴击", "免疫", "穿透", "亲和"
)

val NEGATIVE_BUFFS = listOf(
    "脆弱", "失能", "失能后效", "标记",
    "麻痹", "眩晕",
    "寒冷", "冻结",
    "困倦", "睡眠",
    "停顿", "束缚",
    "失重", "浮空",
    "沉默", "战栗", "禁疗",
    "迟缓", "恐惧", "目盲"
)

val ALL_STATUS_NAMES = POSITIVE_BUFFS + NEGATIVE_BUFFS

// 带X的状态
val X_STATUSES = setOf("伤害强化", "护盾", "屏障", "抵抗", "元素屏障", "脆弱", "失重")

// 元素韧性上限
val ELITE_TENACITY = mapOf(0 to 6, 1 to 9, 2 to 12)

val ELEMENT_TYPES = listOf(
    "凋亡损伤", "组织损伤", "毒性损伤", "侵蚀损伤", "灼燃损伤", "神经损伤"
)

data class BurstEffect(
    val trueDmgMult: Int,
    val extra: String,
    val statuses: List<String>
)

val ELEMENTAL_BURST_EFFECTS = mapOf(
    "凋亡损伤" to BurstEffect(
        trueDmgMult = 2,
        extra = "失去3SP；若无SP可失去，额外造成1次真实伤害",
        statuses = listOf("迟缓")
    ),
    "组织损伤" to BurstEffect(
        trueDmgMult = 3,
        extra = "",
        statuses = listOf("迟缓")
    ),
    "毒性损伤" to BurstEffect(
        trueDmgMult = 2,
        extra = "爆发期间施加[禁疗]",
        statuses = listOf("迟缓", "禁疗")
    ),
    "侵蚀损伤" to BurstEffect(
        trueDmgMult = 2,
        extra = "爆发期间受物理伤害+1辅助骰",
        statuses = listOf("迟缓")
    ),
    "灼燃损伤" to BurstEffect(
        trueDmgMult = 2,
        extra = "爆发期间受法术伤害+1辅助骰",
        statuses = listOf("迟缓")
    ),
    "神经损伤" to BurstEffect(
        trueDmgMult = 1,
        extra = "爆发期间施加[眩晕]",
        statuses = listOf("迟缓", "眩晕")
    )
)
