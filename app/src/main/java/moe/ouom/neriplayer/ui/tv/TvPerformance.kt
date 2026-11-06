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

/**
 * 可以用 adb 现场改的电视开关，用来做 A/B 对照。
 *
 * 改法（改完要重启应用才生效，值在 Application 创建时读一次）：
 * ```
 * adb shell settings put global neri_tv_biv 0   # 关
 * adb shell settings put global neri_tv_biv 1   # 开（默认）
 * ```
 */
object TvTuning {

    /**
     * 焦点落到某一项时，是否要额外请求一次「把我滚进可视区」。
     *
     * 这是我们相对官方包多出来的东西，代价不小：每次焦点变化都要起协程、等一帧、
     * 再调 `BringIntoViewRequester.bringIntoView()`，而 `bringIntoView` 会让滚动容器
     * **强制重新测量**一次。于是一次按键 = 一次焦点变化 = 一次额外的全量测量，
     * 列表越长越贵（实测一次按键最多触发 6 次 `AndroidOwner:measureAndLayout`）。
     *
     * `LocalBringIntoViewSpec`（TvMinimalBringIntoViewSpec）已经把系统自带的
     * 「focusable 自动 bringIntoView」调成了最小滚动，很多场景其实不再需要这一层。
     *
     * **默认已关闭**：`focusable` 在拿到焦点时本来就会自己请求一次 bring-into-view，
     * 这一层再补一次属于重复请求 —— 两次请求会让滚动容器收到两次滚动指令，
     * 在多层嵌套容器里表现成「停顿一下、再窜一下」。实测关掉后帧数据无差异，
     * 手感的改善才是主要收益。
     */
    @Volatile
    var bringIntoViewOnFocus: Boolean = false
        private set

    /**
     * 歌单页头图配色要不要做 360ms 的渐变。
     *
     * 进歌单页时会从封面提取主色，再从「回退色」渐变到「封面色」。这个渐变每帧都会产出
     * 一个新的 `PlaylistHeroVisualColors` 并提供给整棵子树，所有读取它的组件（页头、
     * 操作条、超滚动背景）都会**逐帧重组**约 22 帧 —— 而这正好发生在页面刚进入、
     * 本来就在组合整页的时候。
     *
     * 关掉之后颜色直接到位（`snap()`），观感差别只是少了半秒的淡入。
     */
    @Volatile
    var heroColorFade: Boolean = true
        private set

    /**
     * 诊断开关：是否给每个可聚焦项挂焦点环（`LocalIndication = TvFocusIndication`）。
     *
     * 焦点环是每个可聚焦项一个 `DrawModifierNode`。电视版把几乎所有卡片/行都做成了
     * 可聚焦，节点数比手机原版多 —— 而实测我们与官方的差距主要在 `drawRecord`
     * （绘制录制，和节点数量正相关）。这个开关只用于 A/B 定位成本，不是日常功能
     * （关掉之后电视上就没有焦点框了，没法正常用）。
     */
    @Volatile
    var focusRing: Boolean = true
        private set

    /**
     * 诊断开关：首页卡片区是否退回 `LazyRow`（默认用 `HorizontalPager`）。
     *
     * 电视版为了「焦点能退回上一页」把首页每个区块从 LazyRow 换成了
     * HorizontalPager + BoxWithConstraints。Pager 会预取相邻页、每个区块还多一份
     * 子组合，同样的内容节点更多。设成 true 可对比两种结构的帧成本。
     * （LazyRow 下焦点退回有问题，仅用于诊断。）
     */
    @Volatile
    var useLazyRow: Boolean = false
        private set

    /**
     * 焦点滚动的动画时长（毫秒），`0` 表示瞬时。
     *
     * 这是电视上手感最敏感的一个参数。系统默认约 100ms 缓动，问题有两个：
     * 快速连按时每次新请求都会打断上一段动画、从当前位置重新起算，于是
     * 「焦点跑出好远、页面几乎不动」，松手后才慢慢补；多层滚动容器还会各自
     * 起一段动画，串成「停顿一下、再窜一下」。
     *
     * 运行时可调，方便现场挑手感（改完要重启应用）：
     * ```
     * adb shell settings put global neri_tv_scroll_ms 0     # 瞬时：一格一格跳，按住键就是「一停一停」
     * adb shell settings put global neri_tv_scroll_ms 50    # 更跟手
     * adb shell settings put global neri_tv_scroll_ms 70    # 默认：接近遥控器按键重复间隔，连按能连成连续位移
     * adb shell settings put global neri_tv_scroll_ms 100   # 系统默认长度，起步慢、容易被下一次按键打断
     * ```
     *
     * 为什么默认是 70：遥控器按住不放时按键重复间隔大致在 50~80ms 之间。
     * 动画时长取这个量级，上一次位移刚好在下一次按键到来时走完，连续按就变成连续滚动；
     * 给 0（瞬时）反而是一格一格地跳，给 100 又会被下一段请求反复打断。
     */
    @Volatile
    var scrollAnimMs: Int = 70
        private set

    fun refresh(context: android.content.Context) {
        val resolver = context.contentResolver
        fun flag(key: String, default: Int = 1): Boolean = runCatching {
            android.provider.Settings.Global.getInt(resolver, key, default) != 0
        }.getOrDefault(default != 0)
        // 默认关：见 bringIntoViewOnFocus 的注释（focusable 自带的那次已足够）
        bringIntoViewOnFocus = flag("neri_tv_biv", default = 0)
        // 焦点滚动动画时长（0 = 瞬时）
        scrollAnimMs = runCatching {
            android.provider.Settings.Global.getInt(resolver, "neri_tv_scroll_ms", 32)
        }.getOrDefault(32)
        heroColorFade = flag("neri_tv_hero_fade")
        focusRing = flag("neri_tv_ring")
        // neri_tv_pager 默认 1（用 Pager），设 0 退回 LazyRow
        useLazyRow = !flag("neri_tv_pager")
    }
}
