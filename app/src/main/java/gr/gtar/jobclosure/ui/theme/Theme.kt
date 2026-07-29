package gr.gtar.jobclosure.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = NavyLight,
    onPrimary = Color.White,
    primaryContainer = NavyContainerLight,
    onPrimaryContainer = NavyLight,
    secondary = GoldLight,
    onSecondary = Color.White,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = TealLight,
    onTertiary = Color.White,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = Color(0xFF002022),
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1A),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4D463C),
    outline = OutlineLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = NavyDark,
    onPrimary = Color(0xFF0A2530),
    primaryContainer = NavyContainerDark,
    onPrimaryContainer = NavyContainerLight,
    secondary = GoldDark,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldContainerLight,
    tertiary = TealDark,
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealContainerLight,
    background = BackgroundDark,
    onBackground = Color(0xFFE7E1D9),
    surface = SurfaceDark,
    onSurface = Color(0xFFE7E1D9),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCFC5B8),
    outline = OutlineDark,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
)

@Composable
fun JobClosureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: the app has its own designed palette, and dynamic (Material You)
    // colors would override it with whatever the user's wallpaper happens to produce.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
