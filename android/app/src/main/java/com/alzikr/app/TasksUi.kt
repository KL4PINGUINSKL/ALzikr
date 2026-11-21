package com.alzikr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen() {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("المهام", "العادات", "التركيز", "الختمة", "الملاحظات").forEachIndexed { i, t -> Choice(t, Nav.daily == i) { Nav.daily = i } }
        }
        when (Nav.daily) {
            1 -> HabitsList()
            2 -> FocusScreen()
            3 -> KhatmaPage()
            4 -> NotesList()
            else -> TaskList()
        }
    }
}

@Composable
private fun TaskList() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var text by rememberSaveable { mutableStateOf("") }
    var after by rememberSaveable { mutableStateOf<String?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val all = Store.tasks()
    val open = all.filter { !it.done }
    val done = all.filter { it.done }
    fun add() { if (text.isNotBlank()) { Store.addTask(text.trim(), after?.let(Prayer::valueOf)); text = "" } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("tasks", "✅", "اكتب المهمة ثم اضغط «إضافة». اربطها بصلاة (مثل «بعد العصر») ليذكّرك بها إشعار الأذان.") }
        item {
            Panel(pad = 12.dp) {
                OutlinedTextField(text, { text = it }, placeholder = { Text("مهمة جديدة…") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { add() }))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Choice("بلا وقت", after == null) { after = null }
                    Prayer.entries.filter { it.alertable }.forEach { p -> Choice("بعد ${p.ar}", after == p.name) { after = p.name } }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { Action("إضافة") { add() } }
            }
        }
        if (open.isEmpty()) item { Sub("لا مهام مفتوحة. أضف مهمة، واربطها بصلاة لتذكّرك بها عند الأذان.", Modifier.padding(8.dp)) }
        items(open, key = { it.id }) { TaskRow(it) }
        if (done.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${if (showDone) "▾" else "◂"} المنجزة (${done.size})", color = P.muted, fontSize = 14.sp, modifier = Modifier.weight(1f).clip(L.shape(8.dp)).clickable { showDone = !showDone }.padding(6.dp))
                if (showDone) Text("حذف المنجزة", color = P.danger, fontSize = 13.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Store.clearDone() }.padding(6.dp))
            }
        }
        if (showDone) items(done, key = { it.id }) { TaskRow(it) }
    }
}

@Composable
private fun TaskRow(t: Store.Task) {
    Row(Modifier.fillMaxWidth().clip(L.shape(14.dp)).background(P.surface).border(1.dp, P.line, L.shape(14.dp))
        .clickable { Store.toggleTask(t.id) }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(L.shape(6.dp)).background(if (t.done) P.accent else P.surface).border(2.dp, if (t.done) P.accent else P.muted, L.shape(6.dp)),
            contentAlignment = Alignment.Center) { if (t.done) Text("✓", color = P.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.text, color = if (t.done) P.muted else P.ink, fontSize = 15.sp, textDecoration = if (t.done) TextDecoration.LineThrough else null)
            t.after?.let { Sub("بعد ${it.ar}", size = 12.sp, color = P.gold) }
        }
        Text("✕", color = P.muted, fontSize = 15.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { Store.deleteTask(t.id) }.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun NotesList() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var editing by remember { mutableStateOf<Store.Note?>(null) }
    var adding by remember { mutableStateOf(false) }
    val notes = Store.notes()
    val fmt = remember { SimpleDateFormat("d MMM · h:mm a", Locale("ar")) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Hint("notes", "📝", "اضغط «ملاحظة جديدة» لتكتب، واضغط على أي ملاحظة لتعديلها أو حذفها.") }
        item { Action("+ ملاحظة جديدة", Modifier.fillMaxWidth(), filled = false) { adding = true } }
        if (notes.isEmpty()) item { Sub("اكتب ما يخطر ببالك: فائدة، آية، فكرة…", Modifier.padding(8.dp)) }
        items(notes, key = { it.id }) { n ->
            Panel(Modifier.clickable { editing = n }, pad = 14.dp) {
                Text(n.text, color = P.ink, fontSize = 15.sp, maxLines = 6, lineHeight = 22.sp)
                Sub(fmt.format(Date(n.at)), Modifier.padding(top = 6.dp), size = 11.sp)
            }
        }
    }
    if (adding || editing != null) {
        var text by remember(editing) { mutableStateOf(editing?.text ?: "") }
        val close = { adding = false; editing = null }
        AlertDialog(onDismissRequest = close, containerColor = P.surface,
            title = { Text(if (editing == null) "ملاحظة جديدة" else "تعديل الملاحظة", color = P.ink) },
            text = { OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 5) },
            confirmButton = { TextButton({ if (text.isNotBlank()) Store.saveNote(editing?.id, text.trim()); close() }) { Text("حفظ") } },
            dismissButton = {
                Row {
                    editing?.let { n -> TextButton({ Store.deleteNote(n.id); close() }) { Text("حذف", color = P.danger) } }
                    TextButton(close) { Text("إلغاء") }
                }
            })
    }
}
