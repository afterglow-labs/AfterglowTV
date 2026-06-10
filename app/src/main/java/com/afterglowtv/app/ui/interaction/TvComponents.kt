package com.afterglowtv.app.ui.interaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonGlow
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceGlow
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Surface
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppShapeSet
import com.afterglowtv.app.ui.design.AppStyles
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.design.afterglowButtonShape

/**
 * Drop-in replacement for TV Material3 Surface(onClick) that automatically adds
 * [mouseClickable] to the modifier so the first finger-tap fires onClick on phones/tablets,
 * while D-pad and mouse navigation on TV remain unchanged.
 */
@Composable
fun TvClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    border: ClickableSurfaceBorder = defaultClickableSurfaceBorder(),
    scale: ClickableSurfaceScale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    glow: ClickableSurfaceGlow = ClickableSurfaceDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.mouseClickable(onClick = onClick, enabled = enabled, onLongClick = onLongClick),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        scale = scale,
        glow = glow,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Drop-in replacement for TV Material3 Button(onClick) that automatically adds
 * [mouseClickable] so the first finger-tap fires onClick on phones/tablets.
 */
@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: ButtonScale = ButtonDefaults.scale(focusedScale = 1f),
    glow: ButtonGlow = ButtonDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    shape: ButtonShape = defaultButtonShape(),
    colors: ButtonColors = defaultButtonColors(),
    border: ButtonBorder = defaultButtonBorder(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.mouseClickable(onClick = onClick, enabled = enabled),
        enabled = enabled,
        scale = scale,
        glow = glow,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Drop-in replacement for TV Material3 IconButton(onClick) that automatically adds
 * [mouseClickable] so the first finger-tap fires onClick on phones/tablets.
 */
@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    scale: ButtonScale = IconButtonDefaults.scale(focusedScale = 1f),
    glow: ButtonGlow = IconButtonDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    shape: ButtonShape = defaultIconButtonShape(),
    colors: ButtonColors = IconButtonDefaults.colors(),
    border: ButtonBorder = defaultIconButtonBorder(),
    content: @Composable BoxScope.() -> Unit,
) {
    IconButton(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.mouseClickable(onClick = onClick, enabled = enabled),
        enabled = enabled,
        scale = scale,
        glow = glow,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        border = border,
        content = content,
    )
}

@Composable
private fun defaultClickableSurfaceBorder(): ClickableSurfaceBorder =
    ClickableSurfaceDefaults.border(
        border = Border(
            border = BorderStroke(0.dp, AppColors.Outline.copy(alpha = 0f)),
            shape = RoundedCornerShape(8.dp)
        ),
        focusedBorder = Border(
            border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
            shape = RoundedCornerShape(8.dp)
        )
    )

@Composable
private fun defaultButtonBorder(): ButtonBorder =
    AppStyles.value.button.let { style ->
        val shape = afterglowButtonShape(style)
        ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = if (style == AppShapeSet.ButtonStyle.GHOST) 1.5.dp else 0.dp,
                    color = if (style == AppShapeSet.ButtonStyle.GHOST) AppColors.TiviAccent else AppColors.Outline.copy(alpha = 0f)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = if (style == AppShapeSet.ButtonStyle.DOUBLE_SHADOW) 3.dp else FocusSpec.BorderWidth,
                    color = AppColors.Focus
                ),
                shape = shape
            )
        )
    }

@Composable
private fun defaultIconButtonBorder(): ButtonBorder =
    AppStyles.value.button.let { style ->
        val shape = afterglowButtonShape(style)
        IconButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = if (style == AppShapeSet.ButtonStyle.GHOST) 1.5.dp else 0.dp,
                    color = if (style == AppShapeSet.ButtonStyle.GHOST) AppColors.TiviAccent else AppColors.Outline.copy(alpha = 0f)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = shape
            )
        )
    }

@Composable
private fun defaultButtonShape(): ButtonShape =
    ButtonDefaults.shape(shape = afterglowButtonShape(AppStyles.value.button))

@Composable
private fun defaultIconButtonShape(): ButtonShape =
    IconButtonDefaults.shape(shape = afterglowButtonShape(AppStyles.value.button))

@Composable
private fun defaultButtonColors(): ButtonColors {
    return when (AppStyles.value.button) {
        AppShapeSet.ButtonStyle.PILL,
        AppShapeSet.ButtonStyle.SHARP,
        AppShapeSet.ButtonStyle.SOFT,
        AppShapeSet.ButtonStyle.CUT_CORNER -> ButtonDefaults.colors(
            containerColor = AppColors.TiviAccent,
            focusedContainerColor = AppColors.TiviAccentLight,
            contentColor = AppColors.TiviSurfaceDeep,
            focusedContentColor = AppColors.TiviSurfaceDeep
        )
        AppShapeSet.ButtonStyle.STRIPE -> ButtonDefaults.colors(
            containerColor = AppColors.TiviSurfaceCool,
            focusedContainerColor = AppColors.SurfaceAccent,
            contentColor = AppColors.TextPrimary,
            focusedContentColor = AppColors.TextPrimary
        )
        AppShapeSet.ButtonStyle.GHOST -> ButtonDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = AppColors.TiviAccent.copy(alpha = 0.18f),
            contentColor = AppColors.TiviAccent,
            focusedContentColor = AppColors.TiviAccentLight
        )
        AppShapeSet.ButtonStyle.GLASS -> ButtonDefaults.colors(
            containerColor = AppColors.TiviAccent.copy(alpha = 0.10f),
            focusedContainerColor = AppColors.TiviAccent.copy(alpha = 0.24f),
            contentColor = AppColors.TiviAccentLight,
            focusedContentColor = AppColors.TextPrimary
        )
        AppShapeSet.ButtonStyle.DOUBLE_SHADOW -> ButtonDefaults.colors(
            containerColor = AppColors.TiviAccent,
            focusedContainerColor = AppColors.EpgNowLine,
            contentColor = AppColors.TiviSurfaceDeep,
            focusedContentColor = AppColors.TiviSurfaceDeep
        )
    }
}
