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
 * Sfondo condiviso con gradiente diagonale.
 * Identico per stile a quello di MyVote_by_mcc:
 *  - se [customBaseColor] è null, usa primary/tertiary del tema (default).
 *  - se [customBaseColor] è valorizzato (es. colore TAP del contatore corrente),
 *    diventa il colore dominante della schermata.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    customBaseColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val primary = customBaseColor ?: MaterialTheme.colorScheme.primary
    val tertiary = customBaseColor ?: MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = if (customBaseColor != null) listOf(
                        customBaseColor.copy(alpha = 0.45f),
                        customBaseColor.copy(alpha = 0.20f),
                        background
                    )
                    else listOf(
                        primary.copy(alpha = 0.12f),
                        background,
                        tertiary.copy(alpha = 0.10f)
                    )
                )
            )
    ) {
        // Overlay con brand color tenue in alto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}
