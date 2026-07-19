package com.innocent254.wuwa.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Lightweight, original line icons shaped for the companion's rounded WuWa visual language. */
internal enum class WuWaIcon {
    HOME,
    LIBRARY,
    UPDATES,
    SETTINGS,
    RESONATOR,
    WEAPON,
    ECHO,
    MATERIAL,
    SEARCH,
    BACK,
    DOWNLOAD,
    INSTALL,
    RETRY,
    CHECK,
    THEME,
    IMAGE,
    LANGUAGE,
    INFO,
    DATABASE,
    GENERIC,
}

@Composable
internal fun WuWaGlyph(
    icon: WuWaIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val accessibleModifier = if (contentDescription == null) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = accessibleModifier) {
        val color = if (tint == Color.Unspecified) Color.White else tint
        drawWuWaIcon(icon, color)
    }
}

internal fun categoryIcon(categoryId: String): WuWaIcon = when {
    categoryId.contains("resonator", ignoreCase = true) -> WuWaIcon.RESONATOR
    categoryId.contains("weapon", ignoreCase = true) -> WuWaIcon.WEAPON
    categoryId.contains("echo", ignoreCase = true) -> WuWaIcon.ECHO
    categoryId.contains("material", ignoreCase = true) -> WuWaIcon.MATERIAL
    else -> WuWaIcon.GENERIC
}

