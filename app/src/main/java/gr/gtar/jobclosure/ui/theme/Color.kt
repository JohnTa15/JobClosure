package gr.gtar.jobclosure.ui.theme

import androidx.compose.ui.graphics.Color
import gr.gtar.jobclosure.data.BookingType

// Brand palette: deep navy + warm gold, with a dusty-rose and teal accent for variety
// across booking types - evokes photography/film without leaning on any single wedding cliché.

val NavyLight = Color(0xFF1B3A4B)
val NavyLighter = Color(0xFF2F5568)
val NavyContainerLight = Color(0xFFD3E4EC)
val NavyDark = Color(0xFFAFD3E3)
val NavyContainerDark = Color(0xFF16323F)

val GoldLight = Color(0xFFB8860B)
val GoldContainerLight = Color(0xFFFFE7B3)
val GoldDark = Color(0xFFEFC873)
val GoldContainerDark = Color(0xFF5C4200)

val RoseLight = Color(0xFF9C4259)
val RoseContainerLight = Color(0xFFFFD9E1)
val RoseDark = Color(0xFFFFB1C4)
val RoseContainerDark = Color(0xFF6B2338)

val TealLight = Color(0xFF2A6F77)
val TealContainerLight = Color(0xFFB8EAF0)
val TealDark = Color(0xFF9ED4DA)
val TealContainerDark = Color(0xFF0B4148)

val BackgroundLight = Color(0xFFFFFBF7)
val SurfaceLight = Color(0xFFFFFEFB)
val SurfaceVariantLight = Color(0xFFEDE3D8)
val OutlineLight = Color(0xFF7C7267)

val BackgroundDark = Color(0xFF15131A)
val SurfaceDark = Color(0xFF1D1B22)
val SurfaceVariantDark = Color(0xFF433E37)
val OutlineDark = Color(0xFF988F82)

val ErrorLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)

/** A small color identity per booking type, used for badges/accents in the UI. */
fun BookingType.accentColor(): Color = when (this) {
    BookingType.WEDDING -> GoldLight
    BookingType.BAPTISM -> RoseLight
    BookingType.WEDDING_AND_BAPTISM -> NavyLighter
    BookingType.SCHOOL_EVENT -> TealLight
    BookingType.PERFORMANCE -> Color(0xFF6B4FA0)
    BookingType.OTHER -> OutlineLight
}
