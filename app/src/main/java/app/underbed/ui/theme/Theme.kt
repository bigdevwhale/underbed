package app.underbed.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.underbed.R

/** Ink on old paper, as in a Victorian picture book; colour only in the eyes, the candle and the fed belly. */
@Immutable
object C {
    val paper = Color(0xFFEFE6D2)
    val paperDark = Color(0xFFE2D5BA)
    val paperLight = Color(0xFFF6F0E2)
    val line = Color(0xFFB5A68C)
    val ink = Color(0xFF1B1714)
    val ink2 = Color(0xFF4A4038)
    val muted = Color(0xFF7D705F)
    val night = Color(0xFF0D0B0A)
    val night2 = Color(0xFF1A1613)
    val nightLine = Color(0xFF3A322B)
    val chalk = Color(0xFFEFE6D2)
    val chalkMuted = Color(0xFFA99B86)
    val glow = Color(0xFFF2C14E)
    val glowHi = Color(0xFFFFE09A)
    val wax = Color(0xFF7A2A22)
    val stripe = Color(0xFFC0453A)
    val scrim = Color(0xCC0D0B0A)
}

val Display = FontFamily(
    Font(R.font.old_standard_regular, FontWeight.Normal),
    Font(R.font.old_standard_bold, FontWeight.Bold),
    Font(R.font.old_standard_italic, FontWeight.Normal, FontStyle.Italic),
)
/** The pet's own voice and the epitaphs: a hand. */
val Hand = FontFamily(Font(R.font.neucha, FontWeight.Normal))

object T {
    val Title = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 33.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Display, fontSize = 16.sp, lineHeight = 22.sp, color = C.ink)
    val BodyB = Body.copy(fontWeight = FontWeight.Bold)
    val Italic = Body.copy(fontStyle = FontStyle.Italic)
    val Small = TextStyle(fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.5.sp, lineHeight = 19.sp, color = C.ink2)
    val Label = TextStyle(fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 18.sp, color = C.ink2)
    val Button = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = .3.sp)
    /** What the pet says. */
    val Voice = TextStyle(fontFamily = Hand, fontSize = 18.sp, lineHeight = 22.sp, color = C.ink)
}

@Composable
fun BedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.ink, onPrimary = C.paper, background = C.paper, surface = C.paper, onSurface = C.ink,
            onBackground = C.ink, outline = C.line, error = C.wax,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
