package tv.own.owntv.features.shell.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.core.weather.WeatherInfo
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.preview.OwnTVPreview
import tv.own.owntv.ui.preview.TvPreview
import tv.own.owntv.ui.theme.GlassSurface
import tv.own.owntv.ui.theme.LocalGlass
import tv.own.owntv.ui.theme.WeatherGlyph
import java.util.Date

// Floating shell header: fully transparent, three zones (start title / center search / end weather+
// clock). Unlike TopBar's chips, nothing here sits on a solid or glass panel by default — the header
// floats directly over the background artwork, so title/weather/clock read as plain text with a soft
// drop shadow instead of tonal chips. The search pill is the ONLY capsule in this header.
private val HeaderTextShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 12f)
private val SearchPillShape = RoundedCornerShape(50)

/**
 * Transparent three-zone shell header: start = page title, center = the single search pill, end =
 * weather (optional) then clock. Caller pins this to the top of the screen; it never draws a
 * background of its own so the blurred/backdrop artwork behind the shell shows through.
 */
@Composable
fun ShellHeader(
    title: String,
    onSearch: () -> Unit,
    weatherInfo: WeatherInfo?,
    weatherFahrenheit: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            ShellTitle(title)
        }
        ShellSearchPill(onClick = onSearch)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            ShellWeatherAndClock(weatherInfo = weatherInfo, weatherFahrenheit = weatherFahrenheit)
        }
    }
}

@Composable
private fun ShellTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, shadow = HeaderTextShadow),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The header's only capsule: translucent glass when glass mode is on, else a faint flat fill + hairline border. */
@Composable
private fun ShellSearchPill(onClick: () -> Unit) {
    val glassy = LocalGlass.current.isGlassy(GlassSurface.TOPBAR)
    FocusableSurface(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 220.dp)
            .then(if (!glassy) Modifier.border(1.dp, Color.White.copy(alpha = 0.13f), SearchPillShape) else Modifier),
        shape = SearchPillShape,
        surface = GlassSurface.TOPBAR,
        unfocusedContainerColor = Color.White.copy(alpha = 0.09f),
        contentAlignment = Alignment.Center,
    ) { _ ->
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OwnTVIcon(icon = OwnTVIcon.SEARCH, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.common_search),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShellWeatherAndClock(weatherInfo: WeatherInfo?, weatherFahrenheit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (weatherInfo != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WeatherConditionIcon(info = weatherInfo, modifier = Modifier.size(18.dp))
                val temp = if (weatherFahrenheit) {
                    stringResource(R.string.common_weather_fahrenheit, (weatherInfo.temperatureC * 9 / 5 + 32).toInt())
                } else {
                    stringResource(R.string.common_weather_celsius, weatherInfo.temperatureC.toInt())
                }
                Text(temp, style = MaterialTheme.typography.labelLarge.copy(color = Color.White, shadow = HeaderTextShadow))
            }
        }
        ShellClock()
    }
}

@Composable
private fun ShellClock() {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = System.currentTimeMillis() } }
    val formatted = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    Text(formatted, style = MaterialTheme.typography.labelLarge.copy(color = Color.White, shadow = HeaderTextShadow))
}

