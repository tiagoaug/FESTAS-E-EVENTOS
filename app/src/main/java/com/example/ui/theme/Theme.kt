package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily

enum class AppThemeId(val label: String, val swatch: Color) {
    VIOLETA("Violeta", PrimaryViolet),
    OCEANO("Oceano", PrimaryOceano),
    ESMERALDA("Esmeralda", PrimaryEsmeralda),
    CORAL("Coral", PrimaryCoral),
    CLARO("Claro", Color(0xFFB8BEC7)),
}

/**
 * Material3 só substitui os ~15 papéis passados explicitamente a lightColorScheme/
 * darkColorScheme; todo o resto (surfaceContainer, surfaceContainerHigh, outline etc,
 * usados por AlertDialog e outros "submenus") cai no roxo padrão da paleta base do
 * M3 — por isso diálogos ficavam sempre com a mesma cor roxa, ignorando o tema
 * escolhido. Aqui derivamos esses papéis a partir da própria cor primária do tema.
 */
private fun ColorScheme.withThemedContainers(isDark: Boolean): ColorScheme {
    fun tinted(fraction: Float) = lerp(surface, primary, fraction)
    return copy(
        surfaceDim = lerp(surface, onSurface, if (isDark) 0f else 0.08f),
        surfaceBright = lerp(surface, Color.White, if (isDark) 0.16f else 0f),
        surfaceContainerLowest = if (isDark) lerp(surface, Color.Black, 0.06f) else surface,
        surfaceContainerLow = tinted(if (isDark) 0.05f else 0.03f),
        surfaceContainer = tinted(if (isDark) 0.08f else 0.05f),
        surfaceContainerHigh = tinted(if (isDark) 0.11f else 0.08f),
        surfaceContainerHighest = tinted(if (isDark) 0.14f else 0.11f),
        outline = lerp(onSurface, primary, 0.3f),
        outlineVariant = surfaceVariant,
    )
}

private fun buildLightScheme(primary: Color, primaryContainer: Color, onPrimaryContainer: Color): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = SecondaryLav,
        onSecondary = Color.White,
        secondaryContainer = SecondaryLavContainer,
        onSecondaryContainer = OnSecondaryLavContainer,
        tertiary = TertiaryRose,
        onTertiary = Color.White,
        tertiaryContainer = TertiaryRoseContainer,
        onTertiaryContainer = OnTertiaryRoseContainer,
        background = BackgroundVibrant,
        onBackground = OnBackgroundVibrant,
        surface = SurfaceWhite,
        onSurface = OnSurfaceVibrant,
        surfaceVariant = SurfaceVariantVibrant,
        onSurfaceVariant = OnSurfaceVariantVibrant,
    ).withThemedContainers(isDark = false)

/**
 * Tema "Claro": fundo branco vidro e tons prateados/acinzentados neutros,
 * sem tingimento de cor — mais suave e translúcido que os outros temas.
 */
private fun buildNeutralLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF98A2AE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4F6F8),
    onPrimaryContainer = Color(0xFF3F4750),
    secondary = Color(0xFFAAB1BA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F6F8),
    onSecondaryContainer = Color(0xFF3F4750),
    tertiary = Color(0xFFC0C5CC),
    onTertiary = Color(0xFF3F4750),
    tertiaryContainer = Color(0xFFF9FAFB),
    onTertiaryContainer = Color(0xFF3F4750),
    background = Color.White,
    onBackground = Color(0xFF2B3238),
    surface = Color.White,
    onSurface = Color(0xFF2B3238),
    surfaceVariant = Color(0xFFF4F6F8),
    onSurfaceVariant = Color(0xFF6B7280),
).withThemedContainers(isDark = false)

private fun buildNeutralDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFC0C5CC),
    onPrimary = Color(0xFF2B3238),
    primaryContainer = Color(0xFF98A2AE),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFAAB1BA),
    onSecondary = Color(0xFF2B3238),
    background = Color(0xFF1C1F22),
    surface = Color(0xFF1C1F22),
    surfaceVariant = Color(0xFF2A2E33),
).withThemedContainers(isDark = true)

private fun buildDarkScheme(primary: Color, primaryContainer: Color, onPrimaryContainer: Color): ColorScheme =
    darkColorScheme(
        primary = primaryContainer,
        onPrimary = onPrimaryContainer,
        primaryContainer = primary,
        onPrimaryContainer = Color.White,
        secondary = SecondaryLavContainer,
        onSecondary = OnSecondaryLavContainer,
        background = Color(0xFF141218),
        surface = Color(0xFF141218),
        surfaceVariant = Color(0xFF2B2832),
    ).withThemedContainers(isDark = true)

private val lightSchemes: Map<AppThemeId, ColorScheme> = mapOf(
    AppThemeId.VIOLETA to buildLightScheme(PrimaryViolet, PrimaryVioletContainer, OnPrimaryVioletContainer),
    AppThemeId.OCEANO to buildLightScheme(PrimaryOceano, PrimaryOceanoContainer, OnPrimaryOceanoContainer),
    AppThemeId.ESMERALDA to buildLightScheme(PrimaryEsmeralda, PrimaryEsmeraldaContainer, OnPrimaryEsmeraldaContainer),
    AppThemeId.CORAL to buildLightScheme(PrimaryCoral, PrimaryCoralContainer, OnPrimaryCoralContainer),
    AppThemeId.CLARO to buildNeutralLightScheme(),
)

private val darkSchemes: Map<AppThemeId, ColorScheme> = mapOf(
    AppThemeId.VIOLETA to buildDarkScheme(PrimaryViolet, PrimaryVioletContainer, OnPrimaryVioletContainer),
    AppThemeId.OCEANO to buildDarkScheme(PrimaryOceano, PrimaryOceanoContainer, OnPrimaryOceanoContainer),
    AppThemeId.ESMERALDA to buildDarkScheme(PrimaryEsmeralda, PrimaryEsmeraldaContainer, OnPrimaryEsmeraldaContainer),
    AppThemeId.CORAL to buildDarkScheme(PrimaryCoral, PrimaryCoralContainer, OnPrimaryCoralContainer),
    AppThemeId.CLARO to buildNeutralDarkScheme(),
)

private fun appTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun MyApplicationTheme(
    themeId: AppThemeId = AppThemeId.VIOLETA,
    fontId: AppFontId = AppFontId.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkSchemes[themeId] ?: darkSchemes.getValue(AppThemeId.VIOLETA)
    } else {
        lightSchemes[themeId] ?: lightSchemes.getValue(AppThemeId.VIOLETA)
    }
    val fontFamily = remember(fontId) { fontFamilyFor(fontId) }
    val typography = remember(fontFamily) { appTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
