package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isDarkThemeGlobal,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = BlueAccent,
            onPrimary = Color.White,
            background = BgDark,
            onBackground = TextWhite,
            surface = SurfaceDark,
            onSurface = TextWhite,
            surfaceVariant = SurfaceDarkVariant,
            onSurfaceVariant = TextGrayLight,
            secondary = PriorityNormal,
            tertiary = StatusCompleted
        )
    } else {
        lightColorScheme(
            primary = BlueAccent,
            onPrimary = Color.White,
            background = BgDark,
            onBackground = TextWhite,
            surface = SurfaceDark,
            onSurface = TextWhite,
            surfaceVariant = SurfaceDarkVariant,
            onSurfaceVariant = TextGrayLight,
            secondary = PriorityNormal,
            tertiary = StatusCompleted
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

