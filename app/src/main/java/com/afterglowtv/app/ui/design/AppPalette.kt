package com.afterglowtv.app.ui.design

import androidx.compose.ui.graphics.Color

/**
 * A bundled theme identity: backgrounds, accents, semantics, text — everything
 * that gives AfterglowTV a distinct visual character.
 *
 * Themes are intentionally **not just hex swaps**: they pair backgrounds with
 * accents that have a coherent mood (cobalt slate + neon peach; pitch-black +
 * electric blue; warm ember + pink; etc.) and shift the `nowLine` and `live`
 * indicator hue together so the EPG still pops without clashing.
 *
 * Future work: extend with corner-radius scale, density factor, focus-highlight
 * style enum, and typography weight so themes diverge in dimension/density too.
 */
data class AppPalette(
    val id: String,
    val displayName: String,
    val description: String,

    // Surfaces — darkest → lightest
    val surfaceDeep: Color,
    val surfaceBase: Color,
    val surfaceCool: Color,
    val surfaceAccent: Color,

    // Accent family
    val accent: Color,
    val accentLight: Color,
    val accentMuted: Color,

    // Scrims
    val panelScrim: Color,
    val osdScrim: Color,

    // EPG / live indicator
    val nowLine: Color,
    val nowFill: Color,
    val live: Color,

    // PiP / framing
    val pipPreviewOutline: Color,
    val focusFill: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // Semantics
    val success: Color,
    val warning: Color,
    val info: Color,

    // Lines
    val divider: Color,
    val outline: Color,
    val glowIntensity: Float = 1f,
) {
    fun glowAlpha(alpha: Float): Float = (alpha * glowIntensity).coerceIn(0f, 1f)

    companion object {
        /** Default. Cobalt slate darks, blue accent, hot-magenta now-line. Modern, neon-leaning. */
        val NeonDusk = AppPalette(
            id = "neon_dusk",
            displayName = "Neon Dusk",
            description = "Cobalt slate darks, electric blue accent, and magenta now-line.",
            surfaceDeep = Color(0xFF060A14),
            surfaceBase = Color(0xFF101A32),
            surfaceCool = Color(0xFF22355A),
            surfaceAccent = Color(0xFF314C78),
            accent = Color(0xFF4E8DFF),
            accentLight = Color(0xFFA9C8FF),
            accentMuted = Color(0x664E8DFF),
            panelScrim = Color(0xCC080B12),
            osdScrim = Color(0x99080B12),
            nowLine = Color(0xFFFF3D7F),
            nowFill = Color(0x334E8DFF),
            live = Color(0xFFFF3D7F),
            pipPreviewOutline = Color(0xFF4E8DFF),
            focusFill = Color(0x334E8DFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xB3EEEEEE),
            textTertiary = Color(0x80EEEEEE),
            textDisabled = Color(0x4DEEEEEE),
            success = Color(0xFF5E9CFF),
            warning = Color(0xFFFFD166),
            info = Color(0xFFA9C8FF),
            divider = Color(0x1AFFFFFF),
            outline = Color(0x334E8DFF),
            glowIntensity = 0.35f,
        )

        /** Blue-gray steel surfaces with cool blue focus and softened chrome edges. */
        val BlueSteel = AppPalette(
            id = "blue_steel",
            displayName = "Blue Steel",
            description = "Layered blue-gray steel with cool chrome highlights and soft signal-blue edges.",
            surfaceDeep = Color(0xFF112A3C),
            surfaceBase = Color(0xFF163044),
            surfaceCool = Color(0xFF2D536A),
            surfaceAccent = Color(0xFF4C6E84),
            accent = Color(0xFF89A8BC),
            accentLight = Color(0xFFD1DEE8),
            accentMuted = Color(0x6689A8BC),
            panelScrim = Color(0xE613212B),
            osdScrim = Color(0xB313212B),
            nowLine = Color(0xFFB8CEDD),
            nowFill = Color(0x3389A8BC),
            live = Color(0xFFB8CEDD),
            pipPreviewOutline = Color(0xFFD1DEE8),
            focusFill = Color(0x3389A8BC),
            textPrimary = Color(0xFFF5FAFF),
            textSecondary = Color(0xD9D8E7F0),
            textTertiary = Color(0x99D8E7F0),
            textDisabled = Color(0x66D8E7F0),
            success = Color(0xFF9FC3D6),
            warning = Color(0xFFE8B468),
            info = Color(0xFFD1DEE8),
            divider = Color(0x2289A8BC),
            outline = Color(0x6689A8BC),
            glowIntensity = 0.35f,
        )

        /** Rachel's Sunset — pastel mint, cream, neon peach, and coral-pink glow. */
        val SunsetAurora = AppPalette(
            id = "sunset_aurora",
            displayName = "Rachel's Sunset",
            description = "I love you Rachel! Mint teal, girly blue, neon peach-orange, hot pink, and violet sunset glow.",
            surfaceDeep = Color(0xFF062735),
            surfaceBase = Color(0xFF135D70),
            surfaceCool = Color(0xFF5F55D8),
            surfaceAccent = Color(0xFF63F2C7),
            accent = Color(0xFFFF8A2A),
            accentLight = Color(0xFFFFC66D),
            accentMuted = Color(0x66FF8A2A),
            panelScrim = Color(0xD6062735),
            osdScrim = Color(0xA6062735),
            nowLine = Color(0xFFFF4FA3),
            nowFill = Color(0x33FF8A2A),
            live = Color(0xFFFF6FB8),
            pipPreviewOutline = Color(0xFFFF8A2A),
            focusFill = Color(0x40FF8A2A),
            textPrimary = Color(0xFFFFF7E9),
            textSecondary = Color(0xD9FCEADC),
            textTertiary = Color(0x99FCEADC),
            textDisabled = Color(0x66FCEADC),
            success = Color(0xFF63F2C7),
            warning = Color(0xFFFFC66D),
            info = Color(0xFF82D8FF),
            divider = Color(0x22FFF7E9),
            outline = Color(0x66FF8A2A),
            glowIntensity = 1f,
        )

        /** Calm cobalt: deep slate-blue with amber now-line. Easy on the eyes. */
        val ForestMist = AppPalette(
            id = "forest_mist",
            displayName = "Cobalt Mist",
            description = "Deep slate-blue with a calm cobalt accent. Easy on the eyes for long sessions.",
            surfaceDeep = Color(0xFF041020),
            surfaceBase = Color(0xFF0A2448),
            surfaceCool = Color(0xFF1A4270),
            surfaceAccent = Color(0xFF2B5D93),
            accent = Color(0xFF5C8DFF),
            accentLight = Color(0xFFAFC8FF),
            accentMuted = Color(0x665C8DFF),
            panelScrim = Color(0xCC07101E),
            osdScrim = Color(0x9907101E),
            nowLine = Color(0xFFFFD166),
            nowFill = Color(0x335C8DFF),
            live = Color(0xFFFFD166),
            pipPreviewOutline = Color(0xFF5C8DFF),
            focusFill = Color(0x335C8DFF),
            textPrimary = Color(0xFFF2F7FF),
            textSecondary = Color(0xCCDCE8FF),
            textTertiary = Color(0x99DCE8FF),
            textDisabled = Color(0x66DCE8FF),
            success = Color(0xFF5C8DFF),
            warning = Color(0xFFFFD166),
            info = Color(0xFFAFC8FF),
            divider = Color(0x1ADCE8FF),
            outline = Color(0x335C8DFF),
            glowIntensity = 0.35f,
        )

        /** Minimal mono: near-black + clean whites + grey accents. For minimalists. */
        val PureOnyx = AppPalette(
            id = "pure_onyx",
            displayName = "Pure Onyx",
            description = "Near-black with clean whites and minimal grey accents. Minimalist, content-first.",
            surfaceDeep = Color(0xFF050505),
            surfaceBase = Color(0xFF0F0F0F),
            surfaceCool = Color(0xFF1A1A1A),
            surfaceAccent = Color(0xFF252525),
            accent = Color(0xFFE8E8E8),
            accentLight = Color(0xFFFFFFFF),
            accentMuted = Color(0x66E8E8E8),
            panelScrim = Color(0xE6000000),
            osdScrim = Color(0xB3000000),
            nowLine = Color(0xFFE5484D),
            nowFill = Color(0x33FFFFFF),
            live = Color(0xFFE5484D),
            pipPreviewOutline = Color(0xFFE8E8E8),
            focusFill = Color(0x33FFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xCCFFFFFF),
            textTertiary = Color(0x99FFFFFF),
            textDisabled = Color(0x66FFFFFF),
            success = Color(0xFF7FA7FF),
            warning = Color(0xFFFFD166),
            info = Color(0xFFE8E8E8),
            divider = Color(0x1AFFFFFF),
            outline = Color(0x33FFFFFF),
            glowIntensity = 0.35f,
        )

        /** Classic blue IPTV palette. */
        val ClassicBlue = AppPalette(
            id = "classic_blue",
            displayName = "Classic Blue",
            description = "Layered navy and broadcast blue chrome with crisp red live markers and restrained glow.",
            surfaceDeep = Color(0xFF04162E),
            surfaceBase = Color(0xFF092B56),
            surfaceCool = Color(0xFF14508B),
            surfaceAccent = Color(0xFF2372B7),
            accent = Color(0xFF2196F3),
            accentLight = Color(0xFF90CAF9),
            accentMuted = Color(0x662196F3),
            panelScrim = Color(0xD6061B33),
            osdScrim = Color(0xA6061B33),
            nowLine = Color(0xFFF44336),
            nowFill = Color(0x602196F3),
            live = Color(0xFFF44336),
            pipPreviewOutline = Color(0xFF2196F3),
            focusFill = Color(0x402196F3),
            textPrimary = Color(0xFFF4FAFF),
            textSecondary = Color(0xCCD4E9FF),
            textTertiary = Color(0x99D4E9FF),
            textDisabled = Color(0x66D4E9FF),
            success = Color(0xFF4FD39A),
            warning = Color(0xFFFFC107),
            info = Color(0xFF2DAAE2),
            divider = Color(0x1AF4F8FF),
            outline = Color(0x264C6D95),
            glowIntensity = 0.35f,
        )

        /** Synthwave — black laser grid with hot pink and yellow. Distinct from Afterglow. */
        val Synthwave = AppPalette(
            id = "synthwave",
            displayName = "Synthwave",
            description = "Black laser grid with hot pink, sun yellow, and cobalt edge light.",
            surfaceDeep = Color(0xFF02020A),
            surfaceBase = Color(0xFF150824),
            surfaceCool = Color(0xFF231B55),
            surfaceAccent = Color(0xFF203C7A),
            accent = Color(0xFFFF2F88),
            accentLight = Color(0xFFFFD447),
            accentMuted = Color(0x66FF2A6D),
            panelScrim = Color(0xCC020305),
            osdScrim = Color(0x99020305),
            nowLine = Color(0xFFFFD447),
            nowFill = Color(0x33FF2F88),
            live = Color(0xFFFFD447),
            pipPreviewOutline = Color(0xFF4E8DFF),
            focusFill = Color(0x33FF2F88),
            textPrimary = Color(0xFFFFF2F8),
            textSecondary = Color(0xCCD7E4FF),
            textTertiary = Color(0x99D7E4FF),
            textDisabled = Color(0x66D7E4FF),
            success = Color(0xFF4E8DFF),
            warning = Color(0xFFFFD447),
            info = Color(0xFFFF6EA8),
            divider = Color(0x22D7E4FF),
            outline = Color(0x444E8DFF),
            glowIntensity = 0.35f,
        )

        /** Vaporwave — warm chrome sunset with no purple-family base. */
        val Vaporwave = AppPalette(
            id = "vaporwave",
            displayName = "Vaporwave",
            description = "Chrome charcoal with neon sunset orange, hot pink, and gold.",
            surfaceDeep = Color(0xFF0C0A0C),
            surfaceBase = Color(0xFF2A1622),
            surfaceCool = Color(0xFF4A2B2A),
            surfaceAccent = Color(0xFF6B4933),
            accent = Color(0xFFFF7A38),
            accentLight = Color(0xFFFFC44A),
            accentMuted = Color(0x66FF7A38),
            panelScrim = Color(0xCC12110F),
            osdScrim = Color(0x9912110F),
            nowLine = Color(0xFFFF3DAF),
            nowFill = Color(0x33FF7A38),
            live = Color(0xFFFF3DAF),
            pipPreviewOutline = Color(0xFFFF477E),
            focusFill = Color(0x40FF7A38),
            textPrimary = Color(0xFFFFF7E8),
            textSecondary = Color(0xCCFFE8C7),
            textTertiary = Color(0x99FFE8C7),
            textDisabled = Color(0x66FFE8C7),
            success = Color(0xFF4E8DFF),
            warning = Color(0xFFFFC44A),
            info = Color(0xFFFFB47A),
            divider = Color(0x22FFE8C7),
            outline = Color(0x40FF7A38),
            glowIntensity = 0.35f,
        )

        /** Afterglow Gray — direct port of the user's favorite YTAfterglow preset.
         *  Charcoal-monochrome: pure grayscale luminance steps, no chroma. Pure
         *  white accent on `#2E2E2E` body. The Lite-inspired "soft white controls
         *  on charcoal" look from the iOS YouTube tweak, applied to a TV grid. */
        val AfterglowGray = AppPalette(
            id = "afterglow_gray",
            displayName = "Afterglow Gray",
            description = "Charcoal monochrome — pure white controls on `#2E2E2E` body. The Lite-inspired YT favorite.",
            surfaceDeep = Color(0xFF242424),         // YTAG-inspired charcoal
            surfaceBase = Color(0xFF353535),
            surfaceCool = Color(0xFF505050),
            surfaceAccent = Color(0xFF6A6A6A),
            accent = Color(0xFFFFFFFF),              // YTAG accent — pure white
            accentLight = Color(0xFFFAFAFA),         // YTAG overlay
            accentMuted = Color(0x66FFFFFF),
            panelScrim = Color(0xCC1F1F1F),
            osdScrim = Color(0x991F1F1F),
            nowLine = Color(0xFFFFFFFF),             // pure-white now-line pops on `#2E2E2E`
            nowFill = Color(0x33FFFFFF),
            live = Color(0xFFFFFFFF),
            pipPreviewOutline = Color(0xFFFFFFFF),
            focusFill = Color(0x33FFFFFF),
            textPrimary = Color(0xFFF5F5F5),         // YTAG textP (white 0.96)
            textSecondary = Color(0xFFC7C7C7),       // YTAG textS (white 0.78)
            textTertiary = Color(0x99F5F5F5),
            textDisabled = Color(0x66F5F5F5),
            success = Color(0xFFFFFFFF),             // monochrome keeps semantics in luminance only
            warning = Color(0xFFC7C7C7),
            info = Color(0xFFEBEBEB),                // YTAG seekBar
            divider = Color(0x22FFFFFF),
            outline = Color(0x33FFFFFF),
        )

        /** Afterglow Dark 2 — default first-run palette. */
        val AfterglowSunset = AppPalette(
            id = "afterglow_sunset",
            displayName = "Afterglow Dark 2",
            description = "Cobalt-black with neon orange and hot-pink Afterglow accents.",
            surfaceDeep = Color(0xFF050914),
            surfaceBase = Color(0xFF0D1424),
            surfaceCool = Color(0xFF172238),
            surfaceAccent = Color(0xFF263A5A),
            accent = Color(0xFFFF7A18),
            accentLight = Color(0xFFFFB15F),
            accentMuted = Color(0x66FF7A18),
            panelScrim = Color(0xCC050914),
            osdScrim = Color(0x99050914),
            nowLine = Color(0xFFFF2D7A),
            nowFill = Color(0x33FF7A18),
            live = Color(0xFFFF2D7A),
            pipPreviewOutline = Color(0xFFFF7A18),
            focusFill = Color(0x40FF7A18),
            textPrimary = Color(0xFFFFF4D2),
            textSecondary = Color(0xD9FFC285),
            textTertiary = Color(0x99FFC285),
            textDisabled = Color(0x66FFC285),
            success = Color(0xFFFFB15F),
            warning = Color(0xFFFF7A18),
            info = Color(0xFFFF7DB0),
            divider = Color(0x22FFE4D8),
            outline = Color(0x55FF7A18),
        )

        /** Afterglow 1 — cool graphite with blue-steel controls. */
        val Afterglow1 = AppPalette(
            id = "afterglow_1",
            displayName = "Afterglow Dark 1",
            description = "Graphite-black surfaces with blue-steel focus and frosted signal-blue live markers.",
            surfaceDeep = Color(0xFF05080B),
            surfaceBase = Color(0xFF0D141A),
            surfaceCool = Color(0xFF16212A),
            surfaceAccent = Color(0xFF263646),
            accent = Color(0xFF6F8FA8),
            accentLight = Color(0xFFB7C9D8),
            accentMuted = Color(0x666F8FA8),
            panelScrim = Color(0xD605080B),
            osdScrim = Color(0x9905080B),
            nowLine = Color(0xFF9DB8CC),
            nowFill = Color(0x336F8FA8),
            live = Color(0xFF9DB8CC),
            pipPreviewOutline = Color(0xFFB7C9D8),
            focusFill = Color(0x406F8FA8),
            textPrimary = Color(0xFFDCEEFF),
            textSecondary = Color(0xD9AFC7D8),
            textTertiary = Color(0x99AFC7D8),
            textDisabled = Color(0x66AFC7D8),
            success = Color(0xFF8FB2CC),
            warning = Color(0xFFE4B66B),
            info = Color(0xFFB7C9D8),
            divider = Color(0x225A748A),
            outline = Color(0x556F8FA8),
        )

        /** Afterglow 3 — deep violet glass with magenta energy. */
        val Afterglow3 = AppPalette(
            id = "afterglow_3",
            displayName = "Afterglow Dark 3",
            description = "Black-violet surfaces with electric purple focus and magenta live edge.",
            surfaceDeep = Color(0xFF0A050F),
            surfaceBase = Color(0xFF150B20),
            surfaceCool = Color(0xFF241333),
            surfaceAccent = Color(0xFF3A2052),
            accent = Color(0xFFB56BFF),
            accentLight = Color(0xFFD8B8FF),
            accentMuted = Color(0x66B56BFF),
            panelScrim = Color(0xD60A050F),
            osdScrim = Color(0x990A050F),
            nowLine = Color(0xFFFF62C7),
            nowFill = Color(0x33B56BFF),
            live = Color(0xFFFF62C7),
            pipPreviewOutline = Color(0xFFD8B8FF),
            focusFill = Color(0x40B56BFF),
            textPrimary = Color(0xFFF0E4FF),
            textSecondary = Color(0xD9C8A7FF),
            textTertiary = Color(0x99C8A7FF),
            textDisabled = Color(0x66C8A7FF),
            success = Color(0xFFD8B8FF),
            warning = Color(0xFFFFC06D),
            info = Color(0xFFFF8AD7),
            divider = Color(0x22D8C7E8),
            outline = Color(0x55B56BFF),
        )

        /** Afterglow 4 — red-black ember with gold signal color. */
        val Afterglow4 = AppPalette(
            id = "afterglow_4",
            displayName = "Afterglow Dark 4",
            description = "Near-black oxblood surfaces with ember-red focus and warm gold live markers.",
            surfaceDeep = Color(0xFF080303),
            surfaceBase = Color(0xFF170909),
            surfaceCool = Color(0xFF261111),
            surfaceAccent = Color(0xFF3F1B16),
            accent = Color(0xFFFF4D36),
            accentLight = Color(0xFFFF9A76),
            accentMuted = Color(0x66FF4D36),
            panelScrim = Color(0xE6080303),
            osdScrim = Color(0xAA080303),
            nowLine = Color(0xFFFFC857),
            nowFill = Color(0x33FF4D36),
            live = Color(0xFFFFC857),
            pipPreviewOutline = Color(0xFFFF9A76),
            focusFill = Color(0x45FF4D36),
            textPrimary = Color(0xFFFFD1BE),
            textSecondary = Color(0xD9FF9F85),
            textTertiary = Color(0x99FF9F85),
            textDisabled = Color(0x66FF9F85),
            success = Color(0xFFFFC857),
            warning = Color(0xFFFF9A76),
            info = Color(0xFFFFD97F),
            divider = Color(0x22E8B6A8),
            outline = Color(0x55FF4D36),
        )

        // ── Light palettes ───────────────────────────────────────────
        // The light themes are intentionally not white themes. They use
        // smoky, sunset-tinted surfaces with dark text so TV menus keep real
        // contrast instead of turning into a bright flat sheet.

        /** Afterglow Gray Light — pale charcoal monochrome (the inverse of
         *  Afterglow Gray Dark). Bright surfaces, dark controls. */
        val AfterglowGrayLight = AppPalette(
            id = "afterglow_gray_light",
            displayName = "Afterglow Gray Light",
            description = "Soft ash monochrome — dark controls on a muted gray body.",
            surfaceDeep = Color(0xFFB8B8B8),
            surfaceBase = Color(0xFFD0D0D0),
            surfaceCool = Color(0xFFEAEAEA),
            surfaceAccent = Color(0xFFA2A2A2),
            accent = Color(0xFF202020),
            accentLight = Color(0xFF464646),
            accentMuted = Color(0x66333333),
            panelScrim = Color(0xCC1F1F1F),
            osdScrim = Color(0x991F1F1F),
            nowLine = Color(0xFF2C2C2C),
            nowFill = Color(0x33333333),
            live = Color(0xFF202020),
            pipPreviewOutline = Color(0xFF202020),
            focusFill = Color(0x33333333),
            textPrimary = Color(0xFF111111),
            textSecondary = Color(0xFF3D3D3D),
            textTertiary = Color(0x993D3D3D),
            textDisabled = Color(0x663D3D3D),
            success = Color(0xFF202020),
            warning = Color(0xFF4A4A4A),
            info = Color(0xFF2C2C2C),
            divider = Color(0x22000000),
            outline = Color(0x33333333),
        )

        /** Afterglow Light 1 — smoky warm gray, orange-pink glow, no whiteout. */
        val AfterglowLight1 = AppPalette(
            id = "afterglow_light_1",
            displayName = "Afterglow Light 1",
            description = "Smoky warm gray glass with orange controls, pink live glow, and medium mint highlights.",
            surfaceDeep = Color(0xFFBDAFA7),
            surfaceBase = Color(0xFFDCC2B4),
            surfaceCool = Color(0xFFF0DED3),
            surfaceAccent = Color(0xFFB79B91),
            accent = Color(0xFF9A3B1D),
            accentLight = Color(0xFF2BAE66),
            accentMuted = Color(0x669A3B1D),
            panelScrim = Color(0xCC1E2025),
            osdScrim = Color(0x991E2025),
            nowLine = Color(0xFFB82362),
            nowFill = Color(0x339A3B1D),
            live = Color(0xFFB82362),
            pipPreviewOutline = Color(0xFF9A3B1D),
            focusFill = Color(0x339A3B1D),
            textPrimary = Color(0xFF2B1830),
            textSecondary = Color(0xFF623343),
            textTertiary = Color(0x99623343),
            textDisabled = Color(0x66623343),
            success = Color(0xFF2BAE66),
            warning = Color(0xFF9A3B1D),
            info = Color(0xFF7A2754),
            divider = Color(0x22211827),
            outline = Color(0x409A3B1D),
        )

        /** Afterglow Light 2 — dim apricot sunset, closest light cousin to Dark 2. */
        val AfterglowLight2 = AppPalette(
            id = "afterglow_light_2",
            displayName = "Afterglow Light 2",
            description = "Dim apricot surfaces with charcoal text, orange focus, pink live glow, and medium mint signal.",
            surfaceDeep = Color(0xFFB87B62),
            surfaceBase = Color(0xFFD7A58B),
            surfaceCool = Color(0xFFF0CDBA),
            surfaceAccent = Color(0xFFA56552),
            accent = Color(0xFF8B3419),
            accentLight = Color(0xFF2EAF68),
            accentMuted = Color(0x668B3419),
            panelScrim = Color(0xCC211612),
            osdScrim = Color(0x99211612),
            nowLine = Color(0xFFB5265E),
            nowFill = Color(0x338B3419),
            live = Color(0xFFB5265E),
            pipPreviewOutline = Color(0xFF8B3419),
            focusFill = Color(0x408B3419),
            textPrimary = Color(0xFF3B1607),
            textSecondary = Color(0xFF793018),
            textTertiary = Color(0x99793018),
            textDisabled = Color(0x66793018),
            success = Color(0xFF2EAF68),
            warning = Color(0xFF8B3419),
            info = Color(0xFF2EAF68),
            divider = Color(0x22211120),
            outline = Color(0x408B3419),
        )

        /** Afterglow Light 3 — muted mint-gray with orange contrast. */
        val AfterglowLight3 = AppPalette(
            id = "afterglow_light_3",
            displayName = "Afterglow Light 3",
            description = "Muted mint-gray panels with dark slate text, orange focus, and medium mint signal.",
            surfaceDeep = Color(0xFFA9C4B2),
            surfaceBase = Color(0xFFC8DBC9),
            surfaceCool = Color(0xFFE6F0E8),
            surfaceAccent = Color(0xFF88A895),
            accent = Color(0xFF7E3B18),
            accentLight = Color(0xFF33B56D),
            accentMuted = Color(0x667E3B18),
            panelScrim = Color(0xCC102B27),
            osdScrim = Color(0x99102B27),
            nowLine = Color(0xFF9F285D),
            nowFill = Color(0x337E3B18),
            live = Color(0xFF9F285D),
            pipPreviewOutline = Color(0xFF33B56D),
            focusFill = Color(0x407E3B18),
            textPrimary = Color(0xFF0F3325),
            textSecondary = Color(0xFF23624B),
            textTertiary = Color(0x9923624B),
            textDisabled = Color(0x6623624B),
            success = Color(0xFF33B56D),
            warning = Color(0xFF7E3B18),
            info = Color(0xFF33B56D),
            divider = Color(0x22102B27),
            outline = Color(0x4033B56D),
        )

        /** Rachel's Sunrise — daylight version of the mint, powder blue, violet, peach, and pink reference. */
        val RachelsSunrise = AppPalette(
            id = "rachels_sunrise",
            displayName = "Rachel's Sunrise",
            description = "I love you Rachel! Daylight mint teal, powder blue, violet, neon peach-orange, and hot-pink sunset energy.",
            surfaceDeep = Color(0xFF8FE6DC),
            surfaceBase = Color(0xFFB7D8FF),
            surfaceCool = Color(0xFFD6C2FF),
            surfaceAccent = Color(0xFFFF9DD5),
            accent = Color(0xFF063B45),
            accentLight = Color(0xFFFF6F2E),
            accentMuted = Color(0x66063B45),
            panelScrim = Color(0xCC07333B),
            osdScrim = Color(0x9907333B),
            nowLine = Color(0xFFFF2F92),
            nowFill = Color(0x33063B45),
            live = Color(0xFFFF2F92),
            pipPreviewOutline = Color(0xFF063B45),
            focusFill = Color(0x40063B45),
            textPrimary = Color(0xFF07333B),
            textSecondary = Color(0xFF0D5360),
            textTertiary = Color(0xFF155F6C),
            textDisabled = Color(0x660D5360),
            success = Color(0xFF0D6775),
            warning = Color(0xFFB74718),
            info = Color(0xFFB8276B),
            divider = Color(0x2207333B),
            outline = Color(0x66063B45),
            glowIntensity = 1f,
        )

        /** Afterglow Light 4 — warm ash, peach controls, medium mint secondary. */
        val AfterglowLight4 = AppPalette(
            id = "afterglow_light_4",
            displayName = "Afterglow Light 4",
            description = "Warm ash panels with burnt peach focus, medium mint signal, and charcoal contrast.",
            surfaceDeep = Color(0xFFBFA9A0),
            surfaceBase = Color(0xFFDCC5BA),
            surfaceCool = Color(0xFFF0E0D8),
            surfaceAccent = Color(0xFFAD8374),
            accent = Color(0xFF8F3518),
            accentLight = Color(0xFF2EAF68),
            accentMuted = Color(0x668F3518),
            panelScrim = Color(0xCC211816),
            osdScrim = Color(0x99211816),
            nowLine = Color(0xFFA6225A),
            nowFill = Color(0x338F3518),
            live = Color(0xFFA6225A),
            pipPreviewOutline = Color(0xFF2EAF68),
            focusFill = Color(0x408F3518),
            textPrimary = Color(0xFF2E2013),
            textSecondary = Color(0xFF69502A),
            textTertiary = Color(0x9969502A),
            textDisabled = Color(0x6669502A),
            success = Color(0xFF2EAF68),
            warning = Color(0xFF8F3518),
            info = Color(0xFFE7357A),
            divider = Color(0x221C0D1B),
            outline = Color(0x402EAF68),
        )

        /** Afterglow Violet Spectrum — theme built from the six-swatch reference image. */
        val UltravioletSpectrum = AppPalette(
            id = "ultraviolet_spectrum",
            displayName = "Afterglow Violet Spectrum",
            description = "Afterglow six-swatch purple spectrum with lavender highlights and deep violet surfaces.",
            surfaceDeep = Color(0xFF10002B),
            surfaceBase = Color(0xFF240046),
            surfaceCool = Color(0xFF3C096C),
            surfaceAccent = Color(0xFF501488),
            accent = Color(0xFFA855F0),
            accentLight = Color(0xFFE0AAFF),
            accentMuted = Color(0x66A855F0),
            panelScrim = Color(0xD610002B),
            osdScrim = Color(0xA610002B),
            nowLine = Color(0xFFC77DFF),
            nowFill = Color(0x33A855F0),
            live = Color(0xFF9D4EDD),
            pipPreviewOutline = Color(0xFFA855F0),
            focusFill = Color(0x40A855F0),
            textPrimary = Color(0xFFF8F5FF),
            textSecondary = Color(0xD9E0AAFF),
            textTertiary = Color(0x99E0AAFF),
            textDisabled = Color(0x66E0AAFF),
            success = Color(0xFFC77DFF),
            warning = Color(0xFFE0AAFF),
            info = Color(0xFFE0AAFF),
            divider = Color(0x22E0AAFF),
            outline = Color(0x66A855F0),
        )

        /** Mineral Slate — coherent cool blue-slate ramp lit by a single warm copper accent. */
        val MineralSlate = AppPalette(
            id = "mineral_slate",
            displayName = "Mineral Slate",
            description = "Cool blue-slate surfaces stepping dark to light, lit by a single warm copper accent and bronze live marker.",
            surfaceDeep = Color(0xFF0D151D),
            surfaceBase = Color(0xFF1E3142),
            surfaceCool = Color(0xFF40515F),
            surfaceAccent = Color(0xFF5A6A78),
            accent = Color(0xFFFFE6CC),
            accentLight = Color(0xFFFFF1E6),
            accentMuted = Color(0x66FFE6CC),
            panelScrim = Color(0xD613181D),
            osdScrim = Color(0xA613181D),
            nowLine = Color(0xFFDA9D62),
            nowFill = Color(0x33CD8B58),
            live = Color(0xFFDA9D62),
            pipPreviewOutline = Color(0xFFE7B68E),
            focusFill = Color(0x40CD8B58),
            textPrimary = Color(0xFFF4F7F9),
            textSecondary = Color(0xFFC8D0D8),
            textTertiary = Color(0xFF98A4AE),
            textDisabled = Color(0x66C8D0D8),
            success = Color(0xFF82B58C),
            warning = Color(0xFFDA9D62),
            info = Color(0xFFB4C6D6),
            divider = Color(0x22F4F7F9),
            outline = Color(0x66CD8B58),
            glowIntensity = 0.35f,
        )

        /** Afterglow Copper Fjord — theme built from the rust, teal, burgundy, and charcoal swatch reference image. */
        val CopperFjord = AppPalette(
            id = "copper_fjord",
            displayName = "Afterglow Copper Fjord",
            description = "Afterglow rust copper, deep teal, burgundy shadow, and near-black charcoal.",
            surfaceDeep = Color(0xFF071118),
            surfaceBase = Color(0xFF003A40),
            surfaceCool = Color(0xFF003A45),
            surfaceAccent = Color(0xFF015468),
            accent = Color(0xFFD86A3C),
            accentLight = Color(0xFFD46A4E),
            accentMuted = Color(0x66D86A3C),
            panelScrim = Color(0xD6071118),
            osdScrim = Color(0xA6071118),
            nowLine = Color(0xFFD46A4E),
            nowFill = Color(0x33D86A3C),
            live = Color(0xFFD46A4E),
            pipPreviewOutline = Color(0xFF015468),
            focusFill = Color(0x40D86A3C),
            textPrimary = Color(0xFFF7EFEB),
            textSecondary = Color(0xD9D6E6E7),
            textTertiary = Color(0x99D6E6E7),
            textDisabled = Color(0x66D6E6E7),
            success = Color(0xFF2F8AA0),
            warning = Color(0xFFD46A4E),
            info = Color(0xFFB86A78),
            divider = Color(0x22D6E6E7),
            outline = Color(0x66015468),
        )

        /** Afterglow Amber Noir — theme built from the tan, amber, mauve, umber, and black swatch reference image. */
        val AmberNoir = AppPalette(
            id = "amber_noir",
            displayName = "Afterglow Amber Noir",
            description = "Afterglow soft tan, amber gold, muted mauve, umber brown, and near-black.",
            surfaceDeep = Color(0xFF0B0A0C),
            surfaceBase = Color(0xFF2F1E16),
            surfaceCool = Color(0xFF6E585E),
            surfaceAccent = Color(0xFF6B4716),
            accent = Color(0xFFBF8737),
            accentLight = Color(0xFFD0B99A),
            accentMuted = Color(0x66BF8737),
            panelScrim = Color(0xD60B0A0C),
            osdScrim = Color(0xA60B0A0C),
            nowLine = Color(0xFFD0B99A),
            nowFill = Color(0x33BF8737),
            live = Color(0xFFD0B99A),
            pipPreviewOutline = Color(0xFFBF8737),
            focusFill = Color(0x40BF8737),
            textPrimary = Color(0xFFF7EFE5),
            textSecondary = Color(0xD9E7D7C6),
            textTertiary = Color(0x99E7D7C6),
            textDisabled = Color(0x66E7D7C6),
            success = Color(0xFF8C6B74),
            warning = Color(0xFFBF8737),
            info = Color(0xFFD0B99A),
            divider = Color(0x22E7D7C6),
            outline = Color(0x66D0B99A),
        )

        /** Golden Slate — theme built from the navy, steel, cream, sand, and copper swatch reference image. */
        val GoldenSlate = AppPalette(
            id = "golden_slate",
            displayName = "Golden Slate",
            description = "Navy slate, steel blue, soft cream, sand, and copper.",
            surfaceDeep = Color(0xFF182B44),
            surfaceBase = Color(0xFF304A68),
            surfaceCool = Color(0xFF66727B),
            surfaceAccent = Color(0xFF8D755F),
            accent = Color(0xFFDE9040),
            accentLight = Color(0xFFFEEECD),
            accentMuted = Color(0x66DE9040),
            panelScrim = Color(0xD62E3D50),
            osdScrim = Color(0xA62E3D50),
            nowLine = Color(0xFFD7B791),
            nowFill = Color(0x33DE9040),
            live = Color(0xFFDE9040),
            pipPreviewOutline = Color(0xFFD7B791),
            focusFill = Color(0x40DE9040),
            textPrimary = Color(0xFFFEEECD),
            textSecondary = Color(0xFFF1E6CE),
            textTertiary = Color(0xFFD9C9AC),
            textDisabled = Color(0x66E7D9C0),
            success = Color(0xFF8894A2),
            warning = Color(0xFFD7B791),
            info = Color(0xFFFEEECD),
            divider = Color(0x22FEEECD),
            outline = Color(0x66D7B791),
            glowIntensity = 0.35f,
        )

        /** All available presets, alphabetized by display name for theme picker browsing. */
        val ALL: List<AppPalette> = listOf(
            AmberNoir,            // Afterglow Amber Noir
            CopperFjord,          // Afterglow Copper Fjord
            Afterglow1,           // Afterglow Dark 1
            AfterglowSunset,      // Afterglow Dark 2 — default
            Afterglow3,           // Afterglow Dark 3
            Afterglow4,           // Afterglow Dark 4
            AfterglowGray,        // Afterglow Gray (dark monochrome)
            AfterglowGrayLight,
            AfterglowLight1,
            AfterglowLight2,
            AfterglowLight3,
            AfterglowLight4,
            UltravioletSpectrum,  // Afterglow Violet Spectrum
            BlueSteel,
            ClassicBlue,
            ForestMist,
            GoldenSlate,
            MineralSlate,
            NeonDusk,
            PureOnyx,
            RachelsSunrise,       // Rachel's Sunrise
            SunsetAurora,         // Rachel's Sunset
            Synthwave,
            Vaporwave,
        )

        fun byId(id: String?): AppPalette = ALL.firstOrNull { it.id == id } ?: AfterglowSunset
    }
}
