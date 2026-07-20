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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
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
import kotlin.math.min
import kotlin.math.sin

private const val LaunchDurationMillis = 5_500

/**
 * Native Compose port of the supplied splash_phone.html and splash_tablet.html.
 * All artwork and motion are rendered from vector paths at runtime.
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
        val timeline = LaunchTimeline(progress.value)
        val tablet = maxWidth >= 600.dp && maxWidth > maxHeight

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
    val centerX = width / 2
    val centerY = height / 2
    val iconSize = 78.dp
    val stagedIconCenterY = centerY - 58.dp
    val iconCenterY = lerp(stagedIconCenterY, centerY, timeline.settle)
    val lineY = centerY + 2.dp

    PhoneWaveField(
        centerX = centerX,
        lineY = lineY,
        iconCenterY = iconCenterY,
        timeline = timeline,
    )

    LaunchEmblem(
        drawable = R.drawable.resonance_node_icon,
        size = iconSize,
        x = centerX - iconSize / 2,
        y = iconCenterY - iconSize / 2,
        timeline = timeline,
    )

    LaunchTitle(
        modifier = Modifier
            .offset(x = centerX - 170.dp, y = lineY + 31.dp)
            .width(340.dp),
        centered = true,
        tablet = false,
        timeline = timeline,
    )

    LaunchProgress(
        modifier = Modifier.offset(x = centerX - 50.dp, y = lineY + 116.dp),
        width = 100.dp,
        timeline = timeline,
    )
}

@Composable
private fun TabletLaunchLayout(
    width: Dp,
    height: Dp,
    timeline: LaunchTimeline,
) {
    val centerX = width / 2
    val centerY = height / 2
    val iconSize = 88.dp
    val stagedIconCenterX = centerX - 126.dp
    val iconCenterX = lerp(stagedIconCenterX, centerX, timeline.settle)
    val titleLeft = centerX + 34.dp

    TabletWaveField(
        lineX = centerX,
        centerY = centerY,
        iconCenterX = iconCenterX,
        timeline = timeline,
    )

    LaunchEmblem(
        drawable = R.drawable.resonance_node_icon,
        size = iconSize,
        x = iconCenterX - iconSize / 2,
        y = centerY - iconSize / 2,
        timeline = timeline,
    )

    LaunchTitle(
        modifier = Modifier
            .offset(x = titleLeft, y = centerY - 39.dp)
            .width(360.dp),
        centered = false,
        tablet = true,
        timeline = timeline,
    )

    LaunchProgress(
        modifier = Modifier.offset(x = titleLeft, y = centerY + 64.dp),
        width = 120.dp,
        timeline = timeline,
    )
}

@Composable
private fun PhoneWaveField(
    centerX: Dp,
    lineY: Dp,
    iconCenterY: Dp,
    timeline: LaunchTimeline,
) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.fillMaxSize()) {
        if (timeline.sceneAlpha <= 0f) return@Canvas

        val cx = centerX.toPx()
        val y = lineY.toPx()
        val iconY = iconCenterY.toPx()
        val grow = smoothStep(0f, 0.09f, timeline.raw)
        val halfLength = min(size.width * 0.44f, 210.dp.toPx()) * grow
        val amplitude = 2.dp.toPx()
        val path = Path()
        val segments = 80

        repeat(segments + 1) { index ->
            val fraction = index / segments.toFloat()
            val x = cx - halfLength + halfLength * 2f * fraction
            val envelope = sin(fraction * PI).toFloat()
            val wave = sin(fraction * PI * 6f + timeline.raw * PI * 6f).toFloat()
            val waveY = y + wave * amplitude * envelope
            if (index == 0) path.moveTo(x, waveY) else path.lineTo(x, waveY)
        }

        drawPath(
            path = path,
            color = color,
            alpha = timeline.sceneAlpha * 0.06f,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = color,
            alpha = timeline.sceneAlpha * 0.86f,
            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
        )

        val capPulse = 0.7f + 0.3f * sin(timeline.raw * PI * 22f).toFloat()
        drawCircle(
            color = color,
            radius = 2.3.dp.toPx() * capPulse,
            center = androidx.compose.ui.geometry.Offset(cx - halfLength, y),
            alpha = timeline.sceneAlpha * 0.5f,
        )
        drawCircle(
            color = color,
            radius = 2.3.dp.toPx() * capPulse,
            center = androidx.compose.ui.geometry.Offset(cx + halfLength, y),
            alpha = timeline.sceneAlpha * 0.5f,
        )

        repeat(40) { index ->
            val base = ((index * 37) % 101) / 100f
            val drift = sin(timeline.raw * PI * 8f + index * 1.7f).toFloat()
            val particleX = cx - halfLength + halfLength * 2f * base
            val particleY = y + drift * 5.dp.toPx()
            val pulse = 0.5f + 0.5f * sin(timeline.raw * PI * 10f + index).toFloat()
            drawCircle(
                color = color,
                radius = (0.6f + (index % 4) * 0.25f).dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(particleX, particleY),
                alpha = timeline.sceneAlpha * pulse * 0.52f,
            )
        }

        repeat(16) { index ->
            val angle = index / 16f * PI * 2f + timeline.raw * PI * 3f
            val radius = (19 + index % 5 * 2).dp.toPx()
            val particleX = cx + cos(angle).toFloat() * radius
            val particleY = iconY + sin(angle).toFloat() * radius * 0.42f
            val pulse = 0.72f + 0.28f * sin(timeline.raw * PI * 4f + index).toFloat()
            drawCircle(
                color = color,
                radius = (0.85f + (index % 3) * 0.3f).dp.toPx() * pulse,
                center = androidx.compose.ui.geometry.Offset(particleX, particleY),
                alpha = timeline.sceneAlpha * 0.52f * pulse,
            )
        }

        drawAmbientParticles(
            color = color,
            colorAlpha = timeline.sceneAlpha * 0.18f,
            phase = timeline.raw,
        )
    }
}

@Composable
private fun TabletWaveField(
    lineX: Dp,
    centerY: Dp,
    iconCenterX: Dp,
    timeline: LaunchTimeline,
) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.fillMaxSize()) {
        if (timeline.sceneAlpha <= 0f) return@Canvas

        val x = lineX.toPx()
        val cy = centerY.toPx()
        val iconX = iconCenterX.toPx()
        val grow = smoothStep(0f, 0.09f, timeline.raw)
        val halfLength = min(size.height * 0.43f, 280.dp.toPx()) * grow
        val amplitude = 3.5.dp.toPx()
        val path = Path()
        val segments = 96

        repeat(segments + 1) { index ->
            val fraction = index / segments.toFloat()
            val y = cy - halfLength + halfLength * 2f * fraction
            val envelope = sin(fraction * PI).toFloat()
            val wave = sin(fraction * PI * 8f + timeline.raw * PI * 4f).toFloat()
            val waveX = x + wave * amplitude * envelope
            if (index == 0) path.moveTo(waveX, y) else path.lineTo(waveX, y)
        }

        drawPath(
            path = path,
            color = color,
            alpha = timeline.sceneAlpha * 0.08f,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = color,
            alpha = timeline.sceneAlpha * 0.90f,
            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round),
        )

        val capPulse = 0.7f + 0.3f * sin(timeline.raw * PI * 18f).toFloat()
        drawCircle(
            color = color,
            radius = 2.5.dp.toPx() * capPulse,
            center = androidx.compose.ui.geometry.Offset(x, cy - halfLength),
            alpha = timeline.sceneAlpha * 0.55f,
        )
        drawCircle(
            color = color,
            radius = 2.5.dp.toPx() * capPulse,
            center = androidx.compose.ui.geometry.Offset(x, cy + halfLength),
            alpha = timeline.sceneAlpha * 0.55f,
        )

        repeat(50) { index ->
            val fraction = ((index * 29) % 101) / 100f
            val particleY = cy - halfLength + halfLength * 2f * fraction
            val drift = sin(timeline.raw * PI * 8f + index * 1.3f).toFloat()
            val particleX = x + drift * 4.dp.toPx()
            val pulse = 0.5f + 0.5f * sin(timeline.raw * PI * 9f + index).toFloat()
            drawCircle(
                color = color,
                radius = (0.55f + (index % 4) * 0.22f).dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(particleX, particleY),
                alpha = timeline.sceneAlpha * pulse * 0.55f,
            )
        }

        repeat(3) { ringIndex ->
            val radius = (31 + ringIndex * 28).dp.toPx()
            drawOval(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(
                    iconX - radius,
                    cy - radius * 0.35f,
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.70f),
                alpha = timeline.sceneAlpha * 0.045f,
                style = Stroke(width = 0.55.dp.toPx(), cap = StrokeCap.Round),
            )

            val count = 10 + ringIndex * 6
            repeat(count) { index ->
                val direction = if (ringIndex % 2 == 0) 1f else -1f
                val angle = index / count.toFloat() * PI * 2f +
                    direction * timeline.raw * PI * (2f + ringIndex * 0.5f)
                val pulse = 0.88f + 0.12f * sin(timeline.raw * PI * 8f + index).toFloat()
                val particleX = iconX + cos(angle).toFloat() * radius * pulse
                val particleY = cy + sin(angle).toFloat() * radius * 0.35f * pulse
                drawCircle(
                    color = color,
                    radius = (0.75f + (index % 3) * 0.25f).dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(particleX, particleY),
                    alpha = timeline.sceneAlpha * (0.28f + ringIndex * 0.07f),
                )
            }
        }

        drawAmbientParticles(
            color = color,
            colorAlpha = timeline.sceneAlpha * 0.16f,
            phase = timeline.raw,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAmbientParticles(
    color: androidx.compose.ui.graphics.Color,
    colorAlpha: Float,
    phase: Float,
) {
    repeat(18) { index ->
        val xFraction = ((index * 43) % 97) / 96f
        val yFraction = ((index * 67) % 103) / 102f
        val driftX = sin(phase * PI * 2f + index).toFloat() * 8.dp.toPx()
        val driftY = cos(phase * PI * 1.4f + index * 0.7f).toFloat() * 8.dp.toPx()
        drawCircle(
            color = color,
            radius = (0.4f + (index % 3) * 0.2f).dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                size.width * xFraction + driftX,
                size.height * yFraction + driftY,
            ),
            alpha = colorAlpha * (0.55f + (index % 4) * 0.12f),
        )
    }
}

@Composable
private fun LaunchEmblem(
    @DrawableRes drawable: Int,
    size: Dp,
    x: Dp,
    y: Dp,
    timeline: LaunchTimeline,
) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer {
                alpha = timeline.iconAlpha
                scaleX = timeline.iconScale
                scaleY = timeline.iconScale
                rotationZ = timeline.iconRotation
            }
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(22),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = stringResource(R.string.launch_icon_description),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LaunchTitle(
    centered: Boolean,
    tablet: Boolean,
    timeline: LaunchTimeline,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.launch_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = if (tablet) 28.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .offset(y = 12.dp * (1f - timeline.titleEntrance))
                .alpha(timeline.titleAlpha),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.launch_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (tablet) 13.sp else 11.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 6.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .offset(y = 8.dp * (1f - timeline.subtitleEntrance))
                .alpha(timeline.subtitleAlpha),
        )
    }
}

@Composable
private fun LaunchProgress(
    width: Dp,
    timeline: LaunchTimeline,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(2.dp)
            .alpha(timeline.progressAlpha)
            .background(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
            ),
    ) {
        Box(
            modifier = Modifier
                .width(width * timeline.progressFraction)
                .height(2.dp)
                .background(
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(50),
                ),
        )
    }
}

private data class LaunchTimeline(val raw: Float) {
    private val exit = 1f - smoothStep(0.73f, 0.82f, raw)

    val sceneAlpha = when {
        raw < 0.055f -> smoothStep(0f, 0.055f, raw)
        raw < 0.73f -> 1f
        raw < 0.82f -> 1f - smoothStep(0.73f, 0.82f, raw)
        else -> 0f
    }
    val iconAlpha = smoothStep(0f, 0.10f, raw)
    val iconScale = lerp(0.72f, 1f, smoothStep(0f, 0.20f, raw))
    val iconRotation = lerp(-18f, 0f, smoothStep(0f, 0.26f, raw))
    val titleEntrance = smoothStep(0.145f, 0.365f, raw)
    val subtitleEntrance = smoothStep(0.255f, 0.435f, raw)
    val titleAlpha = titleEntrance * exit
    val subtitleAlpha = subtitleEntrance * exit
    val progressAlpha = smoothStep(0.327f, 0.435f, raw) * exit
    val progressFraction = when {
        raw < 0.364f -> 0f
        raw < 0.538f -> lerp(0f, 0.55f, smoothStep(0.364f, 0.538f, raw))
        raw < 0.669f -> lerp(0.55f, 0.80f, smoothStep(0.538f, 0.669f, raw))
        raw < 0.800f -> lerp(0.80f, 1f, smoothStep(0.669f, 0.800f, raw))
        else -> 1f
    }
    val settle = smoothStep(0.82f, 0.90f, raw)
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
