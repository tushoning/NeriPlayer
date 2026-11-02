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

    /**
     * 关掉「离屏图层 + DstIn 渐变遮罩」这类边缘渐隐装饰。
     *
     * 它的实现是 `compositingStrategy = Offscreen` + `BlendMode.DstIn`：内容要先整块渲染进
     * 一张离屏纹理，再拿渐变去混合一次。手机上有 GPU 分块（tile）和充足带宽，几乎不花钱；
     * 电视盒子（尤其 1080p 大面积的歌词区域）每帧都要多分配、多写、多读一张全程纹理，
     * 是这批装饰里最贵的一档，比模糊更贵，因为它不依赖 Android 12 的 RenderEffect。
     */
    @Volatile
    var disableOffscreenMasks: Boolean = false
}
