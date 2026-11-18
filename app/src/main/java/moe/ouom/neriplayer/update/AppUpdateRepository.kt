/*
 * NeriPlayer - update channel.
 *
 * This file is part of NeriPlayer (GPL-3.0-or-later).
 */
package moe.ouom.neriplayer.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新通道：只认我们自己 fork 仓库的 Release。
 *
 * 电视版和手机版共用这一套，唯一差别是 [ASSET_NAME]（两个包挂在同一个 Release 里）。
 *
 * 探测分两步，日常只走第一步：
 * 1. 请求 `/releases/latest`，从 302 的 `Location` 里取 tag（不花 API 配额），
 *    再对自己那个资产发一次 HEAD，确认这个 Release 里确实有我们的包；
 * 2. 资产不存在（比如手动发过只带另一种包的 Release）或者第一步整个走不通，
 *    才退到 Releases API，按资产名在历史 Release 里往前找。
 *
 * 三条链路统一：直连 + gh-proxy + ghfast。下载时先各发一个 1KB 请求测延迟再挑，
 * 失败自动换下一条。
 */
internal object AppUpdateRepository {

    private const val REPO = "tushoning/NeriPlayer"

    /** 电视包资产名，和 CI 出包保持一致 */
    private const val ASSET_NAME = "NeriPlayer-tv-arm32-v7a.apk"

    /** 一条直连 + 两条国内加速，三个入口对所有请求通用 */
    private val BASES = listOf(
        "",
        "https://gh-proxy.com/",
        "https://ghfast.top/"
    )

    data class Latest(val tag: String, val versionCode: Int)

    /**
     * 一次检查的结果。
     *
     * 必须把「接口通了、但仓库里没有我的包」和「网络/接口挂了」分开：
     * 前者是正常状态，界面应该显示「暂无新版本」，不能每次开机都弹「检查更新失败」。
     */
    sealed interface FetchResult {
        data class Found(val latest: Latest) : FetchResult
        data object NotFound : FetchResult
        data object Failed : FetchResult
    }

    suspend fun fetchLatest(): FetchResult = withContext(Dispatchers.IO) {
        // ---------- 第一步：latest 重定向 + 资产 HEAD ----------
        for (base in BASES) {
            val tag = runCatching { resolveLatestTag(base) }.getOrNull() ?: continue
            val code = versionCodeOfTag(tag)
            if (code <= 0) continue
            // 资产在就直接用；不在说明这个 Release 没带我们的包，交给 API 往前找
            if (assetExists(tag)) {
                return@withContext FetchResult.Found(Latest(tag, code))
            }
            break
        }

        // ---------- 第二步：Releases API 兜底 ----------
        for (base in BASES) {
            val scan = runCatching { scanReleases(base + apiUrl()) }
            if (scan.isSuccess) {
                val latest = scan.getOrNull()
                return@withContext if (latest != null) {
                    FetchResult.Found(latest)
                } else {
                    FetchResult.NotFound
                }
            }
        }
        FetchResult.Failed
    }

    private fun apiUrl(): String =
        "https://api.github.com/repos/$REPO/releases?per_page=50"

    /**
     * `/releases/latest` 会 302 到 `/releases/tag/<tag>`，tag 直接从 Location 里取。
     *
     * 注意 gh-proxy 会自己跟到底并返回 HTML，拿不到 Location —— 它在这一步会自然失败，
     * 由后面的入口接手；真正需要它的是下载和 API。
     */
    private fun resolveLatestTag(base: String): String? {
        val url = "${base}https://github.com/$REPO/releases/latest"
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

    /**
     * 这个 Release 里有没有我们那个资产。
     *
     * 只认直连和 ghfast（gh-proxy 不保证支持 HEAD）。探测不出来时按「存在」处理，
     * 免得把一次网络抖动当成「没有包」而白跑一遍 API。
     */
    private fun assetExists(tag: String): Boolean {
        for (base in listOf("", "https://ghfast.top/")) {
            val code = runCatching { headCode(assetUrl(base, tag)) }.getOrNull() ?: continue
            if (code in 200..299) return true
            if (code == 404) return false
        }
        return true
    }

    private fun headCode(url: String): Int {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_CONNECT
            readTimeout = TIMEOUT_READ
            requestMethod = "HEAD"
            instanceFollowRedirects = true
        }
        return try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 拉一页 Release，挑出「附带我们这个资产」的那条（非草稿、非预发布），取 versionCode 最大的。
     *
     * tag 是什么前缀都不影响；只挂另一种包的 Release 会被跳过。
     */
    private fun scanReleases(url: String): Latest? {
        val body = httpGet(url) ?: throw IllegalStateException("release api unavailable")
        val array = JSONArray(body)
        var best: Latest? = null
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optBoolean("draft") || item.optBoolean("prerelease")) continue
            if (!hasOurAsset(item.optJSONArray("assets"))) continue
            val tag = item.optString("tag_name")
            val code = versionCodeOfTag(tag)
            if (code <= 0) continue
            if (best == null || code > best.versionCode) best = Latest(tag, code)
        }
        return best
    }

    private fun hasOurAsset(assets: JSONArray?): Boolean {
        if (assets == null) return false
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name") == ASSET_NAME) return true
        }
        return false
    }

    private fun httpGet(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_CONNECT
            readTimeout = TIMEOUT_READ
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            // GitHub API 不带 UA 会直接 403
            setRequestProperty("User-Agent", "NeriPlayer")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
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
        val ordered = BASES.sortedBy { latencyMillis(assetUrl(it, tag)) }
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

    /** `v-20261119-0345` / `tv-20261119-0345` → `26111903`，与构建脚本的 `yyMMddHH` 对齐。 */
    fun versionCodeOfTag(tag: String): Int {
        val match = TAG_PATTERN.find(tag) ?: return 0
        val (year, month, day, hour) = match.destructured
        return (year.takeLast(2) + month + day + hour).toIntOrNull() ?: 0
    }
}
