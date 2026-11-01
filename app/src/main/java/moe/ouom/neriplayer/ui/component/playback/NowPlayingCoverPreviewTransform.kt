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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal const val CoverPreviewMinScale = 1f
private const val CoverPreviewMaxScale = 5f
private const val CoverPreviewDoubleTapScale = 2f

internal data class CoverPreviewTransform(
    val scale: Float = CoverPreviewMinScale,
    val offset: Offset = Offset.Zero
)

internal fun fitCoverPreviewContentSize(
    viewportSizePx: Float,
    intrinsicSizePx: Size
): Size = fitCoverPreviewContentSize(viewportSizePx, viewportSizePx, intrinsicSizePx)

/**
 * 按图片自身宽高比，缩进 [viewportWidthPx] x [viewportHeightPx] 这个矩形里能放下的最大值。
 *
 * 方形视口（封面预览的「相框」模式）传相同的宽高即可；
 * 电视全屏预览要传真实屏幕宽高，长方形封面才能铺满整屏而不是被方形框限制住。
 */
internal fun fitCoverPreviewContentSize(
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    intrinsicSizePx: Size
): Size {
    val viewportWidth = viewportWidthPx.takeIf { it.isFinite() && it > 0f }
        ?: return Size.Zero
    val viewportHeight = viewportHeightPx.takeIf { it.isFinite() && it > 0f }
        ?: return Size.Zero
    val intrinsicWidth = intrinsicSizePx.width
        .takeIf { it.isFinite() && it > 0f }
        ?: return Size(viewportWidth, viewportHeight)
    val intrinsicHeight = intrinsicSizePx.height
        .takeIf { it.isFinite() && it > 0f }
        ?: return Size(viewportWidth, viewportHeight)
    val fitScale = minOf(
        viewportWidth / intrinsicWidth,
        viewportHeight / intrinsicHeight
    )
    return Size(
        width = intrinsicWidth * fitScale,
        height = intrinsicHeight * fitScale
    )
}

internal fun updateCoverPreviewTransform(
    current: CoverPreviewTransform,
    zoomChange: Float,
    panChange: Offset,
    viewportSizePx: Float,
    fittedContentSizePx: Size
): CoverPreviewTransform = updateCoverPreviewTransform(
    current = current,
    zoomChange = zoomChange,
    panChange = panChange,
    viewportWidthPx = viewportSizePx,
    viewportHeightPx = viewportSizePx,
    fittedContentSizePx = fittedContentSizePx
)

internal fun updateCoverPreviewTransform(
    current: CoverPreviewTransform,
    zoomChange: Float,
    panChange: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    fittedContentSizePx: Size
): CoverPreviewTransform {
    val normalizedZoomChange = zoomChange.takeIf { it.isFinite() && it > 0f } ?: 1f
    val targetScale = (current.scale * normalizedZoomChange)
        .coerceIn(CoverPreviewMinScale, CoverPreviewMaxScale)
    if (targetScale <= CoverPreviewMinScale) {
        return CoverPreviewTransform()
    }

    val viewportWidth = viewportWidthPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val viewportHeight = viewportHeightPx.takeIf { it.isFinite() && it > 0f } ?: 0f
    val contentWidth = fittedContentSizePx.width
        .takeIf { it.isFinite() && it > 0f }
        ?: viewportWidth
    val contentHeight = fittedContentSizePx.height
        .takeIf { it.isFinite() && it > 0f }
        ?: viewportHeight
    val maxOffsetX = ((contentWidth * targetScale - viewportWidth) / 2f)
        .coerceAtLeast(0f)
    val maxOffsetY = ((contentHeight * targetScale - viewportHeight) / 2f)
        .coerceAtLeast(0f)
    val targetOffset = current.offset + panChange
    return CoverPreviewTransform(
        scale = targetScale,
        offset = Offset(
            x = targetOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
            y = targetOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
        )
    )
}

internal fun toggleCoverPreviewZoom(
    current: CoverPreviewTransform
): CoverPreviewTransform {
    return if (current.scale > CoverPreviewMinScale) {
        CoverPreviewTransform()
    } else {
        CoverPreviewTransform(scale = CoverPreviewDoubleTapScale)
    }
}
