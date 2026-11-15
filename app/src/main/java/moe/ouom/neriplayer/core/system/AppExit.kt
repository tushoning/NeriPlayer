package moe.ouom.neriplayer.core.system

import android.app.Activity
import android.content.Context
import moe.ouom.neriplayer.core.player.service.PlaybackExit

/**
 * 真正退出应用（不是只关界面）。
 *
 * 只调 [Activity.finish] 的话，界面没了但进程还活着：播放服务在 manifest 里声明为
 * `stopWithTask="false"`，它会继续在前台运行，回到桌面照样能听见歌在放。
 *
 * 所以顺序是：
 *  1. 先让播放链路停干净（暂停 → 撤前台通知 → stopSelf → onDestroy 释放播放器运行时）；
 *  2. 再 finishAffinity() 关掉任务栈里所有 Activity。
 *
 * 服务停掉之后进程就没有任何组件了，系统会按正常流程回收，不需要（也不应该）
 * 自己 killProcess —— 那会在 Room 写一半时切断，留下热 WAL 让下次启动打不开库。
 */
object AppExit {
    fun quit(context: Context) {
        PlaybackExit.stopAllForAppExit(context)
        (context as? Activity)?.finishAffinity()
    }
}
