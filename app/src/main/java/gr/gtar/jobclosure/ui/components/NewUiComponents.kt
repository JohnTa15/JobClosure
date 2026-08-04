package gr.gtar.jobclosure.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import gr.gtar.jobclosure.ui.theme.NewUiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared easing for screen transitions, entrances and expand/collapse - cubic-bezier(.2,.8,.2,1). */
val NewDesignEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Shared building blocks for the restyled ("new design") screens - see
 * design_handoff_theme_switcher/README.md. Kept separate from the classic-UI screens so the old
 * look stays untouched; only rendered when Settings > Νέα εμφάνιση is on.
 */

/** Places a soft blurred glow of [glowColor] behind [content], clipped to [shape]. */
@Composable
fun GlowBox(
    glowColor: Color,
    blurRadius: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(glowColor, shape),
        )
        content()
    }
}

@Composable
fun NewSectionLabel(text: String, color: Color = NewUiColors.onGroundFaint, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        letterSpacing = 0.14.em,
        modifier = modifier,
    )
}

/** 44x44 (or custom size) bordered square icon button used for back/settings/palette buttons. */
@Composable
fun NewIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    borderColor: Color = NewUiColors.outline,
    containerColor: Color = Color(0x99232532),
    iconColor: Color = NewUiColors.onGroundMuted,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconColor, modifier = Modifier.size(iconSize))
    }
}

/** The pill/rect accent button used for the FAB, save button, "open route" button, etc. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    borderColor: Color,
    containerColor: Color,
    contentColor: Color,
    glowColor: Color,
    glowRadius: Dp = 28.dp,
    height: Dp = 44.dp,
    fillWidth: Boolean = true,
    fontSize: TextUnit = 14.sp,
) {
    GlowBox(glowColor = glowColor, blurRadius = glowRadius, shape = shape, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .height(height)
                .clip(shape)
                .background(containerColor)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(text, color = contentColor, fontSize = fontSize, fontWeight = FontWeight.Medium)
        }
    }
}

/** Pill-shaped attribute/type chip (drone, reception, type label, fact pill...). */
@Composable
fun NewChip(
    text: String,
    textColor: Color,
    borderColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontSize: TextUnit = 11.sp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
        }
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Medium)
    }
}

/** Custom 46x26dp glowing-track switch used in the edit screen's switch group. */
@Composable
fun NewSwitch(checked: Boolean, onColor: Color, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) onColor else NewUiColors.outline,
        animationSpec = tween(250),
        label = "switch-track",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(250),
        label = "switch-knob",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = knobOffset)
                .clip(CircleShape)
                .background(NewUiColors.ground),
        )
    }
}

/** Selectable colour-swatch chip used by the type picker / theme picker. */
@Composable
fun RowScope.NewSelectableSwatch(
    label: String,
    selected: Boolean,
    swatchBrush: Brush,
    accentColor: Color,
    accentBorder: Color,
    accentContainer: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentContainer else Color(0x80232532))
            .border(1.dp, if (selected) accentBorder else NewUiColors.outlineSoft, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(swatchBrush))
            Text(
                label,
                color = if (selected) accentColor else NewUiColors.onGroundDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Staggered rise-and-fade entrance for list items - opacity 0->1, translateY 14dp->0, 500ms,
 *  staggered 70ms per [index], capped at ~6 items so long lists don't crawl in one by one. */
@Composable
fun NewListEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val offsetYDp = remember { Animatable(14f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(6) * 70).toLong())
        launch { alpha.animateTo(1f, tween(500, easing = NewDesignEasing)) }
        launch { offsetYDp.animateTo(0f, tween(500, easing = NewDesignEasing)) }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetYDp.value.dp.toPx()
        },
    ) {
        content()
    }
}
