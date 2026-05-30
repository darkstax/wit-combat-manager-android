package com.witcombat.manager.domain

import kotlin.random.Random

// ============================================================
// 先攻系统
// ============================================================

fun teamInitiative(players: List<GameUnit>, monsters: List<GameUnit>): CombatState {
    val state = CombatState(initiativeMode = "team", active = true)

    fun teamScore(units: List<GameUnit>): Int {
        if (units.isEmpty()) return 0
        val speeds = units.map { it.speed }
        return if (speeds.size >= 2) speeds.max() + speeds.min() else speeds.first() * 2
    }

    val playerScore = teamScore(players)
    val monsterScore = teamScore(monsters)

    val playersSorted = players.sortedByDescending { it.speed }
    val monstersSorted = monsters.sortedByDescending { it.speed }

    if (playerScore >= monsterScore) {
        state.firstTeam = "player"
        state.turnOrder.addAll(playersSorted.map { it.unitId } + monstersSorted.map { it.unitId })
    } else {
        state.firstTeam = "monster"
        state.turnOrder.addAll(monstersSorted.map { it.unitId } + playersSorted.map { it.unitId })
    }

    return state
}

fun manualInitiative(firstTeam: String, players: List<GameUnit>, monsters: List<GameUnit>): CombatState {
    val state = CombatState(initiativeMode = "manual", active = true)
    state.firstTeam = firstTeam

    val playersSorted = players.sortedByDescending { it.speed }
    val monstersSorted = monsters.sortedByDescending { it.speed }

    if (firstTeam == "player") {
        state.turnOrder.addAll(playersSorted.map { it.unitId } + monstersSorted.map { it.unitId })
    } else {
        state.turnOrder.addAll(monstersSorted.map { it.unitId } + playersSorted.map { it.unitId })
    }

    return state
}

fun traditionalInitiative(units: List<GameUnit>, diceFaces: Int = 20): CombatState {
    val state = CombatState(initiativeMode = "traditional", active = true)
    val rolls = mutableMapOf<String, Int>()

    fun rollUnit(u: GameUnit): Int = Random.nextInt(1, diceFaces + 1) + u.speed

    for (u in units) {
        rolls[u.unitId] = rollUnit(u)
    }

    resolveTies(units, rolls, ::rollUnit)

    state.initiativeRolls.putAll(rolls)
    val sortedUnits = units.sortedWith(
        compareByDescending<GameUnit> { rolls[it.unitId] ?: 0 }
            .thenByDescending { it.speed }
    )
    state.turnOrder.addAll(sortedUnits.map { it.unitId })

    return state
}

private fun resolveTies(
    units: List<GameUnit>,
    rolls: MutableMap<String, Int>,
    rollFunc: (GameUnit) -> Int,
    maxAttempts: Int = 10
) {
    val unitMap = units.associateBy { it.unitId }

    repeat(maxAttempts) {
        val byRoll = rolls.entries.groupBy({ it.value }, { it.key })
        val ties = byRoll.filter { it.value.size > 1 }
        if (ties.isEmpty()) return

        for ((_, tiedIds) in ties) {
            val speeds = tiedIds.mapNotNull { unitMap[it]?.speed }
            if (speeds.toSet().size == speeds.size) continue

            val speedGroups = tiedIds.groupBy { unitMap[it]?.speed ?: 0 }
            for ((_, group) in speedGroups) {
                if (group.size > 1) {
                    for (uid in group) {
                        unitMap[uid]?.let { rolls[uid] = rollFunc(it) }
                    }
                }
            }
        }
    }
}

// ============================================================
// 伤害系统
// ============================================================

