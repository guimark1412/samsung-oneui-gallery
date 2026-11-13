package com.oneui.gallery.ui.theme

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt

data class OneUIColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val primaryBlue: Color,
    val galleryRose: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val bottomBar: Color,
    val selectionScrim: Color
)

val DarkOneUIColors = OneUIColorPalette(
    isDark = true,
    background = Color(0xFF000000),
    surfaceCard = Color(0xFF17171A),
    surfaceElevated = Color(0xFF242429),
    primaryBlue = Color(0xFF387AFF),
    galleryRose = Color(0xFFF53855),
    textPrimary = Color(0xFFFAFAFC),
    textSecondary = Color(0xFF8E8E93),
    divider = Color(0xFF2C2C2E),
    bottomBar = Color(0xF20D0D0F),
    selectionScrim = Color(0x55387AFF)
)

val LightOneUIColors = OneUIColorPalette(
    isDark = false,
    background = Color(0xFFF2F2F7),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE9E9EF),
    primaryBlue = Color(0xFF0381FE),
    galleryRose = Color(0xFFF53855),
    textPrimary = Color(0xFF111114),
    textSecondary = Color(0xFF6E6E73),
    divider = Color(0xFFE0E0E5),
    bottomBar = Color(0xF2F7F7FA),
    selectionScrim = Color(0x440381FE)
)

val LocalOneUIColors = staticCompositionLocalOf { DarkOneUIColors }

val OneUIShapeLarge = RoundedCornerShape(26.dp)
val OneUIShapeMedium = RoundedCornerShape(18.dp)
val OneUIShapeSmall = RoundedCornerShape(8.dp)
val OneUIShapeGridDense = RoundedCornerShape(2.dp)

@Composable
fun OneUITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkOneUIColors else LightOneUIColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primaryBlue,
            secondary = palette.galleryRose,
            background = palette.background,
            surface = palette.surfaceCard,
            surfaceVariant = palette.surfaceElevated,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    } else {
        lightColorScheme(
            primary = palette.primaryBlue,
            secondary = palette.galleryRose,
            background = palette.background,
            surface = palette.surfaceCard,
            surfaceVariant = palette.surfaceElevated,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.bottomBar.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalOneUIColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * Samsung One UI Ergonomic Collapsing Header Scaffold.
 * Displays a spacious Viewing Area (~156.dp) with a large centered title when at scroll top,
 * smoothly collapsing into a compact 56.dp action bar as the user scrolls down.
 */
@Composable
fun OneUICollapsingScaffold(
    title: String,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    headerExtra: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val colors = LocalOneUIColors.current
    val density = LocalDensity.current
    val maxExpandPx = with(density) { 148.dp.toPx() }
    var collapseOffsetPx by remember(title) { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(maxExpandPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && collapseOffsetPx < maxExpandPx) {
                    val prev = collapseOffsetPx
                    collapseOffsetPx = (collapseOffsetPx - delta).coerceIn(0f, maxExpandPx)
                    return Offset(0f, -(collapseOffsetPx - prev))
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0f && collapseOffsetPx > 0f) {
                    val prev = collapseOffsetPx
                    collapseOffsetPx = (collapseOffsetPx - delta).coerceIn(0f, maxExpandPx)
                    return Offset(0f, -(collapseOffsetPx - prev))
                }
                return Offset.Zero
            }
        }
    }

    val collapseFraction by animateFloatAsState(
        targetValue = (collapseOffsetPx / maxExpandPx).coerceIn(0f, 1f),
        label = "oneui_collapse"
    )
    val expandedHeightDp = with(density) {
        ((maxExpandPx * (1f - collapseFraction)).roundToInt()).toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Viewing Area (collapses smoothly)
        if (expandedHeightDp > 1.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedHeightDp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val expandedAlpha = (1f - collapseFraction * 1.45f).coerceIn(0f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(expandedAlpha)
                ) {
                    Text(
                        text = title,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Interaction Action Bar (always within easy reach)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            }
            val collapsedTitleAlpha = ((collapseFraction - 0.35f) * 1.54f).coerceIn(0f, 1f)
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (navigationIcon != null) 4.dp else 10.dp)
                    .alpha(collapsedTitleAlpha)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = actions
            )
        }

        if (headerExtra != null) {
            headerExtra()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }

        bottomBar()
    }
}

@Composable
fun OneUISectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalOneUIColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OneUIShapeLarge,
        color = colors.surfaceCard,
        tonalElevation = 0.dp
    ) {
        content()
    }
}
