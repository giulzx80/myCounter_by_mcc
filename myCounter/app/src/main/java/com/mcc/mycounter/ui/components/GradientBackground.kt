package com.mcc.mycounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Sfondo condiviso — DexHub-style: piatto e scuro, con un leggerissimo
 * accento del primary in alto (~6%). Identico a quello di MyVote_by_mcc.
 *
 * Se [customBaseColor] è valorizzato (es. colore del contatore corrente
 * sulla TapScreen), prende il sopravvento come dominante della schermata.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    customBaseColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (customBaseColor != null) Brush.verticalGradient(
                    colors = listOf(
                        customBaseColor.copy(alpha = 0.45f),
                        customBaseColor.copy(alpha = 0.18f),
                        background
                    )
                )
                else Brush.verticalGradient(
                    // Solo il top ha un velo del primary (~6%) per dare
                    // "segnaletica" senza distrarre dai contenuti.
                    colors = listOf(
                        primary.copy(alpha = 0.06f),
                        background,
                        background
                    )
                )
            )
    ) {
        content()
    }
}
