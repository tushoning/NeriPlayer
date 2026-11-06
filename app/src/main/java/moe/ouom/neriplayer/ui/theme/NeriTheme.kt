package moe.ouom.neriplayer.ui.theme

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.theme/NeriTheme
 * Created: 2025/8/8
 */

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import moe.ouom.neriplayer.data.model.settings.appearance.UiStyle
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.CleanTvRingRadius
import moe.ouom.neriplayer.ui.tv.DefaultTvRingRadius
import moe.ouom.neriplayer.ui.tv.TvFocusIndication
import moe.ouom.neriplayer.ui.tv.TvMinimalBringIntoViewSpec
import moe.ouom.neriplayer.ui.tv.TvPerf
import moe.ouom.neriplayer.ui.tv.TvTuning
import moe.ouom.neriplayer.ui.tv.rememberIsTelevision
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.toColorInt
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import moe.ouom.neriplayer.data.settings.appearance.ThemeDefaults

private val NeriTypography = Typography()
// popups and menus read extraSmall; 4dp reads as a hard rectangle at modern densities
private val NeriShapes = Shapes(extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(12.dp))
private const val ThemeColorTransitionDurationMs = 420

internal val LocalNeriTargetColorScheme = staticCompositionLocalOf {
    lightColorScheme()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NeriTheme(
    followSystemDark: Boolean,
    forceDark: Boolean,
    dynamicColor: Boolean,
    seedColorHex: String,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    systemDark: Boolean? = null,
    uiStyle: UiStyle = UiStyle.Material,
    liquidGlass: Boolean = false,
    animateColors: Boolean = true,
    neutralSurfaces: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val isDark = when {
        forceDark -> true
        followSystemDark -> systemDark ?: isSystemInDarkTheme()
        else -> false
    }

    val accentColorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            // 兜底非法 hex: 解析前归一, 避免坏值使 toColorInt 在主题组合期抛异常崩溃
            val seed = Color(("#${ThemeDefaults.sanitizeSeedColorHex(seedColorHex)}").toColorInt())
            rememberDynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                // 黑白灰封面的色相不可信, 按调色风格硬补色度会染成紫色
                style = resolveSeedPaletteStyle(seed, paletteStyle),
                specVersion = colorSpec
            )
        }
    }
    val targetColorScheme = remember(accentColorScheme, uiStyle, isDark, neutralSurfaces) {
        if (uiStyle == UiStyle.Clean && neutralSurfaces) {
            cleanColorScheme(accentColorScheme, isDark)
        } else {
            accentColorScheme
        }
    }
    val colorScheme = if (animateColors && !rememberIsTelevision()) {
        animateColorScheme(targetColorScheme)
    } else {
        // 电视上不做整套配色的逐帧插值：冷启动和切歌都要把几十个颜色槽各跑一遍动画，
        // 弱 SoC 上纯属浪费
        targetColorScheme
    }
    val baseTokens = neriDesignTokensFor(uiStyle)
    val designTokens = remember(baseTokens, liquidGlass) {
        baseTokens.copy(liquidGlass = liquidGlass && uiStyle == UiStyle.Clean)
    }

    CompositionLocalProvider(
        LocalNeriTargetColorScheme provides targetColorScheme,
        LocalNeriDesign provides designTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (uiStyle == UiStyle.Clean) CleanTypography else NeriTypography,
            shapes = if (uiStyle == UiStyle.Clean) CleanShapes else NeriShapes
        ) {
            // one provider for both styles: branching around content() would remount the
            // whole app (and its NavController) whenever the style changes
            val materialIndication = LocalIndication.current
            val highlight = rememberUpdatedState(colorScheme.onSurface)
            val cleanIndication = remember { CleanPressIndication(highlight) }
            // 电视上没有触摸屏，必须让焦点可见，否则遥控器无法判断当前选中项
            val isTelevision = rememberIsTelevision()
            val density = LocalDensity.current
            val tvRingColor = rememberUpdatedState(colorScheme.primary)
            // 焦点环圆角跟随 app 自己的卡片圆角，避免切线跟底板对不上
            // 卡片绝大多数是 12dp 圆角，环写 16dp 会比卡片圆一圈，看着就是「框和边没对齐」
            val tvRingRadius = if (uiStyle == UiStyle.Clean) CleanTvRingRadius else DefaultTvRingRadius
            val tvIndication = remember(tvRingRadius) {
                TvFocusIndication(tvRingColor, cornerRadius = tvRingRadius)
            }
            // 电视上关掉所有装饰性动效（标签切换、页面进出场），只保留播放页动画
            // 玻璃模糊同样关掉：RenderEffect 在电视 GPU 上每帧都要重新取样，是最大的一个开销
            SideEffect {
                TvPerf.reduceMotion = isTelevision
                TvPerf.disableBlur = isTelevision
                TvPerf.disableOffscreenMasks = isTelevision
            }
            // Material3 的组件（Surface/Card/Button/列表项）内部硬写 ripple()，不读 LocalIndication，
            // 所以额外把 ripple 的焦点态叠加强化，否则电视上看不出光标停在哪
            val tvRipple = remember(colorScheme.primary) {
                RippleConfiguration(
                    color = colorScheme.primary,
                    rippleAlpha = RippleAlpha(
                        draggedAlpha = 0.20f,
                        focusedAlpha = 0.34f,
                        hoveredAlpha = 0.10f,
                        pressedAlpha = 0.28f
                    )
                )
            }
            CompositionLocalProvider(
                LocalIndication provides when {
                    // 诊断：neri_tv_ring=0 时电视也不挂焦点环，退回普通 indication，
                    // 用来量化「每个可聚焦项一个焦点环节点」到底占多少绘制成本
                    isTelevision && TvTuning.focusRing -> tvIndication
                    uiStyle == UiStyle.Clean -> cleanIndication
                    else -> materialIndication
                },
                LocalIsTelevision provides isTelevision,
                LocalRippleConfiguration provides if (isTelevision) tvRipple else null,
                // 电视上把「焦点滚进可视区」改成最小滚动。
                // 系统默认在 leanback 设备上用的是 Pivot 规范（把焦点项拽到视口中间），
                // 那正是「焦点框钉在第三格 / 卡片往回挪」的根源。
                LocalBringIntoViewSpec provides if (isTelevision) {
                    // 多留 12dp：卡片滚动停住时会被推进屏幕内部一段，
                    // 外扩的焦点环不再贴着视口边缘被裁掉。
                    // 动画时长走 TvTuning.scrollAnimMs（adb 可调，见该字段注释）。
                    remember(density, TvTuning.scrollAnimMs) {
                        TvMinimalBringIntoViewSpec(
                            marginPx = with(density) { TvFocusScrollMargin.toPx() },
                            durationMs = TvTuning.scrollAnimMs
                        )
                    }
                } else {
                    LocalBringIntoViewSpec.current
                },
                content = content
            )
        }
    }
}

