package moe.ouom.neriplayer.ui.haptic

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
 * File: moe.ouom.neriplayer.util/Haptic
 * Created: 2025/8/13
 */
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvFocusRing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val VIBRATION_EFFECT_CLICK = 0
private const val VIBRATION_EFFECT_DOUBLE_CLICK = 1
private const val VIBRATION_EFFECT_TICK = 2
private const val VIBRATION_EFFECT_HEAVY_CLICK = 5

enum class HapticFeedbackEffect(
    val predefinedEffect: Int,
    val fallbackDurationMs: Long,
    val fallbackAmplitude: Int
) {
    Tick(VIBRATION_EFFECT_TICK, 8L, 32),
    Click(VIBRATION_EFFECT_CLICK, 12L, 72),
    Confirm(VIBRATION_EFFECT_DOUBLE_CLICK, 20L, 96),
    Heavy(VIBRATION_EFFECT_HEAVY_CLICK, 24L, 160)
}

@Volatile
var hapticFeedbackEnabled: Boolean = true
    private set

fun syncHapticFeedbackSetting(enabled: Boolean) {
    hapticFeedbackEnabled = enabled
}

fun Context.performHapticFeedback(effect: HapticFeedbackEffect = HapticFeedbackEffect.Click) {
    if (!hapticFeedbackEnabled) return

    val vibrator = getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return

    runCatching {
        vibrator.vibrate(createVibrationEffect(vibrator, effect))
    }
}

private fun createVibrationEffect(
    vibrator: Vibrator,
    effect: HapticFeedbackEffect
): VibrationEffect {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        vibrator.supportsPredefinedEffect(effect.predefinedEffect)
    ) {
        return VibrationEffect.createPredefined(effect.predefinedEffect)
    }
    return VibrationEffect.createOneShot(effect.fallbackDurationMs, effect.fallbackAmplitude)
}

private fun Vibrator.supportsPredefinedEffect(effect: Int): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
    return isPredefinedEffectSupportUsable(areEffectsSupported(effect).firstOrNull())
}

@RequiresApi(Build.VERSION_CODES.R)
internal fun isPredefinedEffectSupportUsable(support: Int?): Boolean =
    support == Vibrator.VIBRATION_EFFECT_SUPPORT_YES ||
        support == Vibrator.VIBRATION_EFFECT_SUPPORT_UNKNOWN

private fun hapticClick(
    context: Context,
    effect: HapticFeedbackEffect,
    onClick: () -> Unit
): () -> Unit = {
    context.performHapticFeedback(effect)
    onClick()
}

@Composable
fun HapticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRingColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isTelevision = LocalIsTelevision.current
    val ringColor = focusRingColor ?: MaterialTheme.colorScheme.primary
    IconButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        // Material3 的 IconButton 内部硬写 ripple，读不到 LocalIndication，
        // 电视上必须自己补一圈焦点环，否则播放页看不到光标
        modifier = if (isTelevision) modifier.tvFocusRing(ringColor) else modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun HapticFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    focusRingColor: Color? = null,
    /**
     * 焦点环往里收多少。
     *
     * M3 的实心图标按钮**底色固定画在 40dp 的 state layer 上**，外面再套 size(44dp) 时，
     * 节点是 44dp、可见的圆只有 40dp —— 环画在节点边界上就比圆大一圈。
     * 这类按钮传 2dp 就能让环落在圆的边缘上。
     */
    ringInset: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val isTelevision = LocalIsTelevision.current
    // 实心按钮的底色本身就是 primary，环再用 primary 等于什么也看不见，
    // 所以默认改用前景色，保证在任何底色上都有对比
    val ringColor = focusRingColor ?: MaterialTheme.colorScheme.onSurface
    FilledIconButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Confirm, onClick),
        modifier = if (isTelevision) {
            modifier.tvFocusRing(
                ringColor,
                insetHorizontal = ringInset,
                insetVertical = ringInset
            )
        } else {
            modifier
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        content = content
    )
}

@Composable
fun HapticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val isTelevision = LocalIsTelevision.current
    val ringColor = MaterialTheme.colorScheme.primary
    Button(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Confirm, onClick),
        modifier = if (isTelevision) modifier.tvFocusRing(ringColor) else modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun HapticTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val isTelevision = LocalIsTelevision.current
    val ringColor = MaterialTheme.colorScheme.primary
    TextButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        modifier = if (isTelevision) modifier.tvFocusRing(ringColor) else modifier,
        enabled = enabled,
        shape = ButtonDefaults.shape,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun HapticOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val isTelevision = LocalIsTelevision.current
    val ringColor = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        modifier = if (isTelevision) modifier.tvFocusRing(ringColor) else modifier,
        enabled = enabled,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun HapticFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticEffect: HapticFeedbackEffect = HapticFeedbackEffect.Confirm,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = hapticClick(LocalContext.current, hapticEffect, onClick),
        modifier = modifier,
        content = content
    )
}
