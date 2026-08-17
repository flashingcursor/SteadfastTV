package tv.own.owntv.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tv.own.owntv.ui.theme.AccentColor
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.ThemeMode

/**
 * Preview-only theme harness: real OwnTVTheme (Figtree type, colour tokens, default glass/animation
 * locals) around [content], on the theme background. Never referenced from production code.
 */
@Composable
fun OwnTVPreview(light: Boolean = false, content: @Composable () -> Unit) {
    OwnTVTheme(
        themeMode = if (light) ThemeMode.LIGHT else ThemeMode.DARK,
        accent = AccentColor.TEAL,
        systemInDarkTheme = !light,
    ) {
        Box(Modifier.background(OwnTVTheme.colors.background)) { content() }
    }
}

/** Full-screen TV canvas (1080p at TV density) for tall/anchored surfaces. */
@Preview(device = "spec:width=1920dp,height=1080dp,dpi=213", showBackground = true, backgroundColor = 0xFF0C0C0C)
annotation class TvPreview

/** Hugged canvas for small components — no device spec so the preview wraps its content. */
@Preview(showBackground = true, backgroundColor = 0xFF0C0C0C)
annotation class TvComponentPreview

@TvComponentPreview
@Composable
private fun HarnessPreview() = OwnTVPreview {
    androidx.tv.material3.Text("OwnTV preview harness", style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
}
