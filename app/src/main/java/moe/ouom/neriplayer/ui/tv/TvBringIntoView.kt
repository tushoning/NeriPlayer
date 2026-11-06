package moe.ouom.neriplayer.ui.tv

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
 * File: moe.ouom.neriplayer.ui.tv/TvBringIntoView
 * Created: 2026/10/31
 */

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * 电视端的「把焦点滚进可视区」策略：只滚**刚好露出**，绝不居中。
 *
 * 这是「焦点框钉在第三个槽位」这类问题的根因修复：
 *
 * Compose 的 `Modifier.focusable`（`clickable` 里也有它）在拿到焦点时会自动请求一次
 * bring-into-view（`FocusableNode.onFocusStateChange` 里发起的），滚动多少由
 * `LocalBringIntoViewSpec` 决定。而这个 composition local 的默认值在
 * `androidx/compose/foundation/gestures/BringIntoViewSpec.android.kt` 里是按设备类型分支的：
 *
 * ```kotlin
 * if (context.packageManager.hasSystemFeature("android.software.leanback"))
 *     PivotBringIntoViewSpec      // 电视：把焦点项摆到视口的「支点」上（约等于居中）
 * else
 *     BringIntoViewSpec.Default   // 手机：最小滚动
 * ```
 *
 * 于是电视上每按一次方向键，可滚动容器都会把刚获得焦点的那张卡片**往屏幕中间拽**：
 * 焦点框看起来钉在中间（第三格），后面的卡片被一路往左挤；按右时「卡片往左挪、
 * 焦点也跟着往左挪」正是它。横向卡片条、页内标签行、纵向网格全都有这个问题。
 *
 * 换成最小滚动之后：卡片露不全才滚、滚多少刚好露出就滚多少，焦点就能一路顺着
 * 走到这一条的边缘，再按方向键才整体挪一格 —— 和手机上、和「继续播放」分页器一致。
 */
internal class TvMinimalBringIntoViewSpec(
    /**
     * 滚动时额外留出的边距（px）。
     *
     * 只有「刚好露出」时，卡片会贴着视口上下边缘停住，卡片外侧那一圈焦点环
     * 会被滚动容器直接裁掉一条。留出这段边距后，最后一行/第一行会被推进屏幕内部
     * 一段距离，环能完整看见。
     */
    private val marginPx: Float = 0f,
    /**
     * 一次焦点滚动的动画时长（毫秒）。`<= 0` 表示瞬时。
     *
     * 运行时可调（`neri_tv_scroll_ms`），方便现场试手感：
     * ```
     * adb shell settings put global neri_tv_scroll_ms 0     # 瞬时，最跟手
     * adb shell settings put global neri_tv_scroll_ms 32    # 默认
     * adb shell settings put global neri_tv_scroll_ms 100   # 系统默认长度
     * ```
     */
    private val durationMs: Int = 32
) : BringIntoViewSpec {

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (containerSize <= 0f) return 0f
        // 比视口还大：对齐起始边，别去居中
        if (size > containerSize) return offset
        // 边距不能超过「视口比内容多出来的余量」的一半，否则上下两边的需求会互相打架、
        // 表现成来回抖
        val slack = ((containerSize - size) / 2f).coerceAtLeast(0f)
        val margin = marginPx.coerceIn(0f, slack)
        return when {
            offset < margin -> offset - margin
            offset + size > containerSize - margin -> offset + size - containerSize + margin
            else -> 0f
        }
    }

    /**
     * 焦点滚动的动画规格：**短时长 + 匀速**。
     *
     * 系统默认是一段 ~100ms 的缓动动画。快速连按方向键时它有三个问题：
     *
     * 1. **每次新请求都会打断上一段动画**，从「当前位置」重新起算。按得越快，
     *    每段动画走得越少 —— 表现就是「焦点框跑出好远，页面却几乎不动」；
     *    一停手，最后那段动画才慢慢把页面补下去（看起来像「边掉帧边往下走」）。
     * 2. 焦点项往往嵌在**多层滚动容器**里（卡片 → 区块的 Pager → 整页的 LazyColumn）。
     *    bring-into-view 会沿祖先链逐层请求，每层各起一段动画，串起来就是
     *    「停顿一下 → 往上窜一下 → 再停顿 → 再窜一下」。
     * 3. 缓动曲线在 100ms 这种短时长下，起步那一段几乎不动，主观上就是「按了没反应」。
     *
     * 所以这里改成 **60ms 匀速**：
     * - 时长足够短 —— 人手连按的间隔基本都在 80ms 以上，上一段动画已经走完，
     *   于是「按一下 = 滚一段」，连续按就是连续滚，不再分段顿挫；
     * - 仍然有动画 —— 不是瞬间跳，位移是连续可见的，观感顺滑；
     * - 匀速 —— 没有「起步慢」的迟滞感，按下即在动，最接近手机上直接拖动的跟手感。
     *
     * 注：`scrollAnimationSpec` 在新版 foundation 上被标了弃用（不推荐定制），
     * 但它仍是 bring-into-view 取动画规格的唯一入口，覆盖后照常生效。
     */
    @Suppress("DEPRECATION")
    override val scrollAnimationSpec: AnimationSpec<Float> =
        if (durationMs <= 0) snap() else tween(durationMillis = durationMs, easing = LinearEasing)
}
