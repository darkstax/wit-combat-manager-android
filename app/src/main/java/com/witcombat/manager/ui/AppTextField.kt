package com.witcombat.manager.ui

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false,
    fontSizeSp: Int = 11,
    singleLine: Boolean = true
) {
    AndroidView(
        factory = { ctx ->
            EditText(ctx).apply {
                setTextColor(android.graphics.Color.BLACK)
                setBackgroundColor(android.graphics.Color.WHITE)
                inputType = if (isNumber) {
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                } else {
                    InputType.TYPE_CLASS_TEXT
                }
                setSingleLine(singleLine)
                setPadding(12, 4, 12, 4)
                textSize = fontSizeSp.toFloat()
                setSelectAllOnFocus(true)
                setText(value)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        onValueChange(s?.toString() ?: "")
                    }
                })
            }
        },
        update = { editText ->
            // update from outside only when not focused, to avoid cursor jumps
            if (!editText.isFocused) {
                val current = editText.text.toString()
                if (current != value) {
                    editText.setText(value)
                }
            }
        },
        modifier = modifier
    )
}
