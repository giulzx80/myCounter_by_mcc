package com.mcc.mycounter.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette di myCounter — allineata al design system del **DexHub Suite**
 * (vedi `dex-design.css` nel progetto DexHub) e al gemello MyVote_by_mcc.
 *
 * Il design è "dark-first": le superfici scure (#0A0A0A → #262626) sono
 * condivise da tutte le palette, l'unica cosa che cambia è l'accento
 * `primary` / `secondary` / `tertiary`. Per questo le palette qui sotto
 * non ridefiniscono lo sfondo: lo prende [DexSurface] in modo uniforme.
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

/**
 * Costanti delle superfici e dei colori semantici condivise da TUTTE le
 * palette. I valori esatti sono gli stessi di DexHub (`dex-design.css`).
 */
object DexSurface {
    // ----- Dark surface ladder ------------------------------------------------
    val DarkBg        = Color(0xFF0A0A0A)
    val DarkSurface   = Color(0xFF141414)
    val DarkSurface2  = Color(0xFF1C1C1C)
    val DarkSurface3  = Color(0xFF262626)
    val DarkBorder    = Color(0xFF2A2A2A)
    val DarkText      = Color(0xFFECECEC)
    val DarkTextSoft  = Color(0xFFC8C8C8)
    val DarkMuted     = Color(0xFF8A8A8A)

    // ----- Light surface ladder ----------------------------------------------
    val LightBg        = Color(0xFFF5F7FA)
    val LightSurface   = Color(0xFFFFFFFF)
    val LightSurface2  = Color(0xFFF8FAFC)
    val LightSurface3  = Color(0xFFEEF2F7)
    val LightBorder    = Color(0xFFE1E5EA)
    val LightText      = Color(0xFF1F2933)
    val LightTextSoft  = Color(0xFF334155)
    val LightMuted     = Color(0xFF667080)

    // ----- Semantici ---------------------------------------------------------
    val DangerDark   = Color(0xFFEF4444)
    val DangerLight  = Color(0xFFDC2626)
    val SuccessDark  = Color(0xFF10B981)
    val SuccessLight = Color(0xFF16A34A)
    val WarnDark     = Color(0xFFF59E0B)
    val WarnLight    = Color(0xFFD97706)

    // ----- Brand accents per uso diretto (logo, splash, ecc.) ---------------
    val SkyBlue      = Color(0xFF4AA5D0)
    val SkyBlueDark  = Color(0xFF3786B1)
    val Gold         = Color(0xFFCFB347)
}

object Palettes {
    /**
     * Palette di default: monocromatica grigio-scuro.
     * Pensata per chi vuole un look neutro ed elegante — i pulsanti, il logo
     * in-app e gli accenti sono tutte tonalità di grigio scuro su superfici
     * DexHub. In dark mode il primary diventa un grigio chiaro per restare
     * leggibile sui fondi quasi-neri.
     */
    val Default = AppPalette(
        name = "default",
        label = "Default",
        primaryLight = Color(0xFF1F2933),
        secondaryLight = Color(0xFF334155),
        tertiaryLight = Color(0xFF475569),
        primaryDark = Color(0xFFA1A1AA),
        secondaryDark = Color(0xFF71717A),
        tertiaryDark = Color(0xFF52525B),
    )

    /**
     * Palette "Sky" — il blu DexHub.
     * Light: blu più carico (#2563EB), Dark: sky #4AA5D0 + gold accent.
     */
    val Sky = AppPalette(
        name = "sky",
        label = "Sky",
        primaryLight = Color(0xFF2563EB),
        secondaryLight = Color(0xFF1D4ED8),
        tertiaryLight = Color(0xFF0284C7),
        primaryDark = Color(0xFF4AA5D0),
        secondaryDark = Color(0xFF3786B1),
        tertiaryDark = Color(0xFFCFB347),
    )

    val Sunset = AppPalette(
        name = "sunset",
        label = "Sunset",
        primaryLight = Color(0xFFEA580C),
        secondaryLight = Color(0xFFD97706),
        tertiaryLight = Color(0xFFBE185D),
        primaryDark = Color(0xFFFB923C),
        secondaryDark = Color(0xFFF59E0B),
        tertiaryDark = Color(0xFFEC4899),
    )

    val Ocean = AppPalette(
        name = "ocean",
        label = "Ocean",
        primaryLight = Color(0xFF0891B2),
        secondaryLight = Color(0xFF0E7490),
        tertiaryLight = Color(0xFF0369A1),
        primaryDark = Color(0xFF22D3EE),
        secondaryDark = Color(0xFF06B6D4),
        tertiaryDark = Color(0xFF38BDF8),
    )

    val Forest = AppPalette(
        name = "forest",
        label = "Forest",
        primaryLight = Color(0xFF16A34A),
        secondaryLight = Color(0xFF15803D),
        tertiaryLight = Color(0xFF059669),
        primaryDark = Color(0xFF34D399),
        secondaryDark = Color(0xFF10B981),
        tertiaryDark = Color(0xFFA3E635),
    )

    val all = listOf(Default, Sky, Sunset, Ocean, Forest)

    fun byName(name: String): AppPalette = all.firstOrNull { it.name == name } ?: Default
}

/**
 * Colori disponibili per personalizzare il pulsante TAP del singolo contatore.
 * Sono colori-identità, non legati al brand: l'utente li sceglie liberamente
 * per distinguere visivamente i contatori. La prima voce è il grigio neutro
 * della palette Default, così i nuovi contatori ereditano lo stile grigio.
 */
object TapColors {
    val Palette = listOf(
        0xFF52525B.toInt(), // grigio scuro (Default)
        0xFF4AA5D0.toInt(), // sky blue (DexHub Sky)
        0xFFEA580C.toInt(), // arancio
        0xFFEF4444.toInt(), // rosso
        0xFFEC4899.toInt(), // magenta
        0xFF8B5CF6.toInt(), // viola
        0xFF2563EB.toInt(), // blu
        0xFF06B6D4.toInt(), // verde acqua
        0xFF10B981.toInt(), // verde
        0xFFF59E0B.toInt(), // ambra
        0xFF6D4C41.toInt(), // marrone
    )
}
