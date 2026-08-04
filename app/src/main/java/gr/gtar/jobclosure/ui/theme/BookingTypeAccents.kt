package gr.gtar.jobclosure.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import gr.gtar.jobclosure.data.BookingType

/**
 * Fixed per-booking-type colour identity from the restyle design handoff - unlike [AppTheme], these
 * never change with the user's theme choice, so a wedding card is always gold and a baptism card is
 * always pink regardless of which of the 4 app themes is active.
 */
@Immutable
data class TypeColors(val light: Color, val dark: Color, val isFlat: Boolean = false) {
    val chipText: Color get() = light
    val chipBorder: Color get() = light.copy(alpha = 0.35f)
    val chipFill: Color get() = light.copy(alpha = 0.08f)
    val cardPressShadow: Color get() = light.copy(alpha = 0.22f)
    val barBrush: Brush get() = if (isFlat) Brush.verticalGradient(listOf(dark, dark)) else Brush.verticalGradient(listOf(light, dark))
}

fun typeColors(type: BookingType): TypeColors = when (type) {
    BookingType.WEDDING -> TypeColors(light = Color(0xFFFFD166), dark = Color(0xFFFF8A3D))
    BookingType.BAPTISM -> TypeColors(light = Color(0xFFFF8FB0), dark = Color(0xFFFF3D7F))
    BookingType.WEDDING_AND_BAPTISM -> TypeColors(light = Color(0xFFB5ABFC), dark = Color(0xFF9184D9))
    BookingType.SCHOOL_EVENT -> TypeColors(light = Color(0xFF7FF0E2), dark = Color(0xFF14B8A6))
    BookingType.PERFORMANCE -> TypeColors(light = Color(0xFFC4B5FD), dark = Color(0xFF8B5CF6))
    BookingType.OTHER -> TypeColors(light = Color(0xFF9397AB), dark = Color(0xFF75798C), isFlat = true)
}