fun applyDamage(unit: GameUnit, amount: Int, dmgType: String = "物理", isAttack: Boolean = true, attacker: GameUnit? = null): String {
    if (amount <= 0) return "${unit.name} 未受到伤害"

    if (dmgType == "真实") return applyTrueDamage(unit, amount)

    val shield = unit.getStatus("护盾")
    if (shield != null && shield.stacks > 0) {
        shield.stacks -= 1
        if (shield.stacks <= 0) unit.removeStatus("护盾")
        return "${unit.name} 的护盾抵消了本次攻击（剩余${shield.stacks}次）"
    }

    val resist = if (dmgType == "物理") unit.physicalResist else unit.magicResist
    var finalDmg = maxOf(0, amount - resist)

    val dmgBoost = unit.getStatus("伤害强化")
    if (dmgBoost != null && dmgBoost.stacks > 0) {
        finalDmg += dmgBoost.stacks
    }

    val vuln = unit.getStatus("脆弱")
    if (vuln != null && vuln.stacks > 0) {
        finalDmg += vuln.stacks
    }

    val barrier = unit.getStatus("屏障")
    if (unit.tempHp > 0) {
        val absorbed = minOf(finalDmg, unit.tempHp)
        unit.tempHp -= absorbed
        finalDmg -= absorbed
        if (barrier != null && barrier.stacks > 0) {
            barrier.stacks -= 1
            if (barrier.stacks <= 0) {
                unit.removeStatus("屏障")
                unit.tempHp = 0
            }
        }
    }

    unit.currentHp = maxOf(0, unit.currentHp - finalDmg)

    var result = "${unit.name} 受到 ${finalDmg} 点${dmgType}伤害（HP: ${unit.currentHp}/${unit.maxHp}"
    if (unit.tempHp > 0) result += ", 临时HP: ${unit.tempHp}"
    result += "）"

    if (isAttack && unit.hasStatus("睡眠")) {
        unit.removeStatus("睡眠")
        result += "\n${unit.name} 的「睡眠」因受到攻击而解除"
    }

    if (isAttack && attacker != null) {
        val result2 = processEndAttack(attacker)
        if (result2.isNotEmpty()) result += "\n" + result2
    }

    return result
}

private fun applyTrueDamage(unit: GameUnit, amount: Int): String {
    var remaining = amount
    if (unit.tempHp > 0) {
        val absorbed = minOf(remaining, unit.tempHp)
        unit.tempHp -= absorbed
        remaining -= absorbed
        val barrier = unit.getStatus("屏障")
        if (barrier != null && barrier.stacks > 0) {
            barrier.stacks -= 1
            if (barrier.stacks <= 0) {
                unit.removeStatus("屏障")
                unit.tempHp = 0
            }
        }
    }

    unit.currentHp = maxOf(0, unit.currentHp - remaining)
    return "${unit.name} 受到 ${remaining} 点真实伤害（HP: ${unit.currentHp}/${unit.maxHp}）"
}

fun applyHealing(unit: GameUnit, amount: Int): String {
    if (unit.hasStatus("禁疗")) {
        return "${unit.name} 受到「禁疗」影响，治疗失效"
    }

    if (unit.hasStatus("亲和")) {
        unit.removeStatus("亲和")
    }

    val oldHp = unit.currentHp
    unit.currentHp = minOf(unit.maxHp, unit.currentHp + amount)
    val healed = unit.currentHp - oldHp

    val result = "${unit.name} 恢复了 ${healed} 点生命（HP: ${unit.currentHp}/${unit.maxHp}）"

    processEndHealEffect(unit)

    return result
}

// ============================================================
// 元素损伤系统
// ============================================================

fun applyElementalDamage(unit: GameUnit, amount: Int, elemType: String): String {
    if (unit.isInBurst()) {
        val trueDmg = amount * 3
        val msg = applyTrueDamage(unit, trueDmg)
        return "[爆发期间] ${unit.name} 的元素损伤转为 ${trueDmg} 点真实伤害\n$msg"
    }

    var remaining = amount
    val result = StringBuilder()

    val elemBarrier = unit.getStatus("元素屏障")
    if (elemBarrier != null && elemBarrier.stacks > 0) {
        val absorbed = minOf(remaining, elemBarrier.stacks)
        elemBarrier.stacks -= absorbed
        remaining -= absorbed
        result.append("${unit.name} 的元素屏障吸收了 ${absorbed} 点${elemType}")
        if (elemBarrier.stacks <= 0) {
            unit.removeStatus("元素屏障")
            result.append("（元素屏障耗尽）")
        }
        if (remaining <= 0) return result.toString()
    }

    val overflow = unit.reduceTenacity(remaining)
    result.append("\n${unit.name} 受到 ${remaining} 点${elemType}（韧性: ${unit.elementalTenacityCurrent}/${unit.elementalTenacityMax}）")

    if (unit.elementalTenacityCurrent <= 0) {
        val burstMsgs = triggerElementalBurst(unit, elemType)
        result.append("\n").append(burstMsgs)
    }

    return result.toString().trim()
}

