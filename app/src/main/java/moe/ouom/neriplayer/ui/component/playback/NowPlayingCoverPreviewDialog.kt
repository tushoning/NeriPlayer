package moe.ouom.neriplayer.ui.component.playback

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <https://www.gnu.org/licenses/>.
 */

import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.component.overlay.LocalizedDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.component.overlay.ApplyOverlayWindowNavigationBarPolicy
import moe.ouom.neriplayer.ui.haptic.HapticButton
import moe.ouom.neriplayer.ui.haptic.HapticFilledIconButton
import moe.ouom.neriplayer.util.media.offlineCachedImageRequest
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.CompositionLocalProvider
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.rememberTvFocusRingIndication
import moe.ouom.neriplayer.ui.theme.NeriDesign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.sp

private const val CoverPreviewMinRequestSizePx = 512
private const val CoverPreviewMaxRequestSizePx = 2048
private val CoverPreviewArtworkShape = RoundedCornerShape(28.dp)

@Composable
private fun LightDialogSystemBarIcons() {
    val view = LocalView.current
    SideEffect {
        val dialogWindow = view.findDialogWindowProvider()?.window
            ?: return@SideEffect
        WindowInsetsControllerCompat(
            dialogWindow,
            dialogWindow.decorView
        ).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

private fun View.findDialogWindowProvider(): DialogWindowProvider? {
    var current: View? = this
    while (current != null) {
        if (current is DialogWindowProvider) {
            return current
        }
        current = current.parent as? View
    }
    return null
}

@Composable
internal fun NowPlayingCoverPreviewDialog(
    coverUrl: String,
    songName: String,
    offlineMode: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val title = songName.ifBlank { stringResource(CoreCommonR.string.cover_preview_title) }
    var transform by remember(coverUrl) { mutableStateOf(CoverPreviewTransform()) }
    var intrinsicArtworkSize by remember(coverUrl) { mutableStateOf(Size.Unspecified) }
    val backdropModel = remember(context, coverUrl, offlineMode) {
        offlineCachedImageRequest(
            context = context,
            data = coverUrl,
            sizePx = CoverPreviewMinRequestSizePx,
            allowHardware = true,
            offlineMode = offlineMode
        )
    }

    // 简洁风格：封面图直接铺满整屏，不再套上下两条黑色底板。
    // 电视一律走这套版式：另一种主题风格原来是个 720dp 方框 + 上下黑边，
    // 遥控器上看就是「图片缩在中间一小块」，和简洁风格差太多。
    val cleanFullscreen = NeriDesign.isClean || LocalIsTelevision.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ApplyOverlayWindowNavigationBarPolicy()
        LightDialogSystemBarIcons()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
            contentColor = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = backdropModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.30f
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.52f),
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.94f)
                                )
                            )
                        )
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    // 简洁风格：整个屏幕就是画布。
                    // 图片以 ContentScale.Fit 画在这块画布上 —— 按原比例、能放多大放多大，
                    // 比例和屏幕不一致时由背景填满，绝不裁切图片。
                    // 尺寸全部来自 BoxWithConstraints 的实测值，任何分辨率/缩放下都成立。
                    val artworkSize = if (cleanFullscreen) {
                        DpSize(maxWidth, maxHeight)
                    } else {
                        val side = minOf(maxWidth, maxHeight, 720.dp)
                        DpSize(side, side)
                    }
                    val artworkWidthPx = with(density) {
                        artworkSize.width.toPx().coerceAtLeast(1f)
                    }
                    val artworkHeightPx = with(density) {
                        artworkSize.height.toPx().coerceAtLeast(1f)
                    }
                    val requestSizePx = maxOf(artworkWidthPx, artworkHeightPx)
                        .roundToInt()
                        .coerceIn(
                            CoverPreviewMinRequestSizePx,
                            CoverPreviewMaxRequestSizePx
                        )
                    val fittedArtworkSizePx = remember(
                        artworkWidthPx,
                        artworkHeightPx,
                        intrinsicArtworkSize
                    ) {
                        fitCoverPreviewContentSize(
                            viewportWidthPx = artworkWidthPx,
                            viewportHeightPx = artworkHeightPx,
                            intrinsicSizePx = intrinsicArtworkSize
                        )
                    }
                    val imageContentDescription = if (songName.isBlank()) {
                        stringResource(CoreCommonR.string.cover_preview_image_content_description)
                    } else {
                        stringResource(
                            CoreCommonR.string.cover_preview_image_content_description_named,
                            songName
                        )
                    }
                    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
                        transform = updateCoverPreviewTransform(
                            current = transform,
                            zoomChange = zoomChange,
                            panChange = panChange,
                            viewportWidthPx = artworkWidthPx,
                            viewportHeightPx = artworkHeightPx,
                            fittedContentSizePx = fittedArtworkSizePx
                        )
                    }
                    LaunchedEffect(artworkWidthPx, artworkHeightPx, fittedArtworkSizePx) {
                        transform = updateCoverPreviewTransform(
                            current = transform,
                            zoomChange = 1f,
                            panChange = Offset.Zero,
                            viewportWidthPx = artworkWidthPx,
                            viewportHeightPx = artworkHeightPx,
                            fittedContentSizePx = fittedArtworkSizePx
                        )
                    }

                    val artwork: @Composable () -> Unit = {
                        Surface(
                            modifier = Modifier
                                .size(artworkSize)
                                .pointerInput(coverUrl, artworkWidthPx) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            transform = toggleCoverPreviewZoom(transform)
                                        }
                                    )
                                }
                                .transformable(
                                    state = transformableState,
                                    lockRotationOnZoomPan = true
                                ),
                            shape = if (cleanFullscreen) RectangleShape else CoverPreviewArtworkShape,
                            color = if (cleanFullscreen) {
                                Color.Transparent
                            } else {
                                Color.Black.copy(alpha = 0.58f)
                            },
                            border = if (cleanFullscreen) {
                                null
                            } else {
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                            },
                            shadowElevation = if (cleanFullscreen) 0.dp else 28.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.24f)
                                )
                                AsyncImage(
                                    // 图片来源逻辑保持原样（和改全屏之前完全一致）：
                                    // 只给一个正方形边长做解码约束，按原比例缩放到能放下的最大值。
                                    // 之前的方框版本就是靠这一条把 16:9 的图完整装进方框、上下留黑边的。
                                    // 现在只是把外面那个方框换成整屏画布，解码这一段一个字没动。
                                    model = remember(
                                        context,
                                        coverUrl,
                                        requestSizePx,
                                        offlineMode
                                    ) {
                                        offlineCachedImageRequest(
                                            context = context,
                                            data = coverUrl,
                                            sizePx = requestSizePx,
                                            allowHardware = true,
                                            offlineMode = offlineMode
                                        )
                                    },
                                    contentDescription = imageContentDescription,
                                    contentScale = ContentScale.Fit,
                                    onSuccess = { state ->
                                        intrinsicArtworkSize = state.painter.intrinsicSize
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = transform.scale
                                            scaleY = transform.scale
                                            translationX = transform.offset.x
                                            translationY = transform.offset.y
                                        }
                                )
                            }
                        }
                    }

                    if (cleanFullscreen) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            artwork()
                        }
                        // 顶部只留歌名 + 关闭按钮，不再有底板
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                // 和播放页歌名同一个字号（32sp / 行高 42sp），
                                // 底下压着一层黑色阴影，亮色封面上也看得清。
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 32.sp,
                                    lineHeight = 42.sp,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 10f
                                    )
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HapticFilledIconButton(
                                onClick = onDismiss,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(CoreCommonR.string.action_close)
                                )
                            }
                        }
                        // 底部右侧只留两个按钮，左下角那串「100% 双指缩放…」整段去掉
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (transform.scale > CoverPreviewMinScale) {
                                HapticFilledIconButton(
                                    onClick = { transform = CoverPreviewTransform() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.16f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RestartAlt,
                                        contentDescription = stringResource(
                                            CoreCommonR.string.cover_preview_reset_zoom
                                        )
                                    )
                                }
                            }
                            CoverPreviewDownloadButton(onClick = onDownload)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.42f),
                                contentColor = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shadowElevation = 10.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 18.dp,
                                        top = 8.dp,
                                        end = 8.dp,
                                        bottom = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HapticFilledIconButton(
                                        onClick = onDismiss,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.12f),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(CoreCommonR.string.action_close)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                artwork()
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp),
                                shape = RoundedCornerShape(26.dp),
                                color = Color.Black.copy(alpha = 0.44f),
                                contentColor = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shadowElevation = 12.dp
                            ) {
                                CoverPreviewBottomBar(
                                    zoomPercent = (transform.scale * 100f).roundToInt(),
                                    resetEnabled = transform.scale > CoverPreviewMinScale,
                                    onReset = { transform = CoverPreviewTransform() },
                                    onDownload = onDownload
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverPreviewBottomBar(
    zoomPercent: Int,
    resetEnabled: Boolean,
    onReset: () -> Unit,
    onDownload: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showZoomHint = maxWidth >= 390.dp
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.11f),
                contentColor = Color.White
            ) {
                Text(
                    text = stringResource(CoreCommonR.string.cover_preview_zoom_percent, zoomPercent),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (showZoomHint) {
                Text(
                    text = stringResource(CoreCommonR.string.cover_preview_zoom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            HapticFilledIconButton(
                onClick = onReset,
                enabled = resetEnabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.11f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.06f),
                    disabledContentColor = Color.White.copy(alpha = 0.30f)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = stringResource(CoreCommonR.string.cover_preview_reset_zoom)
                )
            }
            CoverPreviewDownloadButton(onClick = onDownload)
        }
    }
}

/**
 * 「下载封面」按钮。
 *
 * 之前用的是 [HapticButton]（M3 Button 一族）：电视上它内部补的环是
 * `tvFocusRing(圆角=未指定)`，也就是按节点短边一半算 —— 在 44dp 高的胶囊按钮上
 * 环直接变成两头全圆的跑道形，跟按钮自己的 18dp 圆角完全对不上。
 * 这里自己画：焦点环从 `LocalIndication` 来，圆角由外面提供的 ring 决定（18dp）。
 */
@Composable
private fun CoverPreviewDownloadButton(onClick: () -> Unit) {
    val ring = rememberTvFocusRingIndication(
        radius = 18.dp,
        cleanRadiusBoost = 0.dp
    )
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .clickable { onClick() }
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(CoreCommonR.string.action_download_cover),
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    if (ring != null) {
        CompositionLocalProvider(LocalIndication provides ring) { content() }
    } else {
        content()
    }
}
