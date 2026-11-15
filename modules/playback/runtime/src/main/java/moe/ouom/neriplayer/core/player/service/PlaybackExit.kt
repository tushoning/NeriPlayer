package moe.ouom.neriplayer.core.player.service

import android.content.Context
import moe.ouom.neriplayer.core.player.service.lyrics.XiaomiSuperIslandLyricService

/**
 * 退出应用时统一收掉播放相关的前台服务。
 *
 * 这些服务都声明了 `stopWithTask="false"`，界面关掉之后它们仍会留在后台，
 * 播放服务会继续放歌。所以「退出应用」必须先经过这里。
 */
object PlaybackExit {
    fun stopAllForAppExit(context: Context) {
        val appContext = context.applicationContext
        AudioPlayerService.stopForAppExit(appContext)
        // HyperOS 超级岛歌词是另一个前台服务，同样要收掉，否则通知会留在状态栏
        runCatching { XiaomiSuperIslandLyricService.stop(appContext) }
    }
}
