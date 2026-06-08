package com.afterglowtv.app.ui.components.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow

private val VoxRoundWideFontFamily = FontFamily(
    Font(R.font.vox_round_wide_semibold, FontWeight.SemiBold)
)

private const val AFTERGLOW_DARK_2_THEME_ID = "afterglow_sunset"

/**
 * Shared layered backdrop used by every Afterglow-branded screen:
 * blended surface mesh with warm and cool washes. Lives behind the brand strip
 * and screen content.
 *
 * Use as the first child inside a `Box(Modifier.fillMaxSize())`.
 */
@Composable
fun AfterglowBackdrop(modifier: Modifier = Modifier) {
    val gradientsEnabled = AppColors.backgroundGradientsEnabled
    val isAfterglowDark2 = AppColors.palette.id == AFTERGLOW_DARK_2_THEME_ID
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (gradientsEnabled) {
                        Modifier.background(
                            if (isAfterglowDark2) {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF050914),
                                        Color(0xFF0D1424),
                                        Color(0xFF221545),
                                        Color(0xFF263A5A),
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(2600f, 1800f),
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        AppColors.TiviSurfaceDeep,
                                        AppColors.TiviSurfaceBase,
                                        AppColors.TiviSurfaceCool,
                                        AppColors.TiviSurfaceAccent,
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(2600f, 1800f),
                                )
                            }
                        )
                    } else {
                        Modifier.background(AppColors.TiviSurfaceDeep)
                    }
                )
        )
        if (gradientsEnabled) {
            if (isAfterglowDark2) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.TiviAccent.copy(alpha = AppColors.palette.glowAlpha(0.26f)),
                                AppColors.TiviAccent.copy(alpha = AppColors.palette.glowAlpha(0.10f)),
                                AppColors.TiviAccent.copy(alpha = 0f),
                            ),
                            center = Offset(2300f, -80f),
                            radius = 1500f,
                        )
                    )
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8A4DFF).copy(alpha = AppColors.palette.glowAlpha(0.24f)),
                                Color(0xFF5D2BCB).copy(alpha = AppColors.palette.glowAlpha(0.11f)),
                                Color(0xFF5D2BCB).copy(alpha = 0f),
                            ),
                            center = Offset(220f, 1560f),
                            radius = 1500f,
                        )
                    )
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.EpgNowLine.copy(alpha = AppColors.palette.glowAlpha(0.16f)),
                                AppColors.TiviAccent.copy(alpha = AppColors.palette.glowAlpha(0.07f)),
                                AppColors.EpgNowLine.copy(alpha = 0f),
                            ),
                            center = Offset(1500f, 1700f),
                            radius = 1250f,
                        )
                    )
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.TiviSurfaceAccent.copy(alpha = AppColors.palette.glowAlpha(0.42f)),
                                AppColors.TiviSurfaceBase.copy(alpha = AppColors.palette.glowAlpha(0.20f)),
                                AppColors.TiviSurfaceAccent.copy(alpha = 0f),
                            ),
                            center = Offset(2200f, 120f),
                            radius = 1900f,
                        )
                    )
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.TiviSurfaceCool.copy(alpha = AppColors.palette.glowAlpha(0.36f)),
                                AppColors.TiviSurfaceBase.copy(alpha = AppColors.palette.glowAlpha(0.12f)),
                                AppColors.TiviSurfaceCool.copy(alpha = 0f),
                            ),
                            center = Offset(160f, 1600f),
                            radius = 1600f,
                        )
                    )
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.TiviSurfaceBase.copy(alpha = AppColors.palette.glowAlpha(0.24f)),
                                AppColors.TiviSurfaceBase.copy(alpha = 0f),
                            ),
                            center = Offset(1100f, 620f),
                            radius = 1450f,
                        )
                    )
                )
            }
        }
    }
}

/**
 * Compact horizontal brand strip — logo + "Afterglow TV / [wordmark]" + tagline.
 *
 * Use at the top of a screen's content area to anchor the Afterglow TV identity
 * without consuming much vertical space. ~88dp tall.
 *
 * For full-screen splash/settings hero, use [AfterglowHero] instead.
 *
 * @param wordmark The accent half of the title (e.g. "Themes", "VOD",
 *     "Series", "Live TV", "Guide", "Settings"). Rendered in accent color
 *     beside "Afterglow TV".
 * @param tagline One-line subtitle in Afterglow voice.
 * @param logoSize 40-64dp. Defaults to 48dp for compact use.
 */
