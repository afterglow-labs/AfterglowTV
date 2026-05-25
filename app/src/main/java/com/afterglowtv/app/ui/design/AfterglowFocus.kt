package com.afterglowtv.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

enum class AfterglowFocusRole { Row, Card, Pill }

/**
 * AfterglowTV focus highlight in one place: a solid accent border at 4 dp corner radius,
 * filled with a translucent accent over the dark surface. 150 ms crossfade.
 *
 *  - Row:  flat (no scale), full border + fill.
 *  - Card: 1.04x lift, full border + fill.
 *  - Pill: flat, full border + fill on a rounded-pill shape.
 *
 * Caller owns the size/shape of its container. Apply this *before* any
 * `clip` / `background` modifiers — order matters in Compose.
 */
fun Modifier.afterglowFocus(
    role: AfterglowFocusRole,
    shape: Shape = RoundedCornerShape(FocusSpec.CornerRadius),
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val targetScale = when {
        !focused -> 1f
        role == AfterglowFocusRole.Card -> FocusSpec.CardFocusedScale
        else -> FocusSpec.RowFocusedScale
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = AppMotion.FocusFade,
        label = "afterglow-focus-scale",
    )
    val fillColor by animateColorAsState(
        targetValue = if (focused) AppColors.FocusFill else Color.Transparent,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "afterglow-focus-fill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) AppColors.Focus else Color.Transparent,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "afterglow-focus-border",
    )

    val style = AppStyles.value.focus
    val glowSpecs = if (focused && style == AppShapeSet.FocusStyle.GLOW) Glows.focus else emptyList()

    this
        .scale(scale)
        .afterglow(glowSpecs, shape)
        .focusable(enabled = true, interactionSource = interaction)
        .background(fillColor, shape)
        .border(width = FocusSpec.BorderWidth, color = borderColor, shape = shape)
}
