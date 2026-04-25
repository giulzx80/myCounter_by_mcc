package com.mcc.mycounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.mcc.mycounter.data.preferences.UserSettings

/**
 * Tema root dell'app. Legge le impostazioni utente (palette + modalità)
 * per costruire dinamicamente lo [ColorScheme] usato da Material3.
 */
@Composable
fun MyCounterTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val palette = Palettes.byName(settings.paletteName)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primaryDark,
            secondary = palette.secondaryDark,
            tertiary = palette.tertiaryDark
        )
    } else {
        lightColorScheme(
            primary = palette.primaryLight,
            secondary = palette.secondaryLight,
            tertiary = palette.tertiaryLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