fun triggerElementalBurst(unit: GameUnit, elemType: String): String {
    val burstDef = ELEMENTAL_BURST_EFFECTS[elemType]
        ?: return "未知元素类型: $elemType"

    unit.elementalBurst = elemType
    unit.elementalBurstRemaining = 1

    val result = StringBuilder()
    result.append("!!! ${unit.name} 触发了${elemType}爆发 !!!\n")
    result.append("  造成了${elemType}爆发，请额外输入造成的伤害\n")

    for (statusName in burstDef.statuses) {
        result.append("  ${applyStatus(unit, statusName)}\n")
    }

    if (burstDef.extra.isNotEmpty()) {
        result.append("  [${burstDef.extra}]\n")
    }

    return result.toString()
}

fun recoverBurst(unit: GameUnit): List<String> {
    val msgs = mutableListOf<String>()
    if (unit.isInBurst()) {
        msgs.add("${unit.name} 的「${unit.elementalBurst}爆发」结束")
    }
    unit.recoverTenacity()
    if (msgs.isNotEmpty()) {
        msgs.add("${unit.name} 的元素韧性恢复至 ${unit.elementalTenacityMax}")
    }
    return msgs
}

// ============================================================
// 状态系统
// ============================================================

fun applyStatus(unit: GameUnit, statusName: String, stacks: Int = 0): String {
    if (unit.hasStatus("免疫")) {
        return "${unit.name} 的「免疫」阻挡了「${statusName}」"
    }

    val resist = unit.getStatus("抵抗")
    if (resist != null && resist.stacks > 0) {
        resist.stacks -= 1
        if (resist.stacks <= 0) unit.removeStatus("抵抗")
        return "${unit.name} 消耗一次「抵抗」无效了「${statusName}」（剩余${resist.stacks}次）"
    }

    if (statusName == "标记") return applyMark(unit)

    if (statusName in STATUS_UPGRADE) {
        val upgraded = STATUS_UPGRADE[statusName]!!
        if (unit.hasStatus(upgraded)) {
            return "${unit.name} 已有「${upgraded}」，「${statusName}」不叠加"
        }
        if (unit.hasStatus(statusName)) {
            unit.removeStatus(statusName)
            while (unit.hasStatus(statusName)) unit.removeStatus(statusName)
            unit.addStatus(upgraded)
            return "${unit.name} 的「${statusName}」升级为「${upgraded}」！"
        } else {
            unit.addStatus(statusName)
            return "${unit.name} 获得了「${statusName}」"
        }
    }

    if (statusName in X_STATUSES && stacks > 0) {
        val existing = unit.getStatus(statusName)
        if (existing != null) {
            existing.stacks += stacks
            return "${unit.name} 的「${statusName}」层数 +${stacks} → 当前 ${existing.stacks} 层"
        } else {
            unit.addStatus(statusName, stacks)
            return "${unit.name} 获得了「${statusName}${stacks}」(${stacks}层)"
        }
    } else if (statusName in X_STATUSES) {
        val existing = unit.getStatus(statusName)
        if (existing != null) {
            existing.stacks += 1
            return "${unit.name} 的「${statusName}」层数 +1 → 当前 ${existing.stacks} 层"
        } else {
            unit.addStatus(statusName, 1)
            return "${unit.name} 获得了「${statusName}1」(1层)"
        }
    }

    if (unit.hasStatus(statusName)) {
        return "${unit.name} 已有「${statusName}」，不重复添加"
    }

    unit.addStatus(statusName, stacks)
    val stacksText = if (stacks > 0) stacks.toString() else ""
    return "${unit.name} 获得了「${statusName}${stacksText}」"
}

