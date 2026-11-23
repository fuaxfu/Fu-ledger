package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.AppColorTheme
import com.example.data.local.ThemeMode

private val EmeraldDarkScheme = darkColorScheme(
  primary = ForestPrimaryDark,
  onPrimary = OnForestPrimaryDark,
  primaryContainer = ForestContainerDark,
  onPrimaryContainer = OnForestContainerDark,
  secondary = EmeraldSecondaryDark,
  onSecondary = OnEmeraldSecondaryDark,
  secondaryContainer = EmeraldContainerDark,
  onSecondaryContainer = OnEmeraldContainerDark,
  tertiary = AmberTertiaryDark,
  onTertiary = OnAmberTertiaryDark,
  tertiaryContainer = AmberContainerDark,
  onTertiaryContainer = OnAmberContainerDark,
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  error = ExpenseCoralSoft,
)

private val EmeraldLightScheme = lightColorScheme(
  primary = ForestPrimaryLight,
  onPrimary = OnForestPrimaryLight,
  primaryContainer = ForestContainerLight,
  onPrimaryContainer = OnForestContainerLight,
  secondary = EmeraldSecondaryLight,
  onSecondary = OnEmeraldSecondaryLight,
  secondaryContainer = EmeraldContainerLight,
  onSecondaryContainer = OnEmeraldContainerLight,
  tertiary = AmberTertiaryLight,
  onTertiary = OnAmberTertiaryLight,
  tertiaryContainer = AmberContainerLight,
  onTertiaryContainer = OnAmberContainerLight,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  error = ExpenseCoral,
)

private val SapphireLightScheme = lightColorScheme(
  primary = Color(0xFF1D4ED8),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDBEAFE),
  onPrimaryContainer = Color(0xFF0A2266),
  secondary = Color(0xFF0891B2),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCFFAFE),
  onSecondaryContainer = Color(0xFF083344),
  tertiary = Color(0xFFB45309),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFEF3C7),
  onTertiaryContainer = Color(0xFF451A03),
  background = Color(0xFFF4F6FA),
  onBackground = Color(0xFF111827),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF111827),
  surfaceVariant = Color(0xFFE5EAF4),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = ExpenseCoral,
)

private val SapphireDarkScheme = darkColorScheme(
  primary = Color(0xFF60A5FA),
  onPrimary = Color(0xFF09255E),
  primaryContainer = Color(0xFF1E3A8A),
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = Color(0xFF38BDF8),
  onSecondary = Color(0xFF082F49),
  secondaryContainer = Color(0xFF164E63),
  onSecondaryContainer = Color(0xFFCFFAFE),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = Color(0xFF78350F),
  onTertiaryContainer = Color(0xFFFEF3C7),
  background = Color(0xFF090E1A),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF11192B),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF1C2740),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF334155),
  error = ExpenseCoralSoft,
)

private val GoldLightScheme = lightColorScheme(
  primary = Color(0xFFB45309),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFEF3C7),
  onPrimaryContainer = Color(0xFF451A03),
  secondary = Color(0xFF0F766E),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCCFBF1),
  onSecondaryContainer = Color(0xFF042F2E),
  tertiary = Color(0xFF374151),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFE5E7EB),
  onTertiaryContainer = Color(0xFF111827),
  background = Color(0xFFFAF8F3),
  onBackground = Color(0xFF1C1917),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF1C1917),
  surfaceVariant = Color(0xFFEDE8DD),
  onSurfaceVariant = Color(0xFF57534E),
  outline = Color(0xFFD6CFC2),
  error = ExpenseCoral,
)

private val GoldDarkScheme = darkColorScheme(
  primary = Color(0xFFF59E0B),
  onPrimary = Color(0xFF3F1D00),
  primaryContainer = Color(0xFF78350F),
  onPrimaryContainer = Color(0xFFFEF3C7),
  secondary = Color(0xFF2DD4BF),
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = Color(0xFF115E59),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = Color(0xFFD1D5DB),
  onTertiary = Color(0xFF1F2937),
  tertiaryContainer = Color(0xFF374151),
  onTertiaryContainer = Color(0xFFF3F4F6),
  background = Color(0xFF11100D),
  onBackground = Color(0xFFF5F5F4),
  surface = Color(0xFF1C1915),
  onSurface = Color(0xFFF5F5F4),
  surfaceVariant = Color(0xFF2B2620),
  onSurfaceVariant = Color(0xFFA8A29E),
  outline = Color(0xFF44403C),
  error = ExpenseCoralSoft,
)

private val CrimsonLightScheme = lightColorScheme(
  primary = Color(0xFF9F1239),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFE4E6),
  onPrimaryContainer = Color(0xFF4C0519),
  secondary = Color(0xFF0D9488),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFCCFBF1),
  onSecondaryContainer = Color(0xFF042F2E),
  tertiary = Color(0xFFB45309),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFEF3C7),
  onTertiaryContainer = Color(0xFF451A03),
  background = Color(0xFFFAF6F7),
  onBackground = Color(0xFF1F1619),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF1F1619),
  surfaceVariant = Color(0xFFF2E6E9),
  onSurfaceVariant = Color(0xFF5D4B51),
  outline = Color(0xFFDEC8CE),
  error = ExpenseCoral,
)

private val CrimsonDarkScheme = darkColorScheme(
  primary = Color(0xFFFB7185),
  onPrimary = Color(0xFF4C0519),
  primaryContainer = Color(0xFF881337),
  onPrimaryContainer = Color(0xFFFFE4E6),
  secondary = Color(0xFF5EEAD4),
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = Color(0xFF115E59),
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = Color(0xFFFBBF24),
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = Color(0xFF78350F),
  onTertiaryContainer = Color(0xFFFEF3C7),
  background = Color(0xFF140D10),
  onBackground = Color(0xFFF8F2F4),
  surface = Color(0xFF1F1418),
  onSurface = Color(0xFFF8F2F4),
  surfaceVariant = Color(0xFF302026),
  onSurfaceVariant = Color(0xFFB8A3AA),
  outline = Color(0xFF4A343C),
  error = ExpenseCoralSoft,
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BirrLedgerTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  colorTheme: AppColorTheme = AppColorTheme.EMERALD_FOREST,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme: ColorScheme = when (colorTheme) {
    AppColorTheme.DYNAMIC_SYSTEM -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (isDark) EmeraldDarkScheme else EmeraldLightScheme
      }
    }
    AppColorTheme.EMERALD_FOREST -> if (isDark) EmeraldDarkScheme else EmeraldLightScheme
    AppColorTheme.ROYAL_SAPPHIRE -> if (isDark) SapphireDarkScheme else SapphireLightScheme
    AppColorTheme.OBSIDIAN_GOLD -> if (isDark) GoldDarkScheme else GoldLightScheme
    AppColorTheme.CRIMSON_SLATE -> if (isDark) CrimsonDarkScheme else CrimsonLightScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
