import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Common {
    fun getBuildVersionCode(buildTimestamp: String? = null): Int {
        val buildDate = buildTimestamp?.let { value ->
            val timestamp = value.toLongOrNull()?.takeIf { it > 0L }
                ?: throw IllegalArgumentException("buildTimestamp must be positive epoch milliseconds")
            Date(timestamp)
        } ?: Date()
        val appVerCode: Int by lazy {
            // 必须固定 UTC：CI 的 tag 名用 `date -u +%Y%m%d-%H%M` 生成，
            // AppUpdateRepository 再从 tag 里取 `yyMMddHH` 和 BuildConfig.VERSION_CODE 比大小。
            // 两边时区不一致时（runner 是 UTC、本地是 UTC+8），tag 解析值会和安装包的版本号
            // 差 8 小时，更新检查就会永远提示「有新版本」。
            val sdf = SimpleDateFormat("yyMMddHH", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(buildDate).toInt()
        }
        return appVerCode
    }

    private fun getCurrentDate(project: Project): String {
        val override = project.findProperty("buildVersionTimestamp") as String?
        if (!override.isNullOrBlank()) {
            return override
        }

        val sdf = SimpleDateFormat("MMddHHmm", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Taipei")
        return sdf.format(Date())
    }


    private fun getShortGitRevision(): String {
        val command = "git rev-parse --short HEAD"
        val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return if (exitCode == 0) {
            output.trim()
        } else {
            "no_commit"
        }
    }

    fun getBuildVersionName(project: Project): String {
        return "${getShortGitRevision()}.${getCurrentDate(project)}"
    }
}

