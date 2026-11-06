package moe.ouom.neriplayer.util.media

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
 * File: moe.ouom.neriplayer.util/ImageUtil
 * Created: 2025/1/20
 */

import android.graphics.Bitmap
import android.content.Context
import android.content.res.Configuration
import coil.size.Precision
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.transform.Transformation
import moe.ouom.neriplayer.data.traffic.isOfflineModeNow
import androidx.compose.ui.graphics.ImageBitmap
import java.util.LinkedHashMap

private const val DEFAULT_LOCAL_IMAGE_REQUEST_SIZE_PX = 512
private const val DEFAULT_IMAGE_CROSSFADE_DURATION_MILLIS = 180

/** 这个尺寸以下按「缩略图」处理（列表封面），不属于页头/背景那种大图 */
private const val TV_THUMBNAIL_MAX_SIZE_PX = 512

private fun Context.isTelevisionUi(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION

/**
 * 电视上列表缩略图不做淡入。
 *
 * 每张封面加载完都会跑一段 180ms 的 crossfade。滚动时几十张图陆续加载，就是几十段动画叠在
 * 一起，每一帧都要重新绘制并合成 —— 实测首页连按 25 次方向键，卡顿率 80%、p99 450ms，
 * 而同一台机器上官方包是 68% / 250ms、帧数还多一倍。
 *
 * 只压制缩略图；页头大图、播放页背景这些仍然保留淡入（那里只有一张图，观感更重要）。
 */
private fun Context.suppressThumbnailCrossfade(sizePx: Int?): Boolean =
    isTelevisionUi() && sizePx != null && sizePx <= TV_THUMBNAIL_MAX_SIZE_PX

/**
 * 电视缩略图是否用 RGB_565（每像素 2 字节，ARGB_8888 是 4 字节）。
 *
 * 位图首次绘制时要上传成纹理，耗时和字节数成正比。滚动时不断有新封面进来，
 * 这部分同步上传都压在渲染线程上。减半像素格式能直接砍掉一半上传带宽，
 * 代价是渐变色可能出现轻微色带。
 *
 * 由 `neri_tv_565` 开关控制（默认关，先量再决定要不要默认开）。
 */
private fun Context.useRgb565Thumbnails(sizePx: Int?): Boolean =
    isTelevisionUi() && sizePx != null && sizePx <= TV_THUMBNAIL_MAX_SIZE_PX && tvRgb565Enabled()

/** `neri_tv_565`：1 = 电视缩略图用 RGB_565，0 = 保持 ARGB_8888（默认 0，先量再决定） */
private fun Context.tvRgb565Enabled(): Boolean = runCatching {
    android.provider.Settings.Global.getInt(contentResolver, "neri_tv_565", 0) != 0
}.getOrDefault(false)
// mini player artwork also supplies the expanded player's first frame
internal const val PLAYBACK_COVER_BITMAP_MAX_DIMENSION_PX = 512

/**
 * 创建支持离线缓存的图片请求
 */
fun offlineCachedImageRequest(
    context: Context,
    data: Any?,
    sizePx: Int? = null,
    allowHardware: Boolean = true,
    crossfade: Boolean = true,
    offlineMode: Boolean = context.isOfflineModeNow(),
    transformations: List<Transformation> = emptyList(),
    cacheKey: String? = null
): ImageRequest {
    val localSource = isLocalImageSource(data)
    val remoteSource = isRemoteImageSource(data)
    val resolvedSizePx = sizePx ?: if (localSource) DEFAULT_LOCAL_IMAGE_REQUEST_SIZE_PX else null
    val resolvedAllowHardware = if (localSource && sizePx == null) false else allowHardware
    val builder = ImageRequest.Builder(context)
        .data(data)
        .allowHardware(resolvedAllowHardware)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(if (offlineMode && remoteSource) CachePolicy.DISABLED else CachePolicy.ENABLED)
    cacheKey?.trim()?.takeIf(String::isNotEmpty)?.let { resolvedCacheKey ->
        builder
            .memoryCacheKey(resolvedCacheKey)
            .diskCacheKey(resolvedCacheKey)
    }
    if (localSource && !resolvedAllowHardware) {
        builder.bitmapConfig(Bitmap.Config.RGB_565)
    } else if (context.useRgb565Thumbnails(resolvedSizePx)) {
        // 电视缩略图：半精度，纹理上传字节数减半（见 useRgb565Thumbnails 注释）
        builder.bitmapConfig(Bitmap.Config.RGB_565)
    }
    if (resolvedSizePx != null) {
        builder
            .size(resolvedSizePx)
            .precision(Precision.INEXACT)
    }
    if (transformations.isNotEmpty()) {
        builder.transformations(transformations)
    }
    if (crossfade) {
        builder.crossfade(DEFAULT_IMAGE_CROSSFADE_DURATION_MILLIS)
    } else {
        builder.crossfade(false)
    }
    return builder.build()
}

fun fastScrollableImageRequest(
    context: Context,
    data: Any?,
    sizePx: Int = 512,
    crossfade: Boolean = true,
    offlineMode: Boolean = context.isOfflineModeNow(),
    cacheKey: String? = null
): ImageRequest {
    val remoteSource = isRemoteImageSource(data)
    val builder = ImageRequest.Builder(context)
        .data(data)
        .size(sizePx)
        .precision(Precision.INEXACT)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(if (offlineMode && remoteSource) CachePolicy.DISABLED else CachePolicy.ENABLED)
    cacheKey?.trim()?.takeIf(String::isNotEmpty)?.let { resolvedCacheKey ->
        builder
            .memoryCacheKey(resolvedCacheKey)
            .diskCacheKey(resolvedCacheKey)
    }
    if (isLocalImageSource(data)) {
        builder
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.RGB_565)
    } else if (context.useRgb565Thumbnails(sizePx)) {
        builder.bitmapConfig(Bitmap.Config.RGB_565)
    }
    if (crossfade) {
        builder.crossfade(DEFAULT_IMAGE_CROSSFADE_DURATION_MILLIS)
    } else {
        builder.crossfade(false)
    }
    return builder.build()
}

