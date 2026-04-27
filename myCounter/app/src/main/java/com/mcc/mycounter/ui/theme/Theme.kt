package com.mcc.mycounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mcc.mycounter.data.preferences.UserSettings

/**
 * Tema root di myCounter — derivato dal design system del DexHub Suite,
 * identico per logica al tema di MyVote_by_mcc.
 *
 * Tutte le palette condividono lo STESSO surface ladder (vedi [DexSurface]):
 * l'identità si esprime solo tramite l'accento `primary/secondary/tertiary`.
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
            // I primary in dark sono colori chiari (sky #4AA5D0, gray #A1A1AA, ecc.):
            // testo/icone sopra il primary devono essere scuri.
            onPrimary = DexSurface.DarkBg,
            primaryContainer = palette.primaryDark.copy(alpha = 0.16f),
            onPrimaryContainer = palette.primaryDark,
            secondary = palette.secondaryDark,
            onSecondary = DexSurface.DarkText,
            secondaryContainer = palette.secondaryDark.copy(alpha = 0.18f),
            onSecondaryContainer = palette.secondaryDark,
            tertiary = palette.tertiaryDark,
            onTertiary = DexSurface.DarkBg,
            tertiaryContainer = palette.tertiaryDark.copy(alpha = 0.18f),
            onTertiaryContainer = palette.tertiaryDark,

            background = DexSurface.DarkBg,
            onBackground = DexSurface.DarkText,

            surface = DexSurface.DarkSurface,
            onSurface = DexSurface.DarkText,
            surfaceVariant = DexSurface.DarkSurface2,
            onSurfaceVariant = DexSurface.DarkTextSoft,
            surfaceTint = palette.primaryDark,

            outline = DexSurface.DarkBorder,
            outlineVariant = DexSurface.DarkBorder.copy(alpha = 0.6f),

            error = DexSurface.DangerDark,
            onError = DexSurface.DarkText,
            errorContainer = DexSurface.DangerDark.copy(alpha = 0.16f),
            onErrorContainer = DexSurface.DangerDark,

            inverseSurface = DexSurface.DarkText,
            inverseOnSurface = DexSurface.DarkBg,
            inversePrimary = palette.primaryLight,
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = palette.primaryLight,
            onPrimary = Color.White,
            primaryContainer = palette.primaryLight.copy(alpha = 0.12f),
            onPrimaryContainer = palette.primaryLight,
            secondary = palette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = palette.secondaryLight.copy(alpha = 0.14f),
            onSecondaryContainer = palette.secondaryLight,
            tertiary = palette.tertiaryLight,
            onTertiary = Color.White,
            tertiaryContainer = palette.tertiaryLight.copy(alpha = 0.14f),
            onTertiaryContainer = palette.tertiaryLight,

            background = DexSurface.LightBg,
            onBackground = DexSurface.LightText,

            surface = DexSurface.LightSurface,
            onSurface = DexSurface.LightText,
            surfaceVariant = DexSurface.LightSurface2,
            onSurfaceVariant = DexSurface.LightTextSoft,
            surfaceTint = palette.primaryLight,

            outline = DexSurface.LightBorder,
            outlineVariant = DexSurface.LightBorder.copy(alpha = 0.6f),

            error = DexSurface.DangerLight,
            onError = Color.White,
            errorContainer = DexSurface.DangerLight.copy(alpha = 0.10f),
            onErrorContainer = DexSurface.DangerLight,

            inverseSurface = DexSurface.LightText,
            inverseOnSurface = DexSurface.LightBg,
            inversePrimary = palette.primaryDark,
            scrim = Color.Black,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
