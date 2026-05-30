package com.witcombat.manager.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witcombat.manager.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val filteredUnits = remember(state.units, state.currentFilter) { vm.getFilteredUnits() }

    var editDialog by remember { mutableStateOf<GameUnit?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        var fabExpanded by remember { mutableStateOf(false) }
        var gmNotes by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            // --- 左侧面板 (38%) ---
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("全部" to "全部", "player" to "玩家", "monster" to "怪物").forEach { (filter, label) ->
                        FilterChip(
                            selected = state.currentFilter == filter,
                            onClick = { vm.setFilter(filter) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                    Text("类型", Modifier.weight(0.15f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("名称", Modifier.weight(0.35f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("HP", Modifier.weight(0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("#", Modifier.weight(0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredUnits, key = { it.unitId }) { unit ->
                        val isSelected = state.targetUnit?.unitId == unit.unitId
                        val bgColor = if (isSelected) Color(0x4D3399FF) else Color.Transparent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(bgColor)
                                .clickable { vm.selectUnit(unit) }
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (unit.unitType == "player") "玩家" else "怪物", Modifier.weight(0.15f), fontSize = 11.sp)
                            Text(unit.name, Modifier.weight(0.35f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${unit.currentHp}/${unit.maxHp}", Modifier.weight(0.25f), fontSize = 11.sp)
                            Text((filteredUnits.indexOf(unit) + 1).toString(), Modifier.weight(0.25f), fontSize = 10.sp)
                        }
                    }
                }

                val detail = buildDetailText(state.targetUnit)
                Text(
                    detail,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 160.dp)
                        .background(Color(0xFFE6E6E6)).padding(4.dp),
                    fontSize = 10.sp, lineHeight = 16.sp
                )

                Text(
                    "目标: ${state.targetUnit?.name ?: "未选择"}",
                    color = Color(0xFFCC3333), fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // --- 右侧面板 (62%) ---
            Column(
                modifier = Modifier.weight(0.62f).fillMaxHeight().padding(4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 先攻模式
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E6F2))) {
                    Column(Modifier.padding(6.dp)) {
                        Text("先攻模式", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth().height(28.dp)) {
                            listOf("traditional" to "传统", "team" to "团队", "manual" to "客观判断").forEach { (mode, label) ->
                                FilterChip(selected = state.initMode == mode,
                                    onClick = { vm.setInitMode(mode) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("骰子:", fontSize = 10.sp)
                            AppTextField(
                                value = state.diceFaces.toString(),
                                onValueChange = { vm.setDiceFaces(it.toIntOrNull() ?: 20) },
                                Modifier.width(50.dp).height(28.dp),
                                isNumber = true
                            )
                            Text("先动:", fontSize = 10.sp)
                            if (state.initMode == "manual") {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(if (state.manualFirstTeam == "player") "玩家" else "怪物", fontSize = 10.sp)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("玩家") }, onClick = { vm.setManualFirstTeam("player"); expanded = false })
                                        DropdownMenuItem(text = { Text("怪物") }, onClick = { vm.setManualFirstTeam("monster"); expanded = false })
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val curUnit = state.combatState?.currentUnitId?.let { uid -> state.units.find { it.unitId == uid } }
                Text(
                    if (state.combatState?.active == true) "Turn: ${state.combatState!!.turn}  Now: ${curUnit?.name ?: "--"}"
                    else "Turn: --  Now: --",
                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
                if (state.teamScoreText.isNotEmpty()) Text(state.teamScoreText, fontSize = 9.sp, color = Color.Gray)

                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Button(
                        onClick = { vm.startCombat() }, enabled = state.combatState?.active != true,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)
                    ) { Text("开始战斗", fontSize = 10.sp) }
                    Button(
                        onClick = { vm.nextAction() }, enabled = state.combatState?.active == true,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)
                    ) { Text("下一行动", fontSize = 10.sp) }
                    Button(
                        onClick = { vm.endTurn() }, enabled = state.combatState?.active == true,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp)
                    ) { Text("结束回合", fontSize = 10.sp) }
                    Button(
                        onClick = { vm.endCombat() }, enabled = state.combatState?.active == true,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("结束战斗", fontSize = 10.sp) }
                }

                Spacer(Modifier.height(4.dp))

                // 伤害/治疗
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))) {
                    Column(Modifier.padding(6.dp)) {
                        Text("伤害 / 治疗", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("数值:", fontSize = 10.sp)
                            AppTextField(
                                value = state.dmgAmount.toString(),
                                onValueChange = { vm.setDmgAmount(it.toIntOrNull() ?: 0) },
                                Modifier.width(52.dp).height(28.dp),
                                isNumber = true
                            )
                            Text("类型:", fontSize = 10.sp)
                            var dmgExpanded by remember { mutableStateOf(false) }
                            Box(Modifier.weight(0.22f).height(28.dp)
                                .background(Color(0xFFEEEEEE), MaterialTheme.shapes.small)
                                .clickable { dmgExpanded = true }.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart) {
                                Text(state.dmgType, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                DropdownMenu(expanded = dmgExpanded, onDismissRequest = { dmgExpanded = false }) {
                                    listOf("物理", "法术", "真实", "治疗").forEach { d ->
                                        DropdownMenuItem(text = { Text(d, fontSize = 12.sp) }, onClick = { vm.setDmgType(d); dmgExpanded = false })
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = state.isAttack, onCheckedChange = { vm.setIsAttack(it) }, modifier = Modifier.size(20.dp))
                                Text("攻击", fontSize = 9.sp)
                            }
                            Button(onClick = { vm.applyDamage() }, contentPadding = PaddingValues(4.dp)) { Text("施加", fontSize = 10.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 元素损伤
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF0))) {
                    Column(Modifier.padding(6.dp)) {
                        Text("元素损伤", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("数值:", fontSize = 10.sp)
                            AppTextField(
                                value = state.elemAmount.toString(),
                                onValueChange = { vm.setElemAmount(it.toIntOrNull() ?: 0) },
                                Modifier.width(52.dp).height(28.dp),
                                isNumber = true
                            )
                            Text("类型:", fontSize = 10.sp)
                            var elemExpanded by remember { mutableStateOf(false) }
                            Box(Modifier.weight(0.3f).height(28.dp)
                                .background(Color(0xFFEEEEEE), MaterialTheme.shapes.small)
                                .clickable { elemExpanded = true }.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart) {
                                Text(state.elemType, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                DropdownMenu(expanded = elemExpanded, onDismissRequest = { elemExpanded = false }) {
                                    ELEMENT_TYPES.forEach { e ->
                                        DropdownMenuItem(text = { Text(e, fontSize = 12.sp) }, onClick = { vm.setElemType(e); elemExpanded = false })
                                    }
                                }
                            }
                            Button(onClick = { vm.applyElemDmg() }, contentPadding = PaddingValues(4.dp)) { Text("施加", fontSize = 10.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 状态操作
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0FF))) {
                    Column(Modifier.padding(6.dp)) {
                        Text("状态操作", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("状态:", fontSize = 10.sp)
                            var statusExpanded by remember { mutableStateOf(false) }
                            Box(Modifier.weight(0.4f).height(28.dp)
                                .background(Color(0xFFEEEEEE), MaterialTheme.shapes.small)
                                .clickable { statusExpanded = true }.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart) {
                                Text(state.selectedStatus, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                    ALL_STATUS_NAMES.forEach { s ->
                                        DropdownMenuItem(text = { Text(s, fontSize = 12.sp) }, onClick = { vm.setSelectedStatus(s); statusExpanded = false })
                                    }
                                }
                            }
                            if (state.selectedStatus in X_STATUSES) {
                                Text("X:", fontSize = 10.sp)
                                AppTextField(
                                    value = state.statusStacks.toString(),
                                    onValueChange = { vm.setStatusStacks(it.toIntOrNull() ?: 0) },
                                    Modifier.width(45.dp).height(28.dp),
                                    isNumber = true
                                )
                            }
                            Button(onClick = { vm.applyStatusOp() }, Modifier.weight(0.15f), contentPadding = PaddingValues(2.dp)) { Text("施加", fontSize = 10.sp) }
                            Button(onClick = { vm.clearAllStatus() }, Modifier.weight(0.2f), contentPadding = PaddingValues(2.dp)) { Text("清除全部", fontSize = 10.sp) }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // GM 笔记
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .background(Color(0xFFFFFFDD))
                        .padding(2.dp)
                ) {
                    Text("GM 笔记", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666))
                    AppTextField(
                        value = gmNotes,
                        onValueChange = { gmNotes = it },
                        modifier = Modifier.fillMaxSize(),
                        fontSizeSp = 10
                    )
                }
            }
        }

        // --- 底部：战斗日志 + 行动顺序 ---
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp)
        ) {
            // 左：战斗日志
            Text(
                state.logLines.joinToString("\n"),
                modifier = Modifier.weight(0.65f).fillMaxHeight()
                    .background(Color(0xFFF2F2F2))
                    .verticalScroll(rememberScrollState()).padding(4.dp),
                fontSize = 9.sp, lineHeight = 14.sp
            )
            // 右：行动顺序
            Column(
                modifier = Modifier.weight(0.35f).fillMaxHeight()
                    .background(Color(0xFFF0F0FF))
                    .verticalScroll(rememberScrollState()).padding(4.dp)
            ) {
                Text("行动顺序", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                if (state.combatState != null && state.combatState!!.turnOrder.isNotEmpty()) {
                    state.combatState!!.turnOrder.forEachIndexed { i, uid ->
                        val unit = state.units.find { it.unitId == uid } ?: return@forEachIndexed
                        val roll = state.combatState!!.initiativeRolls[uid]
                        val rollText = if (roll != null) " (检定: $roll)" else ""
                        val isNow = i == state.combatState!!.nowIndex
                        val bg = if (isNow) Color(0x333399FF) else Color.Transparent
                        Text(
                            "${i + 1}. ${unit.name}",
                            modifier = Modifier.fillMaxWidth().background(bg).padding(vertical = 1.dp, horizontal = 2.dp),
                            fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text("暂无行动顺序", fontSize = 9.sp, color = Color.Gray)
                }
            }
            } // end bottom Row
        } // end inner Column

        // --- FAB 菜单 (浮动) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // 菜单项 — 列在 FAB 上方
            if (fabExpanded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FabMenuItem("添加玩家") { editDialog = GameUnit(unitType = "player"); fabExpanded = false }
                    FabMenuItem("添加怪物") { editDialog = GameUnit(unitType = "monster"); fabExpanded = false }
                    FabMenuItem("编辑") {
                        state.targetUnit?.let { editDialog = it }
                        fabExpanded = false
                    }
                    FabMenuItem("删除", isDestructive = true) {
                        state.targetUnit?.let { vm.deleteUnit(it) }
                        fabExpanded = false
                    }
                    FabMenuItem("导入xlsx") { fabExpanded = false }
                    FabMenuItem("快速导入文本") { showAddMenu = true; fabExpanded = false }
                }
            }

            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(if (fabExpanded) "X" else "+", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    } // end Box

    if (editDialog != null) {
        UnitEditDialog(
            unit = editDialog!!,
            onDismiss = { editDialog = null },
            onConfirm = { unit ->
                if (state.units.none { it.unitId == unit.unitId }) vm.addUnit(unit)
                else vm.updateUnit(unit)
                editDialog = null
            }
        )
    }

    if (showAddMenu) {
        QuickImportDialog(
            onDismiss = { showAddMenu = false },
            onImport = { name, text ->
                try {
                    val unit = parseQuickImport(text, name)
                    vm.addUnit(unit)
                    vm.log("快速导入成功: ${unit.name}")
                } catch (e: Exception) {
                    vm.log("导入失败: ${e.message}")
                }
                showAddMenu = false
            }
        )
    }
}

@Composable
private fun FabMenuItem(label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val bgColor = if (isDestructive) Color(0xFFFFCDD2) else Color(0xFFE3F2FD)
    Surface(
        onClick = onClick,
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        shadowElevation = 4.dp
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 11.sp, color = Color.Black)
    }
}

private fun buildDetailText(unit: GameUnit?): String {
    if (unit == null) return "请选择一个单位"
    val typeLabel = if (unit.unitType == "player") "玩家" else "怪物"
    val eliteLabels = mapOf(0 to "精零", 1 to "精一", 2 to "精二")
    val statusText = if (unit.statusEffects.isNotEmpty()) {
        unit.statusEffects.joinToString("、") { s -> if (s.stacks > 0) "${s.name}${s.stacks}" else s.name }
    } else "无"
    val burstInfo = if (unit.isInBurst()) "${unit.elementalBurst}（剩余${unit.elementalBurstRemaining}回合）" else "无"

    return buildString {
        appendLine("名称: ${unit.name}  [$typeLabel]  ${eliteLabels[unit.eliteStage] ?: ""}")
        appendLine("ID: ${unit.unitId}")
        appendLine("血量: ${unit.currentHp}/${unit.maxHp}  临时HP: ${unit.tempHp}")
        appendLine("速度: ${unit.speed}  重量: ${unit.weight}")
        appendLine("物抗: ${unit.physicalResist}  法抗: ${unit.magicResist}  护甲: ${unit.armorType}")
        appendLine("元素韧性: ${unit.elementalTenacityCurrent}/${unit.elementalTenacityMax}")
        appendLine("当前爆发: $burstInfo")
        appendLine("状态: $statusText")
    }
}

private val QUICK_IMPORT_FIELDS = listOf(
    Triple("生命值上限", "max_hp", 10),
    Triple("物理抗性", "physical_resist", 0),
    Triple("法术抗性", "magic_resist", 0),
    Triple("元素韧性", "elemental_tenacity_current", 6),
    Triple("速度", "speed", 10),
    Triple("重量等级", "weight", 0)
)

private fun parseQuickImport(text: String, name: String): GameUnit {
    val extracted = mutableMapOf<String, Int>()
    for ((fieldName, key, default) in QUICK_IMPORT_FIELDS) {
        val match = Regex("$fieldName(\\d+)").find(text)
        extracted[key] = match?.groupValues?.get(1)?.toIntOrNull() ?: default
    }
    val maxHp = extracted["max_hp"] ?: 10
    return GameUnit(
        name = name.ifBlank { "导入角色" },
        unitType = "player",
        currentHp = maxHp,
        maxHp = maxHp,
        physicalResist = extracted["physical_resist"] ?: 0,
        magicResist = extracted["magic_resist"] ?: 0,
        speed = extracted["speed"] ?: 10,
        weight = extracted["weight"] ?: 0,
        elementalTenacityCurrent = extracted["elemental_tenacity_current"] ?: 6,
        elementalTenacityMax = extracted["elemental_tenacity_current"] ?: 6,
        eliteStage = 0
    )
}
