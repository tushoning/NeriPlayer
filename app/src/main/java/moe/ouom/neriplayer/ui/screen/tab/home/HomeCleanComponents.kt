package moe.ouom.neriplayer.ui.screen.tab.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.component.common.NeriSearchFieldGlass
import moe.ouom.neriplayer.ui.theme.NeriDesign

/** Opens Explore's search from the home header; null where no navigation host is available. */
val LocalHomeOpenSearch = staticCompositionLocalOf<(() -> Unit)?> { null }

/** 底部文字条高度（卡片比例靠它保持和原来一致） */
private val FeaturedLabelHeight = 54.dp
/** 文字下方蒙版的黑度：和最早那一版一致（0.32），只是从渐变改成整条实心 */
private const val FeaturedLabelScrimAlpha = 0.32f

/** Clean-style search entry pinned under the home title. */
@Composable
internal fun HomeSearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    NeriSearchFieldGlass(modifier = modifier.fillMaxWidth().height(46.dp), shape = CircleShape) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = stringResource(CoreCommonR.string.search_keyword),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    }
}

/**
 * 方形封面 + 底部半透明阴影 + 文字，文字压在封面**内部**（不再单独占一条）。
 *
 * 原来文字条是「同一张图模糊 + 黑色遮罩」拼出来的：模糊时坐标系变成那条窄带，
 * 纹理和上面的封面接不上，看起来像卡片被切成两块。现在改成一张图铺满整张卡片，
 * 底部叠一层从透明过渡到半透明的黑，文字落在阴影上，卡片是一个整体。
 */
@Composable
internal fun HomeFeaturedArtwork(
    model: Any?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    /** 封面加载成功的回执：电视用它记「这张已经加载过，滚动期间可以继续显示」 */
    onImageLoaded: () -> Unit = {}
) {
    // 高度必须**显式**算出来：之前靠 Column 里 Spacer(aspectRatio(1f)) 撑，
    // 从别的页面返回时 item 会按尚未落定的宽度测量一次，于是一部分卡片被压成
    // 「54dp 文字条」那么高（探索页出现高高低低的瀑布流）。这里直接用父给的宽度算。
    //
    // 注意这里**不能**用 BoxWithConstraints：它是 SubcomposeLayout 的实现，会为每一张卡片
    // 建一份独立的子组合。这个组件在列表项里使用，成本要乘以可见卡片数，滚动和焦点变化
    // 时都要重新走子组合 → 用一次普通测量（Modifier.layout）拿到同样的高度，效果一致。
    val labelHeightPx = with(LocalDensity.current) { FeaturedLabelHeight.roundToPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val cardWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth
                } else {
                    constraints.minWidth.coerceAtLeast(labelHeightPx)
                }
                val cardHeight = cardWidth + labelHeightPx
                val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))
                layout(cardWidth, cardHeight) { placeable.place(0, 0) }
            }
    ) {
        Box(
            // 圆角**不在这里裁**：外层 PlaylistCard 的 `Modifier.clip(cardShape)` 已经会裁剪
            // 整棵子树的绘制（包括这张封面）。原来这里再裁一次，同一张卡片就叠了两层离屏裁剪。
            //
            // 冷启动实测首屏两帧的 `Record View#draw()` 合计约 1.2 秒（逐节点录制，无子区间），
            // 而一屏只有十来张卡片 —— 每张卡每少一层裁剪/绘制节点，首屏就少十几层。
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2A2A2E))
        ) {
            AsyncImage(
                model = model,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                onSuccess = { onImageLoaded() },
                modifier = Modifier.fillMaxSize()
            )
            // 文字栏：半透明黑条直接作为这一栏的**背景**画（原来是叠加在上面的第二个 Box，
            // 等于每张卡片多一个绘制节点）。背景画在 padding 之前，覆盖范围与原来一致。
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FeaturedLabelHeight)
                    .background(Color.Black.copy(alpha = FeaturedLabelScrimAlpha))
                    .padding(start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
