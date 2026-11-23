package com.alzikr.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.Favorite
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.Star
import androidx.compose.material.icons.sharp.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The section icons in each icon style; the book is drawn here (the core icon set has none). */
object AppIcons {
    private fun book(outlined: Boolean, rounded: Boolean): ImageVector = ImageVector.Builder("book", 24.dp, 24.dp, 24f, 24f).apply {
        if (outlined) {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineJoin = if (rounded) StrokeJoin.Round else StrokeJoin.Miter) {
                moveTo(12f, 6.5f); curveTo(10f, 5f, 6.5f, 4.6f, 3f, 5.2f); lineTo(3f, 18.6f); curveTo(6.5f, 18f, 10f, 18.4f, 12f, 19.8f)
                curveTo(14f, 18.4f, 17.5f, 18f, 21f, 18.6f); lineTo(21f, 5.2f); curveTo(17.5f, 4.6f, 14f, 5f, 12f, 6.5f); close()
                moveTo(12f, 6.5f); lineTo(12f, 19.8f)
            }
        } else {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.2f, 6f); curveTo(9.2f, 4.6f, 6f, 4.2f, 2.5f, 4.8f); lineTo(2.5f, 19f); curveTo(6f, 18.4f, 9.2f, 18.8f, 11.2f, 20.2f); close()
                moveTo(12.8f, 6f); curveTo(14.8f, 4.6f, 18f, 4.2f, 21.5f, 4.8f); lineTo(21.5f, 19f); curveTo(18f, 18.4f, 14.8f, 18.8f, 12.8f, 20.2f); close()
            }
        }
    }.build()

    private val cache = HashMap<String, ImageVector>()

    fun of(section: String, style: IconStyle): ImageVector = cache.getOrPut("$section/$style") {
        when (section) {
            "prayer" -> when (style) { IconStyle.OUTLINED -> Icons.Outlined.Notifications; IconStyle.ROUNDED -> Icons.Rounded.Notifications; IconStyle.SHARP -> Icons.Sharp.Notifications; else -> Icons.Filled.Notifications }
            "adhkar" -> when (style) { IconStyle.OUTLINED -> Icons.Outlined.FavoriteBorder; IconStyle.ROUNDED -> Icons.Rounded.Favorite; IconStyle.SHARP -> Icons.Sharp.Favorite; else -> Icons.Filled.Favorite }
            "library" -> book(outlined = style == IconStyle.OUTLINED, rounded = style == IconStyle.ROUNDED)
            "daily" -> when (style) { IconStyle.OUTLINED -> Icons.Outlined.CheckCircle; IconStyle.ROUNDED -> Icons.Rounded.CheckCircle; IconStyle.SHARP -> Icons.Sharp.CheckCircle; else -> Icons.Filled.CheckCircle }
            "kids" -> when (style) { IconStyle.OUTLINED -> Icons.Outlined.Star; IconStyle.ROUNDED -> Icons.Rounded.Star; IconStyle.SHARP -> Icons.Sharp.Star; else -> Icons.Filled.Star }
            else -> when (style) { IconStyle.OUTLINED -> Icons.Outlined.Menu; IconStyle.ROUNDED -> Icons.Rounded.Menu; IconStyle.SHARP -> Icons.Sharp.Menu; else -> Icons.Filled.Menu }
        }
    }
}