@Composable
fun AfterglowBrandStrip(
    wordmark: String,
    tagline: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = 48.dp,
    showBrandName: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.afterglow_logo),
            contentDescription = "Afterglow TV",
            modifier = Modifier
                .size(logoSize)
                .afterglow(
                    specs = listOf(
                        GlowSpec(AppColors.TiviAccent, 14.dp, 0.50f),
                        GlowSpec(AppColors.EpgNowLine, 24.dp, 0.28f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp)),
        )
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                if (showBrandName) {
                    AfterglowWordmarkText(
                        text = "Afterglow TV",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = TextUnit(28f, TextUnitType.Sp),
                        ),
                    )
                    Spacer(Modifier.size(10.dp))
                }
                val wordmarkStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextUnit(if (showBrandName) 24f else 28f, TextUnitType.Sp),
                )
                val wordmarkModifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .afterglow(
                        specs = listOf(GlowSpec(AppColors.TiviAccent, 8.dp, 0.45f)),
                    )
                if (wordmark.isAfterglowWordmark()) {
                    AfterglowWordmarkText(
                        text = "Afterglow TV",
                        style = wordmarkStyle.copy(
                            fontWeight = FontWeight.Bold,
                        )
                    )
                } else {
                    Text(
                        text = wordmark,
                        style = wordmarkStyle,
                        color = AppColors.TiviAccent,
                        modifier = wordmarkModifier,
                    )
                }
            }
            if (tagline.isNotBlank()) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TiviAccentLight,
                )
            }
        }
    }
}

/**
 * Full-screen hero block — [AfterglowBackdrop] + centered logo + large
 * "Afterglow TV / [wordmark]" + tagline. For Welcome and major settings screens
 * that should feel premium and identity-forward.
 *
 * Pass [content] to render slot children below the hero header (e.g. a
 * progress indicator on Welcome).
 */
@Composable
fun AfterglowHero(
    wordmark: String,
    tagline: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = 96.dp,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        AfterglowBackdrop()
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.afterglow_logo),
                contentDescription = "Afterglow TV",
                modifier = Modifier
                    .size(logoSize)
                    .afterglow(
                        specs = listOf(
                            GlowSpec(AppColors.TiviAccent, 28.dp, 0.60f),
                            GlowSpec(AppColors.EpgNowLine, 48.dp, 0.36f),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .clip(RoundedCornerShape(24.dp)),
            )
            Spacer(Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AfterglowWordmarkText(
                    text = "Afterglow TV",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(52f, TextUnitType.Sp),
                    ),
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = wordmark,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = TextUnit(44f, TextUnitType.Sp),
                    ),
                    color = AppColors.TiviAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .afterglow(
                            specs = listOf(GlowSpec(AppColors.TiviAccent, 12.dp, 0.55f)),
                        ),
                )
            }
            if (!tagline.isNullOrBlank()) {
                Spacer(Modifier.size(10.dp))
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TiviAccentLight,
                )
            }
            content()
        }
    }
}

@Composable
private fun AfterglowWordmarkText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = AppColors.TextPrimary,
) {
    val glowStart = text.indexOf("glow", ignoreCase = true)
    val tvStart = text.indexOf("TV", startIndex = glowStart.coerceAtLeast(0), ignoreCase = true)
    if (glowStart < 0) {
        Text(
            text = text,
            style = style,
            color = color,
            modifier = modifier,
        )
        return
    }

    val glowEnd = glowStart + "glow".length
    val neonOrange = Color(0xFFFF6A00)
    val neonPink = Color(0xFFFF2D8D)
    val tvEnd = if (tvStart >= glowEnd) tvStart + "TV".length else -1
    val wordEnd = if (tvEnd > 0) tvEnd else glowEnd
    val wordmarkStyle = style.copy(fontFamily = VoxRoundWideFontFamily)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            0.00f to neonPink,
                            0.58f to Color(0xFFFF5A2A),
                            1.00f to neonOrange,
                            start = Offset.Zero,
                            end = Offset(620f, 0f),
                        )
                    )
                ) {
                    append(text.substring(0, wordEnd))
                }
            },
            style = wordmarkStyle.copy(
                shadow = Shadow(
                    color = neonOrange.copy(alpha = 0.24f),
                    offset = Offset.Zero,
                    blurRadius = 8f,
                )
            ),
            color = color,
        )
        if (wordEnd < text.length) {
            Text(
                text = text.substring(wordEnd),
                style = wordmarkStyle,
                color = color,
            )
        }
    }
}

private fun String.isAfterglowWordmark(): Boolean =
    filterNot { it.isWhitespace() }.equals("AfterglowTV", ignoreCase = true)
