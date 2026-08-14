package gr.gtar.jobclosure.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import gr.gtar.jobclosure.desktop.ui.theme.AccentPalette
import gr.gtar.jobclosure.desktop.ui.theme.NewUiColors

/**
 * Three slow-drifting blurred colour blobs behind all content in the restyled screens - purely
 * decorative and non-interactive. Desktop port of the Android AmbientBackground; the Android
 * "reduce motion" accessibility setting doesn't exist here, so it always animates.
 */
@Composable
fun AmbientBackground(palette: AccentPalette, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ambient-bg")
    val phase1 by infinite.animateBlobPhase(18_000, "blob1")
    val phase2 by infinite.animateBlobPhase(24_000, "blob2")
    val phase3 by infinite.animateBlobPhase(30_000, "blob3")

    BoxWithConstraints(modifier.fillMaxSize().background(NewUiColors.ground)) {
        val w = maxWidth
        val h = maxHeight

        AmbientBlob(
            color = palette.blob1,
            width = w * 0.70f,
            height = h * 0.42f,
            alignment = Alignment.TopStart,
            offsetX = -w * 0.18f,
            offsetY = -h * 0.10f,
            blurRadius = 42.dp,
            translateFrom = Offset(-0.06f, -0.04f),
            translateTo = Offset(0.12f, 0.08f),
            scaleFrom = 1.0f,
            scaleTo = 1.18f,
            phase = phase1,
        )
        AmbientBlob(
            color = palette.blob2,
            width = w * 0.62f,
            height = h * 0.38f,
            alignment = Alignment.TopEnd,
            offsetX = w * 0.16f,
            offsetY = h * 0.22f,
            blurRadius = 46.dp,
            translateFrom = Offset(0.08f, 0.06f),
            translateTo = Offset(-0.10f, -0.08f),
            scaleFrom = 1.10f,
            scaleTo = 0.92f,
            phase = phase2,
        )
        AmbientBlob(
            color = palette.blob3,
            width = w * 0.72f,
            height = h * 0.36f,
            alignment = Alignment.BottomStart,
            offsetX = -w * 0.08f,
            offsetY = h * 0.10f,
            blurRadius = 50.dp,
            translateFrom = Offset(0.12f, 0.08f),
            translateTo = Offset(-0.06f, -0.04f),
            scaleFrom = 1.18f,
            scaleTo = 1.0f,
            phase = phase3,
        )
    }
}

@Composable
private fun InfiniteTransition.animateBlobPhase(durationMillis: Int, label: String) =
    animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = label,
    )

@Composable
private fun BoxScope.AmbientBlob(
    color: Color,
    width: Dp,
    height: Dp,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    blurRadius: Dp,
    translateFrom: Offset,
    translateTo: Offset,
    scaleFrom: Float,
    scaleTo: Float,
    phase: Float,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(width, height)
            .graphicsLayer {
                translationX = lerp(translateFrom.x, translateTo.x, phase) * size.width
                translationY = lerp(translateFrom.y, translateTo.y, phase) * size.height
                val s = lerp(scaleFrom, scaleTo, phase)
                scaleX = s
                scaleY = s
            }
            .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .background(
                Brush.radialGradient(colorStops = arrayOf(0f to color, 0.7f to Color.Transparent)),
                CircleShape,
            ),
    )
}
