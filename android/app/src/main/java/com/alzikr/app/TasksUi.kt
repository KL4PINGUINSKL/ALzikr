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
        if (!noteEditorOpen) Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            // «اليوم» first (it ties the others together), then the day's parts in the order a day goes.
            TabGrid(listOf(Triple(Daily.TODAY, "☀", "اليوم"), Triple(Daily.TASKS, "✅", "المهام"), Triple(Daily.HABITS, "🔁", "العادات"), Triple(Daily.KHATMA, "📖", "الختمة"),
                Triple(Daily.FOCUS, "🎯", "التركيز"), Triple(Daily.NOTES, "📝", "ملاحظات")).map { (i, ic, t) -> Triple(i.toString(), ic, t) },
                Nav.daily.toString(), cols = 6) { Nav.daily = it.toInt() }
        }
        androidx.compose.runtime.LaunchedEffect(Nav.daily) { when (Nav.daily) { Daily.TASKS -> "tasks"; Daily.FOCUS -> "focus"; Daily.KHATMA -> "khatma"; else -> null }?.let(Usage::hit) }
        when (Nav.daily) {
            Daily.TODAY -> DayOverview()
            1 -> HabitsList()
            2 -> FocusScreen()
            3 -> KhatmaPage()
            4 -> NotesList()
            else -> TaskListPlus()
        }
    }
}

/** True while the note editor fills the screen (the «يومي» tabs hide). */
private var noteEditorOpen by androidx.compose.runtime.mutableStateOf(false)

@Composable
private fun NotesList() {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    var editing by remember { mutableStateOf<Store.Note?>(null) }
    var adding by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Long?>(null) }
    var find by rememberSaveable { mutableStateOf("") }
    val all = Store.notes()
    val notes = if (find.isBlank()) all else { val m = HadithSearch.matcher(find); all.filter { m(Md.plain(it.text)) } }
    val fmt = remember { SimpleDateFormat("d MMM · h:mm a", Locale("ar")) }
    // The editor covers the whole screen as a page of the app: the header, the bars and the «يومي» tabs step aside
    // (Nav.pages), so with the keyboard open the text keeps the room. Not a Dialog: a Dialog window gets no insets,
    // and its bottom slid under the navigation bar.
    if (adding || editing != null) {
        val token = remember { Any() }
        androidx.compose.runtime.DisposableEffect(token) { Nav.pages += token; noteEditorOpen = true; onDispose { Nav.pages.remove(token); noteEditorOpen = false } }
        RichNoteEditor(editing, inline = true) { adding = false; editing = null }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Hint("notes2", "📝", "اكتب ملاحظاتك بعناوين وخط عريض وألوان وقوائم مهام من الشريط فوق الكتابة. اضغط ملاحظة لتفتحها: تؤشّر مربعاتها، وتعلّقها كقصاصة في أي صفحة، أو على الشاشة الرئيسية، أو فقاعة فوق التطبيقات.") }
        item { Action("+ ملاحظة جديدة", Modifier.fillMaxWidth(), filled = false) { adding = true } }
        if (all.size > 3) item { OutlinedTextField(find, { find = it }, placeholder = { Text("ابحث في ملاحظاتك") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (notes.isEmpty()) item { Sub("اكتب ما يخطر ببالك: فائدة، آية، فكرة، قائمة مشتريات…", Modifier.padding(8.dp)) }
        items(notes, key = { it.id }) { n ->
            Column(Modifier.fillMaxWidth().clip(L.shape(10.dp)).background(Md.paper(n.paper)).border(1.dp, if (n.pinned) P.gold else P.line, L.shape(10.dp))
                .clickable { open = n.id }.padding(14.dp)) {
                NoteText(n.text, ink = androidx.compose.ui.graphics.Color(0xFF2A2A2A), scale = .9f, maxLines = 8) { i -> Store.updateNote(n.id) { it.copy(text = Md.toggleBox(it.text, i)) } }
                val tags = listOfNotNull("📌 أعلى".takeIf { n.pinned }, ("📍 " + placeName(n.place)).takeIf { n.place.isNotEmpty() }, "📋 الشاشة الرئيسية".takeIf { n.widget }, "🫧 فقاعة".takeIf { n.bubble })
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fmt.format(Date(n.at)) + if (tags.isNotEmpty()) " · " + tags.joinToString(" · ") else "", color = androidx.compose.ui.graphics.Color(0x992A2A2A), fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("⧉ نسخ", color = androidx.compose.ui.graphics.Color(0xCC2A2A2A), fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable { copyNote(n.text) }.padding(6.dp))
                    Text("✅ مهمة", color = androidx.compose.ui.graphics.Color(0xCC2A2A2A), fontSize = 12.sp, modifier = Modifier.clip(L.shape(8.dp)).clickable {
                        Store.addTask(Md.title(n.text), null); android.widget.Toast.makeText(ctxOf(), "أُضيفت إلى المهام", android.widget.Toast.LENGTH_SHORT).show() }.padding(6.dp))
                }
            }
        }
    }
    open?.let { id -> NoteSheet(id, edit = { editing = Store.notes().firstOrNull { it.id == id } }) { open = null } }
}

/** Where a note hangs, in words. */
fun placeName(p: String) = when (p) { "all" -> "كل الصفحات"; "prayer" -> "الصلاة"; "adhkar" -> "الأذكار"; "library" -> "المكتبة"; "daily" -> "يومي"; "calendar" -> "التقويم"; "more" -> "المزيد"; else -> p }

private fun ctxOf(): android.content.Context = Store.appContext!!
