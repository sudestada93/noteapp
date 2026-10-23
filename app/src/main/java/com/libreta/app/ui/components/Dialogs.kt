package com.libreta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.libreta.app.ui.theme.Dims
import com.libreta.app.ui.theme.LocalPalette
import com.libreta.app.ui.theme.T
import java.util.Calendar
import java.util.TimeZone

@Composable
fun BrutalDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "Cancelar",
    danger: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .hardShadow(p.shadow, 6.dp)
                    .clip(BrutalShape)
                    .background(p.surface)
                    .border(Dims.border, p.line, BrutalShape),
            ) {
                Row(
                    Modifier.fillMaxWidth().background(if (danger) p.danger else p.canary).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title.uppercase(), style = T.label, color = Color(0xFF383838))
                }
                Box(Modifier.fillMaxWidth().height(Dims.border).background(p.line))
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), content = content)
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrutalButton(dismissText, onDismiss, style = BtnStyle.Ghost)
                    if (confirmText != null && onConfirm != null) {
                        BrutalButton(confirmText, onConfirm, style = if (danger) BtnStyle.Danger else BtnStyle.Primary, enabled = confirmEnabled)
                    }
                }
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    confirmText: String = "Guardar",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val fr = remember { FocusRequester() }
    BrutalDialog(title, onDismiss, confirmText, { if (text.isNotBlank()) onConfirm(text.trim()) }, confirmEnabled = text.isNotBlank()) {
        BrutalTextField(
            text, { text = it }, placeholder = placeholder, modifier = Modifier.fillMaxWidth().focusRequester(fr),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text.trim()) }),
        )
    }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit, danger: Boolean = true) {
    BrutalDialog(title, onDismiss, confirmText, onConfirm, danger = danger) {
        Text(message, style = T.body, color = LocalPalette.current.ink)
    }
}

@Composable
fun BrutalSheet(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = p.canvas,
        shape = BrutalShape,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 44.dp, height = 5.dp).background(p.ink))
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            if (title != null) {
                Text(title.uppercase(), style = T.label, color = p.ink, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))
            }
            content()
        }
    }
}

/** Fila de menú dentro de una hoja inferior. */
@Composable
fun SheetItem(icon: ImageVector, text: String, subtitle: String? = null, tint: Color = LocalPalette.current.ink, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BrutalShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(BrutalShape).background(p.surface).border(Dims.borderThin, p.line, BrutalShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = T.body, color = tint)
            if (subtitle != null) Text(subtitle, style = T.caption, color = p.inkSoft)
        }
        trailing?.invoke()
    }
}

/** Selector de fecha + hora en dos pasos. Devuelve epoch millis local. */
@Composable
fun DateTimePickerDialog(initial: Long?, onDismiss: () -> Unit, withTime: Boolean = true, onPicked: (Long) -> Unit) {
    val p = LocalPalette.current
    val cal = remember { Calendar.getInstance().apply { timeInMillis = initial ?: (System.currentTimeMillis() + 3_600_000L) } }
    var step by remember { mutableStateOf(0) }
    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis,
    )
    val timeState = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24Hour = true)
    fun finish() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedDateUtc ?: dateState.selectedDateMillis ?: System.currentTimeMillis() }
        val out = Calendar.getInstance().apply {
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), if (withTime) timeState.hour else 9, if (withTime) timeState.minute else 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        onPicked(out.timeInMillis)
    }
    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                BrutalButton(if (withTime) "Siguiente" else "Listo", {
                    pickedDateUtc = dateState.selectedDateMillis
                    if (withTime) step = 1 else finish()
                }, small = true, modifier = Modifier.padding(8.dp))
            },
            dismissButton = { BrutalButton("Cancelar", onDismiss, style = BtnStyle.Ghost) },
            shape = BrutalShape,
            colors = DatePickerDefaults.colors(containerColor = p.surface),
        ) {
            DatePicker(
                dateState,
                colors = DatePickerDefaults.colors(
                    containerColor = p.surface,
                    selectedDayContainerColor = p.primary, selectedDayContentColor = p.onPrimary,
                    todayDateBorderColor = p.ink, todayContentColor = p.ink,
                ),
            )
        }
    } else {
        BrutalDialog("Elegí la hora", onDismiss, "Listo", { finish() }) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    timeState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = p.canvas, selectorColor = p.primary,
                        timeSelectorSelectedContainerColor = p.primary, timeSelectorUnselectedContainerColor = p.canvas,
                        timeSelectorSelectedContentColor = p.onPrimary, timeSelectorUnselectedContentColor = p.ink,
                    ),
                )
            }
        }
    }
}