// Moved verbatim from TopBar.kt's WeatherChip/WeatherConditionIcon (weather glyph canvas drawing).
// TopBar keeps its own copy until Task 4 removes TopBar from the tree.
@Composable
private fun WeatherConditionIcon(info: WeatherInfo, modifier: Modifier = Modifier) {
    val key = info.symbolKey()
    val sunC = WeatherGlyph.Sun; val moonC = WeatherGlyph.Moon
    val cloudC = WeatherGlyph.Cloud; val rainC = WeatherGlyph.Rain
    val snowC = WeatherGlyph.Snow; val fogC = WeatherGlyph.Fog
    val thunderC = WeatherGlyph.Thunder

    Canvas(modifier) {
        val s = size.minDimension / 100f
        val fill = androidx.compose.ui.graphics.drawscope.Fill
        val stk = Stroke(width = 4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun o(x: Float, y: Float) = Offset(x * s, y * s)

        fun sun(cx: Float, cy: Float, r: Float) {
            for (i in 0 until 10) { val a = i * kotlin.math.PI.toFloat() / 5f; drawLine(sunC, o(cx + kotlin.math.cos(a) * (r + 8f), cy + kotlin.math.sin(a) * (r + 8f)), o(cx + kotlin.math.cos(a) * (r + 20f), cy + kotlin.math.sin(a) * (r + 20f)), strokeWidth = 4f * s, cap = StrokeCap.Round) }
            drawCircle(sunC, r * s, o(cx, cy))
        }
        fun moon(cx: Float, cy: Float, r: Float) {
            drawCircle(moonC, r * s, o(cx, cy))
            drawCircle(Color.Black, (r * 0.92f * s), o(cx + r * 0.45f * s, cy - r * 0.20f * s), style = fill, blendMode = BlendMode.Clear)
            listOf(-0.42f to -0.28f, -0.20f to 0.25f, 0.02f to -0.06f).forEach { (dx, dy) -> drawCircle(moonC.copy(alpha = 0.55f), 2.2f * s, o(cx + dx * r, cy + dy * r)) }
        }
        fun cloud(cx: Float, cy: Float, k: Float) {
            drawCircle(cloudC, 16f * k * s, o(cx - 19f * k, cy + 5f * k))
            drawCircle(cloudC, 23f * k * s, o(cx, cy - 9f * k))
            drawCircle(cloudC, 18f * k * s, o(cx + 24f * k, cy + 2f * k))
            drawCircle(cloudC, 13f * k * s, o(cx + 39f * k, cy + 10f * k))
        }
        fun drops(cx: Float, cy: Float, n: Int, c: Color) {
            for (i in 0 until n) drawLine(c, o(cx + i * 18f, cy), o(cx - 5f + i * 18f, cy + 18f), strokeWidth = 4f * s, cap = StrokeCap.Round)
        }
        fun snow(cx: Float, cy: Float) {
            for (i in 0 until 3) { val x = cx + i * 20f; val y = cy + (i % 2) * 4f; val w = 3f * s; val c = StrokeCap.Round; drawLine(snowC, o(x - 7f, y), o(x + 7f, y), w, c); drawLine(snowC, o(x, y - 7f), o(x, y + 7f), w, c); drawLine(snowC, o(x - 5f, y - 5f), o(x + 5f, y + 5f), w, c); drawLine(snowC, o(x + 5f, y - 5f), o(x - 5f, y + 5f), w, c) }
        }
        fun fog(cx: Float, cy: Float) {
            for (i in 0 until 4) drawLine(fogC.copy(alpha = 0.74f), o(cx - 38f, cy + i * 12f), o(cx + 38f, cy + i * 12f), strokeWidth = 5f * s, cap = StrokeCap.Round)
        }
        fun bolt(cx: Float, cy: Float) { val p = Path().apply { moveTo(cx * s, cy * s); lineTo((cx - 12f) * s, (cy + 26f) * s); lineTo((cx + 1f) * s, (cy + 23f) * s); lineTo((cx - 8f) * s, (cy + 48f) * s); lineTo((cx + 18f) * s, (cy + 15f) * s); lineTo((cx + 4f) * s, (cy + 18f) * s); close() }; drawPath(p, thunderC, style = fill) }

        when (key) {
            "sunny" -> sun(50f, 50f, 23f)
            "clearNight" -> moon(50f, 50f, 28f)
            "partlyDay" -> { sun(36f, 36f, 17f); cloud(56f, 60f, 1f) }
            "partlyNight" -> { moon(36f, 35f, 20f); cloud(56f, 60f, 1f) }
            "cloudy" -> { cloud(46f, 48f, 1.15f); cloud(60f, 62f, 0.82f) }
            "fog" -> { cloud(50f, 36f, 0.9f); fog(50f, 58f) }
            "drizzle" -> { cloud(50f, 38f, 1f); drops(35f, 62f, 3, rainC.copy(alpha = 0.72f)) }
            "rain" -> { cloud(50f, 36f, 1.05f); drops(30f, 60f, 4, rainC) }
            "snow" -> { cloud(50f, 36f, 1.05f); snow(32f, 65f) }
            "thunder" -> { cloud(50f, 35f, 1.05f); bolt(52f, 48f); drops(28f, 66f, 2, rainC) }
            else -> cloud(46f, 48f, 1.15f)
        }
    }
}

@TvPreview
@Composable
private fun ShellHeaderWithWeatherPreview() = OwnTVPreview {
    ShellHeader(
        title = stringResource(R.string.common_nav_live_tv),
        onSearch = {},
        weatherInfo = WeatherInfo(temperatureC = 22f, city = "", weatherCode = 0, isDay = true),
        weatherFahrenheit = false,
    )
}

@TvPreview
@Composable
private fun ShellHeaderNoWeatherPreview() = OwnTVPreview {
    ShellHeader(
        title = stringResource(R.string.common_nav_movies),
        onSearch = {},
        weatherInfo = null,
        weatherFahrenheit = false,
    )
}
