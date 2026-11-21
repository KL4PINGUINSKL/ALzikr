package com.alzikr.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val tab = mutableStateOf("prayer")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        intent?.getStringExtra("tab")?.let { tab.value = it }
        AppUpdate.check(this)
        // Installed before methods followed the country: look the country up once.
        if (Store.hasLocation && Store.country.isEmpty() && !Store.methodChosen) Alerts.resolvePlace(this, Store.lat, Store.lng)
        if (Build.VERSION.SDK_INT >= 33 && !Alerts.canPost(this)) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { Root(tab.value) { tab.value = it } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("tab")?.let { tab.value = it }
    }

    override fun onResume() {
        super.onResume()
        Alerts.reschedule(this)
        Alerts.focusNotification(this)
    }
}

/** "الجمعة 20 جمادى الأولى 1448 هـ · 21 نوفمبر" */
fun dateLine(date: LocalDate = LocalDate.now()): String {
    val ar = Locale("ar")
    val h = Data.hijri(date)
    val hijri = "${h.day} ${h.monthName} ${h.year}"
    val day = date.format(DateTimeFormatter.ofPattern("EEEE", ar))
    val greg = date.format(DateTimeFormatter.ofPattern("d MMMM", ar))
    return "$day $hijri هـ · $greg"
}

@Composable
private fun Root(tab: String, onTab: (String) -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val pal = when (Data.theme) { 1 -> Day; 2 -> Night; else -> if (isSystemInDarkTheme()) Night else Day }
    val scheme = if (pal.dark) darkColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
        else lightColorScheme(primary = pal.accent, onPrimary = pal.onAccent, surface = pal.surface, onSurface = pal.ink, background = pal.bg, onSurfaceVariant = pal.muted, surfaceVariant = pal.card)
    var settings by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPal provides pal, LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxSize().background(pal.bg)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ALzikr", color = pal.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Sub(dateLine(), size = 12.sp)
                    }
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable { settings = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Settings, "الإعدادات", tint = pal.muted)
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        "adhkar" -> AdhkarScreen()
                        "tasks", "daily" -> TasksScreen()
                        "focus" -> FocusScreen()
                        "more" -> MoreScreen(openSettings = { settings = true })
                        else -> PrayerScreen(openSettings = { settings = true })
                    }
                }
                TabBar(tab, onTab)
            }
            if (settings) SettingsSheet { settings = false }
        }
    }
}

@Composable
private fun TabBar(tab: String, onTab: (String) -> Unit) {
    val items = listOf(
        Triple("prayer", "الصلاة", Icons.Filled.Notifications),
        Triple("adhkar", "الأذكار", Icons.Filled.Favorite),
        Triple("daily", "يومي", Icons.Filled.CheckCircle),
        Triple("focus", "التركيز", Icons.Filled.PlayArrow),
        Triple("more", "المزيد", Icons.Filled.Menu),
    )
    Column(Modifier.fillMaxWidth().background(P.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            items.forEach { (id, label, icon) -> TabItem(label, icon, tab == id || id == "daily" && tab == "tasks") { onTab(id) } }
        }
    }
}

@Composable
private fun TabItem(label: String, icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) P.accent.copy(alpha = .16f) else P.surface).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(icon, null, tint = if (on) P.accent else P.muted, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (on) P.accent else P.muted, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
    }
}

