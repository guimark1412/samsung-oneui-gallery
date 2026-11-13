package com.oneui.gallery.ui.theme

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlin.math.abs
import kotlin.math.roundToInt

data class OneUIColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val popupMenu: Color,
    val floatingPill: Color,
    val floatingBorder: Color,
    val primaryBlue: Color,
    val galleryRose: Color,
    val aiAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val bottomBar: Color,
    val selectionScrim: Color
)

val DarkOneUIColors = OneUIColorPalette(
    isDark = true,
    background = Color(0xFF050507),
    surfaceCard = Color(0xFF131419),
    surfaceElevated = Color(0xFF1F2129),
    popupMenu = Color(0xFF1E2028),
    floatingPill = Color(0xEB171820),
    floatingBorder = Color(0x26FFFFFF),
    primaryBlue = Color(0xFF4785FF),
    galleryRose = Color(0xFFF53855),
    aiAccent = Color(0xFF4785FF),
    textPrimary = Color(0xFFF9FAFC),
    textSecondary = Color(0xFF91949F),
    divider = Color(0xFF272933),
    bottomBar = Color(0xEB0A0B0F),
    selectionScrim = Color(0x384785FF)
)

val LightOneUIColors = OneUIColorPalette(
    isDark = false,
    background = Color(0xFFF4F5F8),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE7E9F0),
    popupMenu = Color(0xFFFCFCFE),
    floatingPill = Color(0xF0FFFFFF),
    floatingBorder = Color(0x18000000),
    primaryBlue = Color(0xFF0072DE),
    galleryRose = Color(0xFFF53855),
    aiAccent = Color(0xFF0072DE),
    textPrimary = Color(0xFF0F1014),
    textSecondary = Color(0xFF6B6F7B),
    divider = Color(0xFFE2E4EC),
    bottomBar = Color(0xEBFAFBFD),
    selectionScrim = Color(0x300072DE)
)

val LocalOneUIColors = staticCompositionLocalOf { DarkOneUIColors }

val OneUIShapeLarge = RoundedCornerShape(28.dp)
val OneUIShapeMenu = RoundedCornerShape(22.dp)
val OneUIShapeMedium = RoundedCornerShape(20.dp)
val OneUIShapeSmall = RoundedCornerShape(10.dp)
val OneUIShapeGridDense = RoundedCornerShape(0.dp)

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
                window.navigationBarColor = palette.background.toArgb()
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
 * Samsung One UI 7 / 8 Ergonomic Collapsing Header Scaffold.
 * Displays a spacious Viewing Area at the top (~160.dp) with centered title (34.sp)
 * and subtitle pill badge, collapsing smoothly into a sleek 56.dp toolbar, while
 * allowing the floating One UI 7/8 bottom capsule to float above scrolling content.
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
    val maxExpandPx = with(density) { 158.dp.toPx() }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(title) {
        collapseOffsetPx = 0f
    }

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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "oneui_collapse"
    )
    val expandedHeightDp = with(density) {
        ((maxExpandPx * (1f - collapseFraction)).roundToInt()).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(maxExpandPx) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    var totalDx = 0f
                    var totalDy = 0f
                    var engaged = false
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Final)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size == 1) {
                            val ch = pressed[0]
                            if (!ch.isConsumed) {
                                val delta = ch.positionChange()
                                totalDx += delta.x
                                totalDy += delta.y
                                if (!engaged && abs(totalDy) > 14f && abs(totalDy) > abs(totalDx) * 1.25f) {
                                    engaged = true
                                }
                                if (engaged && delta.y != 0f) {
                                    collapseOffsetPx = (collapseOffsetPx - delta.y).coerceIn(0f, maxExpandPx)
                                }
                            } else {
                                engaged = false
                            }
                        } else {
                            engaged = false
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewing Area (One UI Top Reachability Region)
            if (expandedHeightDp > 1.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(expandedHeightDp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val expandedAlpha = (1f - collapseFraction * 1.45f).coerceIn(0f, 1f)
                    val titleScale = 1f - collapseFraction * 0.12f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .alpha(expandedAlpha)
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                            }
                    ) {
                        Text(
                            text = title,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceCard
                            ) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Interaction Action Bar (56.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                val collapsedTitleAlpha = ((collapseFraction - 0.35f) * 1.54f).coerceIn(0f, 1f)
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (navigationIcon != null) 4.dp else 12.dp)
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
        }

        // Floating Bottom Bar overlaid at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            bottomBar()
        }
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
