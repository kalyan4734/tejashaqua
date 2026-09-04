package com.tejashaqua.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tejashaqua.app.utils.findActivity

private val LightColorScheme = lightColorScheme(
    primary = AquaBlue,
    secondary = AquaLightBlue,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun TejashAquaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(currentDensity.density, fontScale = 0.94f)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
