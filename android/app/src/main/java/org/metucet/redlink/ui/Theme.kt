package org.metucet.redlink.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val text = Color(0xFFEEF0F2)
private val dim = Color(0xFF9AA3AD)
private val raised = Color(0xFF1A1A1D)

// Dark only, on a pure black background. The red is for the wordmark and the
// Start button, so the quieter buttons take the secondary container's grey.
private val colors = darkColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    secondaryContainer = raised,
    onSecondaryContainer = text,
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0E0E10),
    surfaceContainerHigh = raised,
    onSurface = text,
    onSurfaceVariant = dim,
    outlineVariant = Color(0xFF26262A),
)

// Green is working, amber is waiting or incomplete, red is broken, grey is idle.
enum class Dot(val color: Color) {
    GREEN(Color(0xFF3ECF7A)),
    AMBER(Color(0xFFF2B33D)),
    RED(Color(0xFFF0554D)),
    GREY(dim),
}

@Composable
fun RedLinkTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
