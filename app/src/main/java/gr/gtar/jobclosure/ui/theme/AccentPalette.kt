package gr.gtar.jobclosure.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The four switchable colour themes from the restyle design handoff (design_handoff_theme_switcher).
 * Booking-type accents (see BookingTypeAccents.kt) are fixed and never change with this.
 */
enum class AppTheme(val key: String, val label: String) {
    NOCTURNE("nocturne", "Nocturne"),
    AURORA("aurora", "Aurora"),
    EMBER("ember", "Ember"),
    BLOOM("bloom", "Bloom"),
    ;

    companion object {
        fun fromKey(key: String): AppTheme = entries.firstOrNull { it.key == key } ?: NOCTURNE
    }
}

/** Ground/neutral tokens shared by every theme - dark only, per the design handoff. */
object NewUiColors {
    val ground = Color(0xFF161826)
    val surface = Color(0xFF232532)
    val surfaceLow = Color(0xFF1C1E2B)
    val surfaceSunken = Color(0xFF1B1D29)
    val wellDark = Color(0x800D0E17)
    val onGround = Color(0xFFE9E9ED)
    val onGroundMuted = Color(0xFFCFD3E5)
    val onGroundDim = Color(0xFF9397AB)
    val onGroundFaint = Color(0xFF75798C)
    val outline = Color(0xFF3F424D)
    val outlineSoft = Color(0xFF2F3242)

    val cardGradient = listOf(surface, surfaceLow)

    // Fixed semantic colours (independent of the active theme).
    val unconfirmedMarker = Color(0xFFFFB627)
    val droneChip = Color(0xFF2DD4BF)
    val receptionChip = Color(0xFFFF5C8A)
    val success = Color(0xFF7FF0E2)
}

@Immutable
data class AccentPalette(
    val theme: AppTheme,
    val accent: Color,
    val accentBorder: Color,
    val accentDim: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val swatchStart: Color,
    val swatchEnd: Color,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
) {
    /** `rgb of border` in the handoff - the base colour every alpha-derived glow is built from. */
    val glow: Color get() = accentBorder
    val accentGlow: Color get() = glow.copy(alpha = 0.40f)
    val accentGlowSoft: Color get() = glow.copy(alpha = 0.25f)
    val accentCardBorder: Color get() = glow.copy(alpha = 0.35f)
    val accentCardFillTop: Color get() = glow.copy(alpha = 0.12f)
}

val AppThemePalettes: Map<AppTheme, AccentPalette> = mapOf(
    AppTheme.NOCTURNE to AccentPalette(
        theme = AppTheme.NOCTURNE,
        accent = Color(0xFFB5ABFC),
        accentBorder = Color(0xFF9184D9),
        accentDim = Color(0xFF423A6A),
        accentContainer = Color(0xCC2B2741),
        onAccentContainer = Color(0xFFD2CEFD),
        swatchStart = Color(0xFFB5ABFC),
        swatchEnd = Color(0xFF9184D9),
        blob1 = Color(0xFF9184D9).copy(alpha = 0.50f),
        blob2 = Color(0xFFFF5C8A).copy(alpha = 0.28f),
        blob3 = Color(0xFF2DD4BF).copy(alpha = 0.24f),
    ),
    AppTheme.AURORA to AccentPalette(
        theme = AppTheme.AURORA,
        accent = Color(0xFF7FF0E2),
        accentBorder = Color(0xFF2DD4BF),
        accentDim = Color(0xFF1F5A56),
        accentContainer = Color(0xCC103835),
        onAccentContainer = Color(0xFFC7FFF6),
        swatchStart = Color(0xFF7FF0E2),
        swatchEnd = Color(0xFF14B8A6),
        blob1 = Color(0xFF2DD4BF).copy(alpha = 0.45f),
        blob2 = Color(0xFF60A5FA).copy(alpha = 0.30f),
        blob3 = Color(0xFF9184D9).copy(alpha = 0.22f),
    ),
    AppTheme.EMBER to AccentPalette(
        theme = AppTheme.EMBER,
        accent = Color(0xFFFFB86B),
        accentBorder = Color(0xFFFF8A3D),
        accentDim = Color(0xFF6B3A12),
        accentContainer = Color(0xCC42210C),
        onAccentContainer = Color(0xFFFFD9B3),
        swatchStart = Color(0xFFFFD166),
        swatchEnd = Color(0xFFFF8A3D),
        blob1 = Color(0xFFFF8A3D).copy(alpha = 0.40f),
        blob2 = Color(0xFFFF5C8A).copy(alpha = 0.26f),
        blob3 = Color(0xFFFFD166).copy(alpha = 0.20f),
    ),
    AppTheme.BLOOM to AccentPalette(
        theme = AppTheme.BLOOM,
        accent = Color(0xFFFF9EBC),
        accentBorder = Color(0xFFFF5C8A),
        accentDim = Color(0xFF6B2340),
        accentContainer = Color(0xCC441224),
        onAccentContainer = Color(0xFFFFD0DE),
        swatchStart = Color(0xFFFF9EBC),
        swatchEnd = Color(0xFFFF3D7F),
        blob1 = Color(0xFFFF5C8A).copy(alpha = 0.42f),
        blob2 = Color(0xFF9184D9).copy(alpha = 0.30f),
        blob3 = Color(0xFF2DD4BF).copy(alpha = 0.20f),
    ),
)

val LocalAccentPalette = staticCompositionLocalOf { AppThemePalettes.getValue(AppTheme.NOCTURNE) }
