/*
 * NeriPlayer TV fork - update channel.
 *
 * This file is part of NeriPlayer (GPL-3.0-or-later).
 */
package moe.ouom.neriplayer.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新通道：只认我们自己 fork 仓库的 Release。
 *
 * 版本比较不解析 `versionName`（那是 `<sha7>.<yyMMddHHmm>` 形式，没法比大小），
 * 而是拿 Release tag 里的时间对 `versionCode` —— 构建脚本就是按 `yyMMddHH` 生成的
 * （见 `build-logic/convention/Common.kt`），所以两边天然同序。
 */
internal object AppUpdateRepository {

    private const val REPO = "tushoning/NeriPlayer"
    private const val ASSET_NAME = "NeriPlayer-TV.apk"

    /** 前两条是国内加速镜像，最后一条是原链接；下载时按实测延迟排序后再挑。 */
    private val downloadBases = listOf(
        "https://gh-proxy.com/",
        "https://ghfast.top/",
        ""
    )

    /**
     * 探测版本只能用会把 302 原样抛回来的入口：
     * gh-proxy 会自己跟到底并返回 HTML，拿不到 Location。
     */
    private val redirectBases = listOf("", "https://ghfast.top/")

    data class Latest(val tag: String, val versionCode: Int)

    suspend fun fetchLatest(): Latest? = withContext(Dispatchers.IO) {
        for (base in redirectBases) {
            val tag = runCatching {
                resolveLatestTag("${base}https://github.com/$REPO/releases/latest")
            }.getOrNull()
            if (!tag.isNullOrBlank()) {
                val code = versionCodeOfTag(tag)
                if (code > 0) return@withContext Latest(tag, code)
            }
        }
        null
    }

    /** `/releases/latest` 会 302 到 `/releases/tag/<tag>`，tag 直接从 Location 里取。 */
    private fun resolveLatestTag(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = TIMEOUT_CONNECT
            readTimeout = TIMEOUT_READ
            requestMethod = "GET"
        }
        return try {
            val location = conn.getHeaderField("Location") ?: return null
            location.substringAfterLast("/releases/tag/", "").trim().ifBlank { null }
        } finally {
            conn.disconnect()
        }
    }

    /** `tv-20261107-1730` → `26110717`，与构建脚本的 `yyMMddHH` 对齐。 */
    fun versionCodeOfTag(tag: String): Int {
        val match = TAG_PATTERN.find(tag) ?: return 0
        val (year, month, day, hour) = match.destructured
        return (year.takeLast(2) + month + day + hour).toIntOrNull() ?: 0
    }

    /**
     * 下载到 `cacheDir/app_update/<tag>.apk`。
     *
     * 三条链接先各发一个 1KB 的 Range 请求测延迟，按快慢依次尝试；一条失败自动换下一条。
     * 返回 null 表示全部失败。
     */
    suspend fun download(
        context: Context,
        tag: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        val target = File(dir, "$tag.apk")
        val ordered = downloadBases.sortedBy { latencyMillis(assetUrl(it, tag)) }
        for (base in ordered) {
            val ok = runCatching { fetchToFile(assetUrl(base, tag), target, onProgress) }
                .getOrDefault(false)
            if (ok && target.length() > 0L) return@withContext target
        }
        runCatching { target.delete() }
        null
    }

    private fun assetUrl(base: String, tag: String): String =
        "${base}https://github.com/$REPO/releases/download/$tag/$ASSET_NAME"

    private fun latencyMillis(url: String): Long {
        val startedAt = System.nanoTime()
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_PROBE
                readTimeout = TIMEOUT_PROBE
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-1023")
            }
            try {
                conn.inputStream.use { it.readBytes() }
                (System.nanoTime() - startedAt) / 1_000_000L
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(Long.MAX_VALUE)
    }

    private fun fetchToFile(url: String, target: File, onProgress: (Int) -> Unit): Boolean {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_CONNECT
            readTimeout = TIMEOUT_DOWNLOAD_READ
        }
        return try {
            if (conn.responseCode !in 200..299) return false
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0L) {
                            onProgress(((written * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            onProgress(100)
            true
        } finally {
            conn.disconnect()
        }
    }

    /** 交给系统安装器。文件必须在 FileProvider 暴露的 `app_update/` 目录下。 */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    const val UPDATE_DIR = "app_update"

    private val TAG_PATTERN = Regex("(\\d{4})(\\d{2})(\\d{2})-(\\d{2})(\\d{2})")
    private const val TIMEOUT_PROBE = 4000
    private const val TIMEOUT_CONNECT = 12000
    private const val TIMEOUT_READ = 12000
    private const val TIMEOUT_DOWNLOAD_READ = 30000
    private const val BUFFER_SIZE = 64 * 1024
}
