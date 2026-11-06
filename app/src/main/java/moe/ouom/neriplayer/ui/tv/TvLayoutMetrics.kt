package moe.ouom.neriplayer.ui.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

/**
 * 电视端页面的左右内边距，恒为 0。
 *
 * 导航搬到左侧竖栏之后，左右留白由导航外壳统一负责：左边是
 * [moe.ouom.neriplayer.ui.component.navigation.TvSidebarWidth] 的侧栏，
 * 右边由 `AppAdaptiveNavigationContent` 补同样宽度的留白，内容自然居中对称。
 * 页面再自己加一层就成了双重边距，所以这里恒为 0；手机平板各页面自己传 base。
 */
@Composable
fun tvPageSidePadding(): Dp = 0.dp

/**
 * 页面最外侧的左右边距：电视用 [tvPageSidePadding]（0），手机平板用 [base]。
 *
 * 所有页面都走这一个函数，保证「歌单页留一条白边、媒体库却贴边」这类不一致不会再出现。
 */
@Composable
fun pageSidePaddingOr(base: Dp): Dp =
    if (LocalIsTelevision.current) tvPageSidePadding() else base

/**
 * 电视：按「一张卡片想要多宽」反算网格列数，配合 `GridCells.Fixed` 用。
 *
 * 网格原来用 `GridCells.Adaptive(minSize)`，在电视这种大窗口上算出来的单元格会被撑得很大
 * （比如 960dp 宽、minSize 170dp 时会给出 4 列各 200dp，比首页雷达卡片大一圈）。
 * 这里直接按目标卡片宽度（默认取首页雷达歌单那一档 172dp）算列数，
 * 卡片大小就和首页一致了。
 */
fun tvGridColumnsForCardWidth(
    availableWidth: Dp,
    cardWidth: Dp = 172.dp,
    spacing: Dp = 12.dp
): Int {
    if (availableWidth <= 0.dp) return 1
    val unit = (cardWidth + spacing).value
    if (unit <= 0f) return 1
    return ((availableWidth.value + spacing.value) / unit).roundToInt().coerceAtLeast(1)
}

/**
 * 电视上「第二级标签 / 副标题」这一类按钮的统一尺寸。
 *
 * 探索页的来源标签（网易云音乐 / 哔哩哔哩…）和媒体库的（最近播放 / 本地 / 收藏…）
 * 是同一类东西，必须一样大：原来媒体库那几颗用的是 22sp + 上下各 10dp 内边距，
 * 比探索页的标签整整高出一头，摆在一起明显是两种控件。
 */
val TvChipHeight: Dp = 30.dp
val TvChipHorizontalPadding: Dp = 14.dp

@Composable
fun tvChipLabelStyle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
    // titleMedium 是 16sp，标题式标签在电视上稍大一点更清楚；再大就把 30dp 的行撑爆了
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium
)
