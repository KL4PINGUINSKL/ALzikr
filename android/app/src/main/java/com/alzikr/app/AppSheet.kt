package com.alzikr.app

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties

/**
 * The app's bottom sheet. Unlike Material's, the sheet itself never follows the finger: a light touch or the end of
 * scrolling its text cannot close it. It closes with the back button, a tap outside it, or by dragging its top bar down.
 */
@Composable
fun AppSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // decorFitsSystemWindows = false: the window reaches the screen edges and tells us the system bars' size, so the
    // last options are lifted above the navigation bar (on Android 15 they were hidden under it).
    // The system bars measured in the app's own window (a dialog's window reports 0 on some phones), with the system's
    // own bar sizes as a floor: the last options always sit above the navigation bar.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    fun sys(name: String) = res.getIdentifier(name, "dimen", "android").let { if (it > 0) res.getDimensionPixelSize(it) else 0 }
    val navPx = maxOf(androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(density), sys("navigation_bar_height"))
    val topPx = maxOf(androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density), sys("status_bar_height"))
    val nav = with(density) { navPx.toDp() }; val top = with(density) { topPx.toDp() }
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
        // The whole sheet rises above the keyboard (user, 2026-11-28: the quote picker's search field hid behind it).
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = top).imePadding().clickable(remember { MutableInteractionSource() }, null) { onDismissRequest() }, contentAlignment = Alignment.BottomCenter) {
            val maxH = maxHeight * .94f
            val imeUp = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0
            // Closing by pulling down, anywhere on the sheet, but only a real pull: it must start with the content already
            // at its top (a swipe that was scrolling the text does not count), the sheet follows the finger, and on
            // release it closes only past ~160 dp; otherwise it springs back. The top bar can be dragged the same way.
            var drag by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
            val closeAt = with(density) { 160.dp.toPx() }
            val pull = remember {
                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                    var scrolled = false   // the content moved during this swipe
                    override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                        // Pulling back up first lifts the sheet to its place.
                        if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && drag > 0f && available.y < 0f) {
                            val n = (drag + available.y).coerceAtLeast(0f); val used = n - drag; drag = n
                            return androidx.compose.ui.geometry.Offset(0f, used)
                        }
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                    override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                        if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero
                        if (consumed.y != 0f) { scrolled = true; return androidx.compose.ui.geometry.Offset.Zero }
                        if (available.y > 0f && !scrolled) { drag += available.y; return androidx.compose.ui.geometry.Offset(0f, available.y) }
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                    override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                        scrolled = false   // the finger left the screen: the next swipe starts fresh
                        if (drag <= 0f) return androidx.compose.ui.unit.Velocity.Zero
                        if (drag > closeAt) onDismissRequest()
                        else androidx.compose.animation.core.animate(drag, 0f) { v, _ -> drag = v }
                        return available
                    }
                }
            }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            AnimatedVisibility(shown, enter = slideInVertically { it } + fadeIn()) {
                Column(Modifier.fillMaxWidth().heightIn(max = maxH).graphicsLayer { translationY = drag }.nestedScroll(pull)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(P.surface)
                    // Taps inside the sheet stay inside it.
                    .clickable(remember { MutableInteractionSource() }, null) {}.padding(bottom = if (imeUp) 8.dp else nav + 14.dp)) {
                    Box(Modifier.fillMaxWidth().height(30.dp).pointerInput(Unit) {
                        detectVerticalDragGestures(onDragEnd = { if (drag > closeAt) onDismissRequest() else scope.launch { androidx.compose.animation.core.animate(drag, 0f) { v, _ -> drag = v } } },
                            onDragCancel = { scope.launch { androidx.compose.animation.core.animate(drag, 0f) { v, _ -> drag = v } } }) { ch, dy ->
                            ch.consume(); drag = (drag + dy).coerceAtLeast(0f)
                        }
                    }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(P.line))
                    }
                    // The content scrolls inside what is left, so the space above the navigation bar is never pushed out.
                    Column(Modifier.weight(1f, fill = false)) { content() }
                }
            }
        }
    }
}