private const val AchromaticSeedChromaThreshold = 0.06f

/** 电视上焦点滚动时额外留出的边距：卡片会被推进屏幕内部这么多，外扩的环不会被裁（24 太远，减半） */
private val TvFocusScrollMargin = 12.dp

internal fun isAchromaticSeed(seed: Color): Boolean {
    val maxChannel = maxOf(seed.red, seed.green, seed.blue)
    val minChannel = minOf(seed.red, seed.green, seed.blue)
    return maxChannel - minChannel < AchromaticSeedChromaThreshold
}

internal fun resolveSeedPaletteStyle(seed: Color, requested: PaletteStyle): PaletteStyle =
    if (isAchromaticSeed(seed)) PaletteStyle.Monochrome else requested

private const val CoverSeedBlueHue = 218f
private const val CoverSeedDarkLightness = 0.28f
private const val CoverSeedDarkFloor = 0.06f
private const val CoverSeedGreyChroma = 0.12f

/**
 * Near-black or grey covers have no trustworthy hue; tinting from them yields a muddy pink or
 * purple accent with poor contrast. Such seeds are steered toward blue in proportion to how
 * dark or grey they are, so colourful covers keep their own accent untouched.
 */
internal fun biasCoverSeedTowardBlue(hex: String): String {
    val rgb = ThemeDefaults.sanitizeSeedColorHex(hex).toInt(16)
    val r = (rgb shr 16 and 0xFF) / 255f
    val g = (rgb shr 8 and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val chroma = max - min
    val lightness = (max + min) / 2f
    val hue = when {
        chroma == 0f -> CoverSeedBlueHue
        max == r -> 60f * (((g - b) / chroma) % 6f)
        max == g -> 60f * ((b - r) / chroma + 2f)
        else -> 60f * ((r - g) / chroma + 4f)
    }.let { (it + 360f) % 360f }
    val saturation = if (chroma == 0f) 0f else chroma / (1f - kotlin.math.abs(2f * lightness - 1f))
    val darkWeight = ((CoverSeedDarkLightness - lightness) /
        (CoverSeedDarkLightness - CoverSeedDarkFloor)).coerceIn(0f, 1f)
    val greyWeight = ((CoverSeedGreyChroma - chroma) / CoverSeedGreyChroma).coerceIn(0f, 1f)
    val weight = maxOf(darkWeight, greyWeight)
    if (weight <= 0f) return hex
    val hueDelta = ((CoverSeedBlueHue - hue) % 360f + 540f) % 360f - 180f
    val biased = Color.hsl(
        hue = (hue + hueDelta * weight + 360f) % 360f,
        saturation = (saturation + (0.62f - saturation) * weight).coerceIn(0f, 1f),
        lightness = (lightness + (0.52f - lightness) * weight).coerceIn(0f, 1f)
    )
    return String.format(java.util.Locale.ROOT, "%06X", biased.toArgb() and 0xFFFFFF)
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateThemeColor(target.primary, "theme-primary"),
        onPrimary = animateThemeColor(target.onPrimary, "theme-on-primary"),
        primaryContainer = animateThemeColor(target.primaryContainer, "theme-primary-container"),
        onPrimaryContainer = animateThemeColor(target.onPrimaryContainer, "theme-on-primary-container"),
        inversePrimary = animateThemeColor(target.inversePrimary, "theme-inverse-primary"),
        secondary = animateThemeColor(target.secondary, "theme-secondary"),
        onSecondary = animateThemeColor(target.onSecondary, "theme-on-secondary"),
        secondaryContainer = animateThemeColor(target.secondaryContainer, "theme-secondary-container"),
        onSecondaryContainer = animateThemeColor(target.onSecondaryContainer, "theme-on-secondary-container"),
        tertiary = animateThemeColor(target.tertiary, "theme-tertiary"),
        onTertiary = animateThemeColor(target.onTertiary, "theme-on-tertiary"),
        tertiaryContainer = animateThemeColor(target.tertiaryContainer, "theme-tertiary-container"),
        onTertiaryContainer = animateThemeColor(target.onTertiaryContainer, "theme-on-tertiary-container"),
        background = animateThemeColor(target.background, "theme-background"),
        onBackground = animateThemeColor(target.onBackground, "theme-on-background"),
        surface = animateThemeColor(target.surface, "theme-surface"),
        onSurface = animateThemeColor(target.onSurface, "theme-on-surface"),
        surfaceVariant = animateThemeColor(target.surfaceVariant, "theme-surface-variant"),
        onSurfaceVariant = animateThemeColor(target.onSurfaceVariant, "theme-on-surface-variant"),
        surfaceTint = animateThemeColor(target.surfaceTint, "theme-surface-tint"),
        inverseSurface = animateThemeColor(target.inverseSurface, "theme-inverse-surface"),
        inverseOnSurface = animateThemeColor(target.inverseOnSurface, "theme-inverse-on-surface"),
        error = animateThemeColor(target.error, "theme-error"),
        onError = animateThemeColor(target.onError, "theme-on-error"),
        errorContainer = animateThemeColor(target.errorContainer, "theme-error-container"),
        onErrorContainer = animateThemeColor(target.onErrorContainer, "theme-on-error-container"),
        outline = animateThemeColor(target.outline, "theme-outline"),
        outlineVariant = animateThemeColor(target.outlineVariant, "theme-outline-variant"),
        scrim = animateThemeColor(target.scrim, "theme-scrim"),
        surfaceBright = animateThemeColor(target.surfaceBright, "theme-surface-bright"),
        surfaceDim = animateThemeColor(target.surfaceDim, "theme-surface-dim"),
        surfaceContainer = animateThemeColor(target.surfaceContainer, "theme-surface-container"),
        surfaceContainerHigh = animateThemeColor(target.surfaceContainerHigh, "theme-surface-container-high"),
        surfaceContainerHighest = animateThemeColor(target.surfaceContainerHighest, "theme-surface-container-highest"),
        surfaceContainerLow = animateThemeColor(target.surfaceContainerLow, "theme-surface-container-low"),
        surfaceContainerLowest = animateThemeColor(target.surfaceContainerLowest, "theme-surface-container-lowest"),
        primaryFixed = animateThemeColor(target.primaryFixed, "theme-primary-fixed"),
        primaryFixedDim = animateThemeColor(target.primaryFixedDim, "theme-primary-fixed-dim"),
        onPrimaryFixed = animateThemeColor(target.onPrimaryFixed, "theme-on-primary-fixed"),
        onPrimaryFixedVariant = animateThemeColor(target.onPrimaryFixedVariant, "theme-on-primary-fixed-variant"),
        secondaryFixed = animateThemeColor(target.secondaryFixed, "theme-secondary-fixed"),
        secondaryFixedDim = animateThemeColor(target.secondaryFixedDim, "theme-secondary-fixed-dim"),
        onSecondaryFixed = animateThemeColor(target.onSecondaryFixed, "theme-on-secondary-fixed"),
        onSecondaryFixedVariant = animateThemeColor(target.onSecondaryFixedVariant, "theme-on-secondary-fixed-variant"),
        tertiaryFixed = animateThemeColor(target.tertiaryFixed, "theme-tertiary-fixed"),
        tertiaryFixedDim = animateThemeColor(target.tertiaryFixedDim, "theme-tertiary-fixed-dim"),
        onTertiaryFixed = animateThemeColor(target.onTertiaryFixed, "theme-on-tertiary-fixed"),
        onTertiaryFixedVariant = animateThemeColor(target.onTertiaryFixedVariant, "theme-on-tertiary-fixed-variant")
    )
}

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    if (target == Color.Unspecified) return target
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = ThemeColorTransitionDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = label
    )
    return color
}
