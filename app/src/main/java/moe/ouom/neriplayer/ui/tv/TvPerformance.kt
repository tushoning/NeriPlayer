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
 * File: moe.ouom.neriplayer.ui.tv/TvPerformance
 * Created: 2026/10/26
 */

/**
 * 电视端的性能开关。
 *
 * 电视盒子/电视机的 SoC 比手机弱得多，Compose 的转场动画、位图模糊、玻璃取样
 * 都是纯装饰，却要吃掉大量 GPU/CPU。这些开关由主题层在电视上统一打开，
 * 播放页的动画不受影响。
 */
object TvPerf {

    /** 关掉页面转场、标签切换、预测性返回预览等装饰性动效 */
    @Volatile
    var reduceMotion: Boolean = false

    /** 关掉玻璃模糊取样 */
    @Volatile
    var disableBlur: Boolean = false
}
