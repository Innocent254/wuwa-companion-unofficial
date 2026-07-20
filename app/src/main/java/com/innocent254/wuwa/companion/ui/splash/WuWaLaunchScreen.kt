package com.innocent254.wuwa.companion.ui.splash

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innocent254.wuwa.companion.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LaunchDurationMillis = 5_500

/**
 * Theme-aware launch animation shared by phones and tablets.
 *
 * Phones use the compact vertical lockup; landscape tablets use the horizontal
 * lockup. The title and divider fully retract before the emblem settles at the
 * exact center, which also prevents a trailing glyph or divider overlap.
 */
@Composable
fun WuWaLaunchScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LaunchDurationMillis,
                easing = LinearEasing,
            ),
        )
        currentOnFinished()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val tablet = maxWidth >= 600.dp && maxWidth > maxHeight
        val timeline = LaunchTimeline(progress.value)

        if (tablet) {
            TabletLaunchLayout(
                width = maxWidth,
                height = maxHeight,
                timeline = timeline,
            )
        } else {
            PhoneLaunchLayout(
                width = maxWidth,
                height = maxHeight,
                timeline = timeline,
            )
        }
    }
}

@Composable
private fun PhoneLaunchLayout(
    width: Dp,
    height: Dp,
    timeline: LaunchTimeline,
) {
    val iconSize = 88.dp
    val centerX = width / 2
    val centerY = height / 2
    val stagedCenterY = centerY - 72.dp
    val iconCenterY = lerp(stagedCenterY, centerY, timeline.settle)
    val dividerY = centerY + 10.dp
    val titleTop = dividerY + 18.dp

    LaunchParticles(
        centerX = centerX,
        centerY = iconCenterY,
        radius = 55.dp,
        phase = timeline.raw,
        alpha = timeline.particleAlpha,
    )
    LaunchIcon(
        drawable = R.drawable.resonance_node_icon,
        size = iconSize,
        x = centerX - iconSize / 2,
        y = iconCenterY - iconSize / 2,
        timeline = timeline,
    )

    if (timeline.dividerAlpha > 0f) {
        Box(
            modifier = Modifier
                .offset(x = centerX - 130.dp, y = dividerY)
                .width(260.dp)
                .height(2.dp)
                .graphicsLayer { alpha = timeline.dividerAlpha }
                .background(
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(50),
                ),
        )
    }

    if (timeline.textVisibility > 0f) {
        Box(
            modifier = Modifier
                .offset(x = centerX - 145.dp, y = titleTop)
                .width(290.dp)
                .height(74.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LaunchTitle(
                modifier = Modifier
                    .offset(y = (-74).dp * (1f - timeline.textVisibility))
                    .alpha(timeline.textVisibility),
                centered = true,
            )
        }
    }
}

@Composable
private fun TabletLaunchLayout(
    width: Dp,
    height: Dp,
    timeline: LaunchTimeline,
) {
    val iconSize = 100.dp
    val centerX = width / 2
    val centerY = height / 2
    val stagedCenterX = centerX - 115.dp
    val iconCenterX = lerp(stagedCenterX, centerX, timeline.settle)
    val dividerX = centerX
    val titleLeft = dividerX + 20.dp

    LaunchParticles(
        centerX = iconCenterX,
        centerY = centerY,
        radius = 62.dp,
        phase = timeline.raw,
        alpha = timeline.particleAlpha,
    )
    LaunchIcon(
        drawable = R.drawable.resonance_node_icon,
        size = iconSize,
        x = iconCenterX - iconSize / 2,
        y = centerY - iconSize / 2,
        timeline = timeline,
    )

    if (timeline.dividerAlpha > 0f) {
        Box(
            modifier = Modifier
                .offset(x = dividerX - 1.dp, y = centerY - 70.dp)
                .width(2.dp)
                .height(140.dp)
                .graphicsLayer { alpha = timeline.dividerAlpha }
                .background(
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(50),
                ),
        )
    }

    if (timeline.textVisibility > 0f) {
        Box(
            modifier = Modifier
                .offset(x = titleLeft, y = centerY - 38.dp)
                .width(285.dp)
                .height(76.dp)
                .clipToBounds(),
            contentAlignment = Alignment.CenterStart,
        ) {
            LaunchTitle(
                modifier = Modifier
                    .offset(x = (-285).dp * (1f - timeline.textVisibility))
                    .alpha(timeline.textVisibility),
                centered = false,
            )
        }
    }
}

@Composable
private fun LaunchIcon(
    @DrawableRes drawable: Int,
    size: Dp,
    x: Dp,
    y: Dp,
    timeline: LaunchTimeline,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = stringResource(R.string.launch_icon_description),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer {
                alpha = timeline.iconAlpha
                scaleX = timeline.iconScale
                scaleY = timeline.iconScale
                rotationZ = timeline.iconRotation
            },
    )
}

@Composable
private fun LaunchTitle(
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.launch_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.launch_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.35.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
private fun LaunchParticles(
    centerX: Dp,
    centerY: Dp,
    radius: Dp,
    phase: Float,
    alpha: Float,
) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.fillMaxSize()) {
        if (alpha <= 0f) return@Canvas
        val cx = centerX.toPx()
        val cy = centerY.toPx()
        val baseRadius = radius.toPx()

        repeat(16) { index ->
            val angle = (index / 16f) * (PI * 2f) + phase * PI * 2f
            val wobble = 0.84f +
                0.16f * sin(phase * PI * 4f + index).toFloat()
            val x = cx + cos(angle).toFloat() * baseRadius * wobble
            val y = cy + sin(angle).toFloat() * baseRadius * 0.46f * wobble
            drawCircle(
                color = color,
                radius = if (index % 3 == 0) 1.5.dp.toPx() else 0.9.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y),
                alpha = alpha * (0.25f + (index % 4) * 0.08f),
            )
        }

        repeat(3) { index ->
            drawOval(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(
                    cx - baseRadius * (0.55f + index * 0.23f),
                    cy - baseRadius * (0.20f + index * 0.08f),
                ),
                size = androidx.compose.ui.geometry.Size(
                    baseRadius * 2f * (0.55f + index * 0.23f),
                    baseRadius * 2f * (0.20f + index * 0.08f),
                ),
                alpha = alpha * (0.12f - index * 0.02f),
                style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

private data class LaunchTimeline(val raw: Float) {
    val iconAlpha = smoothStep(0.00f, 0.10f, raw)
    val iconScale = lerp(0.62f, 1f, smoothStep(0.00f, 0.20f, raw))
    val iconRotation = lerp(-55f, 0f, smoothStep(0.00f, 0.28f, raw))
    val textVisibility = when {
        raw < 0.14f -> 0f
        raw < 0.30f -> smoothStep(0.14f, 0.30f, raw)
        raw < 0.64f -> 1f
        raw < 0.78f -> 1f - smoothStep(0.64f, 0.78f, raw)
        else -> 0f
    }
    val dividerAlpha = when {
        raw < 0.05f -> smoothStep(0f, 0.05f, raw)
        raw < 0.70f -> 1f
        raw < 0.78f -> 1f - smoothStep(0.70f, 0.78f, raw)
        else -> 0f
    }
    val settle = smoothStep(0.64f, 0.78f, raw)
    val particleAlpha = iconAlpha * (1f - smoothStep(0.68f, 0.78f, raw))
}

private fun smoothStep(start: Float, end: Float, value: Float): Float {
    if (start == end) return if (value < start) 0f else 1f
    val normalized = ((value - start) / (end - start)).coerceIn(0f, 1f)
    return normalized * normalized * (3f - 2f * normalized)
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp =
    start + (end - start) * fraction