private fun applyMark(unit: GameUnit): String {
    val messages = mutableListOf<String>()
    unit.addStatus("标记")
    messages.add("${unit.name} 获得了「标记」（同时视为停顿/震颤/寒冷/困倦）")

    for (sub in listOf("停顿", "寒冷", "困倦")) {
        if (sub in STATUS_UPGRADE) {
            val upgraded = STATUS_UPGRADE[sub]!!
            if (unit.hasStatus(upgraded)) continue
            if (unit.hasStatus(sub)) {
                unit.removeStatus(sub)
                while (unit.hasStatus(sub)) unit.removeStatus(sub)
                unit.addStatus(upgraded)
                messages.add("  「标记」触发：${sub} → ${upgraded}")
            }
        }
    }

    return messages.joinToString("\n")
}

fun processEndOfTurn(unit: GameUnit): List<String> {
    val removed = mutableListOf<String>()
    for (statusName in END_OF_TURN_STATUSES) {
        if (unit.hasStatus(statusName)) {
            unit.removeStatus(statusName)
            removed.add(statusName)
        }
    }

    val msgs = mutableListOf<String>()
    if (removed.isNotEmpty()) {
        msgs.add("${unit.name} 回合结束清除: ${removed.joinToString("、")}")
    }

    if ("失能" in removed) {
        unit.addStatus("失能后效")
        msgs.add("${unit.name} 获得了「失能后效」")
    }

    return msgs
}

fun processEndAttack(unit: GameUnit): String {
    val removed = END_OF_ATTACK_BUFFS.filter { unit.hasStatus(it) }
    for (name in removed) unit.removeStatus(name)
    return if (removed.isNotEmpty()) "${unit.name} 攻击后清除了: ${removed.joinToString("、")}" else ""
}

fun processEndHealEffect(unit: GameUnit): String {
    val removed = END_OF_HEAL_EFFECT_DEBUFFS.filter { unit.hasStatus(it) }
    for (name in removed) unit.removeStatus(name)
    return if (removed.isNotEmpty()) "${unit.name} 治疗后清除了: ${removed.joinToString("、")}" else ""
}

fun clearAllStatuses(unit: GameUnit): List<String> {
    val removed = unit.statusNames()
    unit.statusEffects.clear()
    return removed
}

fun endTurnCleanup(units: List<GameUnit>): List<String> =
    units.flatMap { processEndOfTurn(it) }

fun processRoundStart(units: List<GameUnit>): List<String> =
    units.flatMap { recoverBurst(it) }

// ============================================================
// 回合管理
// ============================================================

fun advanceTurn(state: CombatState, allUnits: List<GameUnit>): Pair<CombatState, List<String>> {
    state.turn += 1
    state.nowIndex = 0

    val msgs = processRoundStart(allUnits)
    val msgs2 = endTurnCleanup(allUnits)

    val allMsgs = (msgs + msgs2).toMutableList()
    allMsgs.add("--- 第 ${state.turn} 回合开始 ---")

    applySpeedReorder(state, allUnits)
    return state to allMsgs
}

private fun applySpeedReorder(state: CombatState, units: List<GameUnit>) {
    val unitMap = units.associateBy { it.unitId }

    val swifts = state.turnOrder.filter { unitMap[it]?.hasStatus("迅捷") == true }
    val slows = state.turnOrder.filter { unitMap[it]?.hasStatus("迟缓") == true }

    if (swifts.isEmpty() && slows.isEmpty()) return

    for (uid in swifts) {
        state.turnOrder.remove(uid)
        unitMap[uid]?.removeStatus("迅捷")
    }
    for (uid in slows) {
        state.turnOrder.remove(uid)
        unitMap[uid]?.removeStatus("迟缓")
    }

    state.turnOrder = (swifts + state.turnOrder + slows).toMutableList()
}

fun nextActor(state: CombatState, allUnits: List<GameUnit>): Pair<CombatState, List<String>> {
    val messages = mutableListOf<String>()
    state.nowIndex += 1

    if (state.nowIndex >= state.turnOrder.size) {
        val (_, msgs) = advanceTurn(state, allUnits)
        messages.addAll(msgs)
    }
    return state to messages
}