fun isRemoteImageSource(data: Any?): Boolean {
    val normalized = data?.toString()?.trim()?.lowercase().orEmpty()
    return normalized.startsWith("http://") || normalized.startsWith("https://")
}

fun isLocalImageSource(data: Any?): Boolean {
    val normalized = data?.toString()?.trim()?.lowercase().orEmpty()
    return normalized.startsWith("content://") ||
        normalized.startsWith("file://") ||
        normalized.startsWith("android.resource://") ||
        normalized.startsWith("/")
}

// retained playback artwork outlives Coil's Drawable, so keep an independent bitmap
internal fun copyBitmapForRetainedDisplay(source: Bitmap): Bitmap? {
    if (source.isRecycled) return null
    return source.copy(Bitmap.Config.ARGB_8888, false)
}

internal data class RetainedPlaybackCoverBitmap(
    val ownerKey: String,
    val coverUrl: String,
    val cacheKey: String?,
    val bitmap: ImageBitmap
)

/**
 * 保留最近成功解码的封面, 让页面重建或快速切歌时可以先显示旧帧
 */
internal object RetainedPlaybackCoverBitmapCache {
    private const val MAX_ENTRIES = 4
    private val entries = LinkedHashMap<String, RetainedPlaybackCoverBitmap>(
        MAX_ENTRIES,
        0.75f,
        true
    )

    fun put(
        ownerKey: String?,
        coverUrl: String?,
        cacheKey: String?,
        bitmap: ImageBitmap
    ) {
        val normalizedOwner = ownerKey?.trim()?.takeIf(String::isNotEmpty) ?: return
        val normalizedCover = coverUrl?.trim()?.takeIf(String::isNotEmpty) ?: return
        val entry = RetainedPlaybackCoverBitmap(
            ownerKey = normalizedOwner,
            coverUrl = normalizedCover,
            cacheKey = cacheKey?.trim()?.takeIf(String::isNotEmpty),
            bitmap = bitmap
        )
        synchronized(entries) {
            entries[cacheEntryKey(normalizedOwner, normalizedCover)] = entry
            while (entries.size > MAX_ENTRIES) {
                val iterator = entries.entries.iterator()
                if (!iterator.hasNext()) break
                iterator.next()
                iterator.remove()
            }
        }
    }

    fun getExact(ownerKey: String?, coverUrl: String?): RetainedPlaybackCoverBitmap? {
        val normalizedOwner = ownerKey?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val normalizedCover = coverUrl?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return synchronized(entries) {
            entries[cacheEntryKey(normalizedOwner, normalizedCover)]
        }
    }

    fun getLatestForOwner(ownerKey: String?): RetainedPlaybackCoverBitmap? {
        val normalizedOwner = ownerKey?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return synchronized(entries) {
            entries.entries
                .toList()
                .asReversed()
                .firstOrNull { it.value.ownerKey == normalizedOwner }
                ?.value
        }
    }

    private fun cacheEntryKey(ownerKey: String, coverUrl: String): String {
        return "$ownerKey\u0000$coverUrl"
    }
}
