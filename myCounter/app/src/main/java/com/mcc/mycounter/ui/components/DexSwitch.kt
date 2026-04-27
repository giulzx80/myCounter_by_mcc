package com.mcc.mycounter.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wrapper di [Switch] (Material 3) con colori "DexHub-tuned": il default M3
 * punta `uncheckedThumb`/`uncheckedTrack` su token (`outline` /
 * `surfaceContainerHighest`) molto chiari rispetto al `surface`, quindi sul
 * tema light DexHub il toggle OFF risulta praticamente invisibile.
 *
 * Qui rimappiamo:
 *  - thumb OFF → `onSurfaceVariant` (testo soft, sempre ben contrastato)
 *  - track OFF → `surfaceVariant` (DexHub surface-2, distinguibile dal fondo)
 *  - border OFF → `outline`
 *
 * I colori "ON" restano quelli M3 di default (primary + onPrimary).
 *
 * Usalo come drop-in al posto di `Switch(...)`.
 */
@Composable
fun DexSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant,
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.onPrimary,
        )
    )
}
