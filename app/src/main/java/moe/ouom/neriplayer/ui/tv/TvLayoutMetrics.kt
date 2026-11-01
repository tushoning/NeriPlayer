package moe.ouom.neriplayer.ui.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

/**
 * 电视端页面左右安全边距。
 *
 * 手机上页面普遍是 8~18dp 的内边距，电视上这个量级等于「内容顶到屏幕边缘」；
 * 而且用户在设置里调过界面缩放后，窗口的 dp 宽度会变，写死的 dp 边距看着更不对。
 * 所以按窗口宽度取比例（4.5%，夹在 20~72dp），缩放怎么调都能留出一条像样的白边。
 */
@Composable
fun tvPageSidePadding(): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp.dp
    return (widthDp * 0.045f).coerceIn(20.dp, 72.dp)
}

/** 电视端列表的左右内容内边距；非电视返回 0，手机平板保持原样。 */
@Composable
fun tvPageSidePaddingOrZero(): Dp =
    if (LocalIsTelevision.current) tvPageSidePadding() else 0.dp

/**
 * 所有页面统一使用的左右边距，基准 = 首页「雷达歌单」卡片左边缘：
 * 首页外层容器留 [tvPageSidePadding]，里面的横条自己还有 8dp contentPadding。
 * 其它页面没有这 8dp，所以直接补上，保证每个页面的内容左边缘在同一条竖线上。
 */
@Composable
fun tvContentSidePadding(): Dp = tvPageSidePadding() + 8.dp

/**
 * 电视上把「原来那一档页面内边距」换成统一的页面边距；手机平板照旧用 [base]。
 * 这样每个页面最外侧的留白都是同一个数，不会出现「歌单页留一条白边、媒体库却贴边」。
 */
@Composable
fun pageSidePaddingOr(base: Dp): Dp =
    if (LocalIsTelevision.current) tvContentSidePadding() else base

/**
 * 首页等「外面还要套一层 8dp 横条内边距」的容器用这个：只留 [tvPageSidePadding]，
 * 加上里面那 8dp 后正好等于 [tvContentSidePadding]，和其它页面对齐。
 */
@Composable
fun pageSidePaddingOuterOr(base: Dp): Dp =
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
