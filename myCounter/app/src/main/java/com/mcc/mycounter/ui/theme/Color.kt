package com.mcc.mycounter.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette disponibili. Ogni palette definisce primary/secondary/tertiary
 * per la light e la dark mode.
 *
 * Identico per scelta cromatica e struttura alle palette di MyVote_by_mcc.
 */
data class AppPalette(
    val name: String,
    val label: String,
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color,
)

object Palettes {
    val Default = AppPalette(
        name = "default",
        label = "Default (Violet)",
        primaryLight = Color(0xFF6750A4),
        secondaryLight = Color(0xFF625B71),
        tertiaryLight = Color(0xFF7D5260),
        primaryDark = Color(0xFFD0BCFF),
        secondaryDark = Color(0xFFCCC2DC),
        tertiaryDark = Color(0xFFEFB8C8),
    )

    val Sunset = AppPalette(
        name = "sunset",
        label = "Sunset",
        primaryLight = Color(0xFFE65100),
        secondaryLight = Color(0xFFF57C00),
        tertiaryLight = Color(0xFFC2185B),
        primaryDark = Color(0xFFFFB74D),
        secondaryDark = Color(0xFFFFAB91),
        tertiaryDark = Color(0xFFF48FB1),
    )

    val Ocean = AppPalette(
        name = "ocean",
        label = "Ocean",
        primaryLight = Color(0xFF006A6B),
        secondaryLight = Color(0xFF1976D2),
        tertiaryLight = Color(0xFF0288D1),
        primaryDark = Color(0xFF4DD0E1),
        secondaryDark = Color(0xFF81D4FA),
        tertiaryDark = Color(0xFF80DEEA),
    )

    val Forest = AppPalette(
        name = "forest",
        label = "Forest",
        primaryLight = Color(0xFF2E7D32),
        secondaryLight = Color(0xFF558B2F),
        tertiaryLight = Color(0xFF6D4C41),
        primaryDark = Color(0xFFA5D6A7),
        secondaryDark = Color(0xFFC5E1A5),
        tertiaryDark = Color(0xFFBCAAA4),
    )

    val all = listOf(Default, Sunset, Ocean, Forest)

    fun byName(name: String): AppPalette = all.firstOrNull { it.name == name } ?: Default
}

/** Colori disponibili per personalizzare il pulsante TAP del singolo contatore. */
object TapColors {
    val Palette = listOf(
        0xFF6750A4.toInt(), // viola (default)
        0xFFE65100.toInt(), // arancio
        0xFFE53935.toInt(), // rosso
        0xFFD81B60.toInt(), // magenta
        0xFF8E24AA.toInt(), // viola scuro
        0xFF1E88E5.toInt(), // blu
        0xFF00897B.toInt(), // verde acqua
        0xFF43A047.toInt(), // verde
        0xFFFFB300.toInt(), // ambra
        0xFF6D4C41.toInt(), // marrone
        0xFF424242.toInt(), // grigio
    )
}
