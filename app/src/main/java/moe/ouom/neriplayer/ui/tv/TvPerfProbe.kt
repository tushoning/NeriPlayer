package moe.ouom.neriplayer.ui.tv

import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * 临时性能探针（定位完即删）。
 *
 * 目的：回答「按一下遥控器之后那一秒多，主线程到底在处理哪条消息」。
 *
 * 原理：给主线程 Looper 挂一个 Printer。Android 在派发每条消息前后各回调一次，
 * 我们据此算出这条消息占住主线程多久，以及它是哪个 Handler 的哪个 callback。
 *
 * 判定方式：
 * - 若打出 `android.view.Choreographer | ...FrameDisplayEventReceiver` → 卡在一帧内的工作
 *   （组合 / 测量 / 布局 / 录制绘制），配合 gfxinfo framestats 可再切段；
 * - 若打出别的 Handler → 直接就是那个 Handler 的活，与渲染无关。
 */
object TvPerfProbe {

    private const val TAG = "NERI_PERF"

    /** 只记录明显卡顿的消息，避免刷屏 */
    private const val THRESHOLD_MS = 80L

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true

        var startUptime = 0L
        var label = ""
        var seq = 0L

        Looper.getMainLooper().setMessageLogging { line ->
            if (line.startsWith(">>>>>")) {
                startUptime = SystemClock.uptimeMillis()
                label = parseLabel(line)
            } else if (line.startsWith("<<<<<")) {
                val cost = SystemClock.uptimeMillis() - startUptime
                if (cost >= THRESHOLD_MS) {
                    seq += 1
                    Log.w(TAG, "MAIN_BLOCK #$seq ${cost}ms :: $label")
                }
            }
        }
        Log.w(TAG, "TvPerfProbe installed (threshold=${THRESHOLD_MS}ms)")
    }

    /**
     * 从
     * `>>>>> Dispatching to Handler (android.view.Choreographer) {2f1a3b} android.view.Choreographer$FrameHandler: 0`
     * 里取出 `android.view.Choreographer | android.view.Choreographer$FrameHandler: 0`
     */
    private fun parseLabel(line: String): String {
        val handler = run {
            val open = line.indexOf("Handler (")
            if (open < 0) return@run "?"
            val close = line.indexOf(')', open + 9)
            if (close < 0) "?" else line.substring(open + 9, close)
        }
        val callback = run {
            val i = line.lastIndexOf("} ")
            if (i < 0) "" else line.substring(i + 2)
        }
        return "$handler | $callback"
    }
}
