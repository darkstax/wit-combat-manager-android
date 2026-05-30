package com.witcombat.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witcombat.manager.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitEditDialog(
    unit: GameUnit,
    onDismiss: () -> Unit,
    onConfirm: (GameUnit) -> Unit
) {
    val isNew = unit.name.isEmpty()
    var name by remember { mutableStateOf(unit.name) }
    var unitType by remember { mutableStateOf(unit.unitType) }
    var maxHp by remember { mutableStateOf(unit.maxHp.toString()) }
    var speed by remember { mutableStateOf(unit.speed.toString()) }
    var weight by remember { mutableStateOf(unit.weight.toString()) }
    var physRes by remember { mutableStateOf(unit.physicalResist.toString()) }
    var magicRes by remember { mutableStateOf(unit.magicResist.toString()) }
    var armorType by remember { mutableStateOf(unit.armorType) }
    var eliteStage by remember { mutableStateOf(unit.eliteStage.toString()) }
    var tenacity by remember { mutableStateOf(unit.elementalTenacityCurrent.toString()) }
    var statusChecks by remember {
        mutableStateOf(ALL_STATUS_NAMES.associateWith { unit.hasStatus(it) }.toMutableMap())
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "添加单位" else "编辑单位", fontSize = 14.sp) },
        text = {
            Column(Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                editRow("名称", name, "名称") { name = it }
                spinnerRow("类型", unitType, listOf("player" to "玩家", "monster" to "怪物")) { unitType = it }
                editRow("最大HP", maxHp, "10", KeyboardType.Number) { maxHp = it }
                editRow("速度", speed, "10", KeyboardType.Number) { speed = it }
                editRow("重量", weight, "0", KeyboardType.Number) { weight = it }
                editRow("物抗", physRes, "0", KeyboardType.Number) { physRes = it }
                editRow("法抗", magicRes, "0", KeyboardType.Number) { magicRes = it }
                spinnerRow("护甲", armorType, listOf("轻甲", "中甲", "重甲").map { it to it }) { armorType = it }
                spinnerRow("精英阶段", eliteStage, listOf("0" to "精零", "1" to "精一", "2" to "精二")) { eliteStage = it }
                editRow("元素韧性", tenacity, "6", KeyboardType.Number) { tenacity = it }

                Text("状态效果:", fontSize = 11.sp)
                LazyColumn(Modifier.height(120.dp)) {
                    items(ALL_STATUS_NAMES) { statusName ->
                        Row(
                            Modifier.fillMaxWidth().height(28.dp).clickable {
                                statusChecks = statusChecks.toMutableMap().apply {
                                    put(statusName, !(get(statusName) ?: false))
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = statusChecks[statusName] ?: false,
                                onCheckedChange = { checked ->
                                    statusChecks = statusChecks.toMutableMap().apply { put(statusName, checked) }
                                },
                                modifier = Modifier.size(20.dp))
                            Text(statusName, fontSize = 11.sp)
                        }
                    }
                }

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val stage = eliteStage.toIntOrNull() ?: 0
                    val updated = GameUnit(
                        unitId = unit.unitId,
                        name = name.ifBlank { "未命名" },
                        unitType = unitType,
                        maxHp = maxHp.toIntOrNull() ?: 10,
                        currentHp = if (isNew) (maxHp.toIntOrNull() ?: 10) else unit.currentHp,
                        speed = speed.toIntOrNull() ?: 10,
                        weight = weight.toIntOrNull() ?: 0,
                        physicalResist = physRes.toIntOrNull() ?: 0,
                        magicResist = magicRes.toIntOrNull() ?: 0,
                        armorType = armorType,
                        eliteStage = stage,
                        elementalTenacityCurrent = tenacity.toIntOrNull() ?: (ELITE_TENACITY[stage] ?: 6),
                        elementalTenacityMax = ELITE_TENACITY[stage] ?: 6,
                        statusEffects = mutableListOf()
                    )
                    // If name was blank and this is a new unit, generate a new ID
                    if (isNew) {
                        val newUnit = updated.copy(unitId = java.util.UUID.randomUUID().toString().take(8))
                        statusChecks.forEach { (sn, checked) -> if (checked) newUnit.addStatus(sn, 0) }
                        onConfirm(newUnit)
                    } else {
                        statusChecks.forEach { (sn, checked) -> if (checked) updated.addStatus(sn, 0) }
                        onConfirm(updated)
                    }
                } catch (e: NumberFormatException) {
                    errorMsg = "请输入有效数字"
                }
            }) { Text("确定", fontSize = 12.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 12.sp) } }
    )
}

@Composable
private fun editRow(
    label: String, value: String, placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, Modifier.width(55.dp), fontSize = 11.sp, textAlign = TextAlign.End)
        AppTextField(value = value, onValueChange = onChange, Modifier.weight(1f).height(28.dp),
            isNumber = keyboardType == KeyboardType.Number,
            fontSizeSp = 11)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun spinnerRow(
    label: String, value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.firstOrNull { it.first == value }?.second ?: value

    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, Modifier.width(55.dp), fontSize = 11.sp, textAlign = TextAlign.End)
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) { Text(displayText, fontSize = 11.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(text = { Text(display, fontSize = 12.sp) }, onClick = { onChange(key); expanded = false })
                }
            }
        }
    }
}
