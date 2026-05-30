package com.witcombat.manager.domain

data class CombatState(
    var turn: Int = 0,
    var nowIndex: Int = 0,
    var turnOrder: MutableList<String> = mutableListOf(), // unit_id 列表
    val initiativeMode: String = "traditional", // "team" | "traditional" | "manual"
    val initiativeRolls: MutableMap<String, Int> = mutableMapOf(),
    var active: Boolean = false,
    var firstTeam: String? = null,
    var pendingReorder: Boolean = false // 迅捷/迟缓 在下一轮重新排序的标记
) {
    val currentUnitId: String?
        get() = turnOrder.getOrNull(nowIndex)
}
