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
    private val marginPx: Float = 0f
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
}