private fun DrawScope.drawWuWaIcon(icon: WuWaIcon, color: Color) {
    val u = size.minDimension / 24f
    val dx = (size.width - 24f * u) / 2f
    val dy = (size.height - 24f * u) / 2f
    fun p(x: Float, y: Float) = Offset(dx + x * u, dy + y * u)
    val line = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = line)
    fun arc(start: Float, sweep: Float, useCenter: Boolean, x: Float, y: Float, w: Float, h: Float) =
        drawArc(color, start, sweep, useCenter, p(x, y), Size(w * u, h * u), style = line)
    fun oval(x: Float, y: Float, w: Float, h: Float) =
        drawOval(color, p(x, y), Size(w * u, h * u), style = line)

    when (icon) {
        WuWaIcon.HOME -> {
            path { moveTo(p(3.5f, 11f).x, p(3.5f, 11f).y); lineTo(p(12f, 4f).x, p(12f, 4f).y); lineTo(p(20.5f, 11f).x, p(20.5f, 11f).y) }
            path { moveTo(p(5.5f, 10f).x, p(5.5f, 10f).y); lineTo(p(5.5f, 19.5f).x, p(5.5f, 19.5f).y); lineTo(p(18.5f, 19.5f).x, p(18.5f, 19.5f).y); lineTo(p(18.5f, 10f).x, p(18.5f, 10f).y) }
            drawLine(color, p(9.5f, 19.5f), p(9.5f, 14.5f), line.width, StrokeCap.Round)
            drawLine(color, p(9.5f, 14.5f), p(14.5f, 14.5f), line.width, StrokeCap.Round)
        }
        WuWaIcon.LIBRARY -> {
            path { moveTo(p(4f, 5.5f).x, p(4f, 5.5f).y); quadraticBezierTo(p(8f, 4.2f).x, p(8f, 4.2f).y, p(12f, 7f).x, p(12f, 7f).y); lineTo(p(12f, 20f).x, p(12f, 20f).y); quadraticBezierTo(p(8f, 17.3f).x, p(8f, 17.3f).y, p(4f, 18.5f).x, p(4f, 18.5f).y); close() }
            path { moveTo(p(20f, 5.5f).x, p(20f, 5.5f).y); quadraticBezierTo(p(16f, 4.2f).x, p(16f, 4.2f).y, p(12f, 7f).x, p(12f, 7f).y); lineTo(p(12f, 20f).x, p(12f, 20f).y); quadraticBezierTo(p(16f, 17.3f).x, p(16f, 17.3f).y, p(20f, 18.5f).x, p(20f, 18.5f).y); close() }
        }
        WuWaIcon.UPDATES, WuWaIcon.RETRY -> {
            arc(205f, 275f, false, 4f, 4f, 16f, 16f)
            path { moveTo(p(18.8f, 4.8f).x, p(18.8f, 4.8f).y); lineTo(p(19.5f, 9.2f).x, p(19.5f, 9.2f).y); lineTo(p(15.2f, 8.2f).x, p(15.2f, 8.2f).y) }
        }
        WuWaIcon.SETTINGS -> {
            listOf(7f to 8.5f, 12f to 15.5f, 17f to 10.5f).forEach { (x, knob) ->
                drawLine(color, p(x, 4f), p(x, 20f), line.width, StrokeCap.Round)
                drawCircle(color, 2.2f * u, p(x, knob), style = Stroke(line.width))
            }
        }
        WuWaIcon.RESONATOR -> {
            drawCircle(color, 3.1f * u, p(12f, 7.2f), style = line)
            path { moveTo(p(6f, 20f).x, p(6f, 20f).y); quadraticBezierTo(p(6.8f, 13f).x, p(6.8f, 13f).y, p(12f, 12.5f).x, p(12f, 12.5f).y); quadraticBezierTo(p(17.2f, 13f).x, p(17.2f, 13f).y, p(18f, 20f).x, p(18f, 20f).y) }
            drawCircle(color, 1.25f * u, p(12f, 16.5f))
        }
        WuWaIcon.WEAPON -> {
            path { moveTo(p(5f, 19f).x, p(5f, 19f).y); lineTo(p(16.8f, 7.2f).x, p(16.8f, 7.2f).y); lineTo(p(20f, 4f).x, p(20f, 4f).y); lineTo(p(18.8f, 9f).x, p(18.8f, 9f).y); lineTo(p(7f, 20.8f).x, p(7f, 20.8f).y) }
            drawLine(color, p(5.2f, 14.8f), p(9.2f, 18.8f), line.width, StrokeCap.Round)
        }
        WuWaIcon.ECHO -> path { moveTo(p(3f, 12f).x, p(3f, 12f).y); cubicTo(p(5.5f, 5f).x, p(5.5f, 5f).y, p(7.5f, 19f).x, p(7.5f, 19f).y, p(10f, 12f).x, p(10f, 12f).y); cubicTo(p(12.5f, 5f).x, p(12.5f, 5f).y, p(14.5f, 19f).x, p(14.5f, 19f).y, p(17f, 12f).x, p(17f, 12f).y); cubicTo(p(18.5f, 8f).x, p(18.5f, 8f).y, p(20f, 9f).x, p(20f, 9f).y, p(21f, 12f).x, p(21f, 12f).y) }
        WuWaIcon.MATERIAL -> path { moveTo(p(12f, 3.5f).x, p(12f, 3.5f).y); lineTo(p(19.5f, 9f).x, p(19.5f, 9f).y); lineTo(p(16.5f, 20f).x, p(16.5f, 20f).y); lineTo(p(7.5f, 20f).x, p(7.5f, 20f).y); lineTo(p(4.5f, 9f).x, p(4.5f, 9f).y); close(); moveTo(p(4.5f, 9f).x, p(4.5f, 9f).y); lineTo(p(19.5f, 9f).x, p(19.5f, 9f).y); moveTo(p(12f, 3.5f).x, p(12f, 3.5f).y); lineTo(p(9f, 9f).x, p(9f, 9f).y); lineTo(p(12f, 20f).x, p(12f, 20f).y); lineTo(p(15f, 9f).x, p(15f, 9f).y) }
        WuWaIcon.SEARCH -> { drawCircle(color, 6f * u, p(10.5f, 10.5f), style = line); drawLine(color, p(15f, 15f), p(20f, 20f), line.width, StrokeCap.Round) }
        WuWaIcon.BACK -> path { moveTo(p(14.5f, 5f).x, p(14.5f, 5f).y); lineTo(p(7.5f, 12f).x, p(7.5f, 12f).y); lineTo(p(14.5f, 19f).x, p(14.5f, 19f).y) }
        WuWaIcon.DOWNLOAD -> { drawLine(color, p(12f, 4f), p(12f, 15f), line.width, StrokeCap.Round); path { moveTo(p(7.5f, 11f).x, p(7.5f, 11f).y); lineTo(p(12f, 15.5f).x, p(12f, 15.5f).y); lineTo(p(16.5f, 11f).x, p(16.5f, 11f).y) }; drawLine(color, p(5f, 20f), p(19f, 20f), line.width, StrokeCap.Round) }
        WuWaIcon.INSTALL, WuWaIcon.CHECK -> path { moveTo(p(4.5f, 12.5f).x, p(4.5f, 12.5f).y); lineTo(p(9.5f, 17.5f).x, p(9.5f, 17.5f).y); lineTo(p(19.5f, 6.5f).x, p(19.5f, 6.5f).y) }
        WuWaIcon.THEME -> { drawCircle(color, 7f * u, p(12f, 12f), style = line); drawArc(color, 90f, 180f, true, p(5f, 5f), Size(14f * u, 14f * u)) }
        WuWaIcon.IMAGE -> { path { moveTo(p(4f, 5f).x, p(4f, 5f).y); lineTo(p(20f, 5f).x, p(20f, 5f).y); lineTo(p(20f, 19f).x, p(20f, 19f).y); lineTo(p(4f, 19f).x, p(4f, 19f).y); close(); moveTo(p(5f, 17f).x, p(5f, 17f).y); lineTo(p(10f, 12f).x, p(10f, 12f).y); lineTo(p(13f, 15f).x, p(13f, 15f).y); lineTo(p(16f, 11f).x, p(16f, 11f).y); lineTo(p(20f, 16f).x, p(20f, 16f).y) }; drawCircle(color, 1.5f * u, p(15.5f, 9f)) }
        WuWaIcon.LANGUAGE -> { drawCircle(color, 8f * u, p(12f, 12f), style = line); oval(8.5f, 4f, 7f, 16f); drawLine(color, p(4f, 12f), p(20f, 12f), line.width, StrokeCap.Round) }
        WuWaIcon.INFO -> { drawCircle(color, 8f * u, p(12f, 12f), style = line); drawCircle(color, .9f * u, p(12f, 8f)); drawLine(color, p(12f, 11f), p(12f, 16.5f), line.width, StrokeCap.Round) }
        WuWaIcon.DATABASE -> { oval(5f, 4f, 14f, 5f); arc(0f, 180f, false, 5f, 8f, 14f, 5f); arc(0f, 180f, false, 5f, 13f, 14f, 5f); drawLine(color, p(5f, 6.5f), p(5f, 15.5f), line.width, StrokeCap.Round); drawLine(color, p(19f, 6.5f), p(19f, 15.5f), line.width, StrokeCap.Round) }
        WuWaIcon.GENERIC -> { drawCircle(color, 8f * u, p(12f, 12f), style = line); drawCircle(color, 3f * u, p(12f, 12f), style = line); drawCircle(color, 1.2f * u, p(12f, 4f)) }
    }
}
