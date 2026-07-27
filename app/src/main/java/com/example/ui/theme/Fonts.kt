package com.example.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.R

enum class AppFontId {
    SYSTEM,
    ROBOTO_FLEX,
    INTER,
    POPPINS,
    NUNITO,
    QUICKSAND,
    MONTSERRAT,
    LATO,
    PLAYFAIR_DISPLAY,
    COMFORTAA,
}

data class FontOption(val id: AppFontId, val label: String, val googleFontName: String?)

val FONT_OPTIONS: List<FontOption> = listOf(
    FontOption(AppFontId.SYSTEM, "Padrão do Sistema", null),
    FontOption(AppFontId.ROBOTO_FLEX, "Roboto Flex", "Roboto Flex"),
    FontOption(AppFontId.INTER, "Inter", "Inter"),
    FontOption(AppFontId.POPPINS, "Poppins", "Poppins"),
    FontOption(AppFontId.NUNITO, "Nunito", "Nunito"),
    FontOption(AppFontId.QUICKSAND, "Quicksand", "Quicksand"),
    FontOption(AppFontId.MONTSERRAT, "Montserrat", "Montserrat"),
    FontOption(AppFontId.LATO, "Lato", "Lato"),
    FontOption(AppFontId.PLAYFAIR_DISPLAY, "Playfair Display", "Playfair Display"),
    FontOption(AppFontId.COMFORTAA, "Comfortaa", "Comfortaa"),
)

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

fun fontFamilyFor(fontId: AppFontId): FontFamily {
    val option = FONT_OPTIONS.find { it.id == fontId } ?: FONT_OPTIONS.first()
    val googleFontName = option.googleFontName ?: return FontFamily.Default
    val googleFont = GoogleFont(googleFontName)
    return FontFamily(Font(googleFont = googleFont, fontProvider = googleFontProvider))
}
