package com.witcombat.manager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.witcombat.manager.data.UnitRepository
import com.witcombat.manager.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val units: List<GameUnit> = emptyList(),
    val combatState: CombatState? = null,
    val targetUnit: GameUnit? = null,
    val currentFilter: String = "全部",
    val logLines: List<String> = emptyList(),
    val initMode: String = "traditional",
    val diceFaces: Int = 20,
    val manualFirstTeam: String = "player",
    val teamScoreText: String = "",
    val dmgAmount: Int = 5,
    val dmgType: String = "物理",
    val isAttack: Boolean = true,
    val elemAmount: Int = 2,
    val elemType: String = "凋亡损伤",
    val selectedStatus: String = "伤害强化",
    val statusStacks: Int = 0,
    val updateCounter: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UnitRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repository.loadUnits()
            updateState { it.copy(units = loaded) }
        }
    }

    private inline fun updateState(crossinline transform: (UiState) -> UiState) {
        val old = _uiState.value
        _uiState.value = transform(old).copy(updateCounter = old.updateCounter + 1)
    }

    fun setFilter(filter: String) {
        updateState { it.copy(currentFilter = filter) }
    }

    fun getFilteredUnits(): List<GameUnit> {
        val state = _uiState.value
        return if (state.currentFilter == "全部") state.units
        else state.units.filter { it.unitType == state.currentFilter }
    }

    fun selectUnit(unit: GameUnit) {
        updateState { it.copy(targetUnit = unit) }
    }

    fun addUnit(unit: GameUnit) {
        updateState { state ->
            state.copy(units = state.units + unit, targetUnit = unit)
        }
        save()
    }

    fun updateUnit(unit: GameUnit) {
        updateState { state ->
            state.copy(units = state.units.map { if (it.unitId == unit.unitId) unit else it })
        }
        save()
    }

    fun deleteUnit(unit: GameUnit) {
        updateState { state ->
            val newUnits = state.units.filter { it.unitId != unit.unitId }
            state.copy(units = newUnits, targetUnit = if (state.targetUnit?.unitId == unit.unitId) null else state.targetUnit)
        }
        save()
    }

    fun setDiceFaces(value: Int) { updateState { it.copy(diceFaces = value.coerceIn(1, 999)) } }
    fun setInitMode(mode: String) { updateState { it.copy(initMode = mode) } }
    fun setManualFirstTeam(team: String) { updateState { it.copy(manualFirstTeam = team) } }

    fun startCombat() {
        val state = _uiState.value
        val players = state.units.filter { it.unitType == "player" }
        val monsters = state.units.filter { it.unitType == "monster" }
        val allUnits = players + monsters
        if (allUnits.isEmpty()) { log("请先添加至少一个单位"); return }

        val combatState = when (state.initMode) {
            "team" -> {
                if (players.isEmpty() || monsters.isEmpty()) { log("团队先攻需要至少一个玩家和一个怪物"); return }
                val cs = teamInitiative(players, monsters)
                val pScores = players.map { it.speed }.sorted()
                val mScores = monsters.map { it.speed }.sorted()
                val pTeam = if (pScores.size >= 2) pScores.last() + pScores.first() else (pScores.firstOrNull() ?: 0) * 2
                val mTeam = if (mScores.size >= 2) mScores.last() + mScores.first() else (mScores.firstOrNull() ?: 0) * 2
                updateState { it.copy(teamScoreText = "玩家:$pTeam vs 怪物:$mTeam | ${if (cs.firstTeam == "player") "玩家" else "怪物"}先动") }
                cs
            }
            "manual" -> {
                val cs = manualInitiative(state.manualFirstTeam, players, monsters)
                updateState { it.copy(teamScoreText = "客观判断: ${if (cs.firstTeam == "player") "玩家" else "怪物"}先行") }
                cs
            }
            else -> {
                val cs = traditionalInitiative(allUnits, state.diceFaces)
                val lines = cs.initiativeRolls.entries.sortedByDescending { it.value }.joinToString(" | ") { (uid, roll) ->
                    val unit = state.units.find { it.unitId == uid }
                    "${unit?.name ?: uid}: d${state.diceFaces}+${unit?.speed ?: "?"}=$roll"
                }
                updateState { it.copy(teamScoreText = lines) }
                cs
            }
        }
        updateState { it.copy(combatState = combatState) }
    }

    fun nextAction() {
        val state = _uiState.value
        val cs = state.combatState ?: return
        if (!cs.active) return
        val (newState, messages) = nextActor(cs, state.units)
        messages.forEach { log(it) }
        updateState { it.copy(combatState = newState) }
    }

    fun endTurn() {
        val state = _uiState.value
        val cs = state.combatState ?: return
        val (newState, messages) = advanceTurn(cs, state.units)
        messages.forEach { log(it) }
        updateState { it.copy(combatState = newState) }
    }

    fun endCombat() {
        val state = _uiState.value
        state.combatState?.active = false
        updateState { it.copy(combatState = null, teamScoreText = "") }
        log("--- 战斗结束 ---")
    }

    fun setDmgAmount(value: Int) { updateState { it.copy(dmgAmount = value) } }
    fun setDmgType(value: String) { updateState { it.copy(dmgType = value) } }
    fun setIsAttack(value: Boolean) { updateState { it.copy(isAttack = value) } }

    fun applyDamage() {
        val state = _uiState.value
        val target = state.targetUnit ?: run { log("请先选择目标单位"); return }
        val attacker = if (state.isAttack) {
            val curId = state.combatState?.currentUnitId
            state.units.find { it.unitId == curId }
        } else null
        val msg = if (state.dmgType == "治疗") {
            applyHealing(target, state.dmgAmount)
        } else {
            applyDamage(target, state.dmgAmount, state.dmgType, state.isAttack, attacker)
        }
        log(msg)
        updateState { it.copy(targetUnit = target) }
    }

    fun setElemAmount(value: Int) { updateState { it.copy(elemAmount = value) } }
    fun setElemType(value: String) { updateState { it.copy(elemType = value) } }

    fun applyElemDmg() {
        val state = _uiState.value
        val target = state.targetUnit ?: run { log("请先选择目标单位"); return }
        val msg = applyElementalDamage(target, state.elemAmount, state.elemType)
        log(msg)
        updateState { it.copy(targetUnit = target) }
    }

    fun setSelectedStatus(value: String) { updateState { it.copy(selectedStatus = value) } }
    fun setStatusStacks(value: Int) { updateState { it.copy(statusStacks = value) } }

    fun applyStatusOp() {
        val state = _uiState.value
        val target = state.targetUnit ?: run { log("请先选择目标单位"); return }
        val stacks = if (state.selectedStatus in X_STATUSES) state.statusStacks else 0
        val msg = applyStatus(target, state.selectedStatus, stacks)
        log(msg)
        updateState { it.copy(targetUnit = target) }
    }

    fun clearAllStatus() {
        val state = _uiState.value
        val target = state.targetUnit ?: run { log("请先选择目标单位"); return }
        val removed = clearAllStatuses(target)
        if (removed.isNotEmpty()) {
            log("${target.name} 清除了全部状态: ${removed.joinToString("、")}")
        } else {
            log("${target.name} 无状态可清除")
        }
        updateState { it.copy(targetUnit = target) }
    }

    fun log(msg: String) {
        updateState { state ->
            val newLogs = (state.logLines + msg.split("\n")).takeLast(50)
            state.copy(logLines = newLogs)
        }
    }

    private fun save() {
        viewModelScope.launch {
            repository.saveUnits(_uiState.value.units)
        }
    }
}
