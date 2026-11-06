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
 * File: moe.ouom.neriplayer.ui.tv/TvInputActivity
 * Created: 2026/11/03
 */

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 最后一次方向键之后多久算「用户停下来了」。
 *
 * 要盖住两件事：一次 70ms 的焦点滚动动画，以及遥控器松手后可能还在队列里的重复键。
 * 260ms 大约是「手停了、画面也停了、可以开始加载」的时间点。
 */
private const val TvInputIdleMs = 260L

/**
 * 遥控器「正在连续操作」的状态。
 *
 * 用途只有一个：**按键连续触发期间，别再去发起新的封面加载**。
 *
 * 电视端最影响手感的不是动画本身，而是「按键 → 焦点移动 → 新一屏卡片进入视口 →
 * 同一帧里还要解码十几张封面」这条链。帧预算 16.7ms 一旦被解码吃掉，这一格就跳不动，
 * 表现就是按住下键时页面「一停一停」。
 *
 * 所以按键风暴期间先不发新请求，等手停下来再加载。已经加载过的图片不受影响
 * （见 [rememberTvDeferredImage]），所以滚动时看到的是「新卡片先空着，老卡片照常」。
 */
@Stable
class TvInputActivityTracker(private val scope: CoroutineScope) {
    private val busyState = mutableStateOf(false)
    private var idleJob: Job? = null

    /**
     * 是否处于「连续按键」状态。
     *
     * 注意这是普通的 getter 读 snapshot state —— 在 composable 里读它会订阅本节点，
     * 但只在 true/false 翻转时重组，不是每次按键都重组。
     */
    val busy: Boolean get() = busyState.value

    /** 每次方向键按下调用一次；重复调用不会反复触发重组 */
    fun onDirectionKey() {
        if (!busyState.value) busyState.value = true
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(TvInputIdleMs)
            busyState.value = false
        }
    }
}

/**
 * 当前生效的 [TvInputActivityTracker]，没有就是 null（手机、预览）。
 *
 * 这里下发的是**追踪器对象本身**，不是 `busy` 的布尔值 —— 后者会让下发它的那个
 * composable（NeriApp 根节点）在每次滚动开始/结束时整棵树重组。
 * 对象引用是稳定的，用 `staticCompositionLocalOf` 下发后永远不触发失效；
 * `busy` 由真正需要它的卡片自己读，订阅范围就只落在那几张卡上。
 */
val LocalTvInputTracker = staticCompositionLocalOf<TvInputActivityTracker?> { null }

/** 四个方向键。确定/返回这类不算「连续操作」 */
fun Key.isTvDirectionKey(): Boolean =
    this == Key.DirectionUp ||
        this == Key.DirectionDown ||
        this == Key.DirectionLeft ||
        this == Key.DirectionRight
