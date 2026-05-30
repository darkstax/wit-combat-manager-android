package com.witcombat.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickImportDialog(
    onDismiss: () -> Unit,
    onImport: (name: String, text: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速导入角色", fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("角色名称:", fontSize = 11.sp)
                AppTextField(
                    value = name, onValueChange = { name = it },
                    Modifier.fillMaxWidth().height(44.dp),
                    fontSizeSp = 12
                )
                Text("粘贴骰娘导出文本:", fontSize = 11.sp)
                AppTextField(
                    value = text, onValueChange = { text = it },
                    Modifier.fillMaxWidth().height(120.dp),
                    fontSizeSp = 11
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(name, text) }, enabled = text.isNotBlank()) {
                Text("导入", fontSize = 12.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 12.sp) } }
    )
}
