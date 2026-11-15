package moe.ouom.neriplayer.ui.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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

/** 电视卡片横条 / 网格里卡片两侧的内边距（首页 `PagedCardStrip` 用的就是它）。 */
val TvCardStripHorizontalPadding: Dp = 8.dp

/**
 * 电视：卡片横条 / 网格「一排放几张、每张多宽」的**唯一**算法。
 *
 * 首页的 `PagedCardStrip`（「为你推荐 / 雷达歌单 / 继续播放」那一排）和探索页的网格
 * 都调这一个函数 —— 只有算式完全一致，两处的卡片才是逐像素一样大。
 * 只对齐「目标宽度 172dp」是不够的：格位宽由容器宽摊平，差几个 dp 肉眼就能看出来
 * （探索页原来是 154dp、首页是 151dp，一大圈）。
 *
 * @param availableWidth 容器**内容宽**，调用方自己扣掉左右内边距
 * @param targetCardWidth 期望的单张卡片宽度，只用来反算一排能放几张
 */
data class TvCardStripMetrics(
    /** 一排几张 */
    val perPage: Int,
    /** 格位宽 */
    val slotWidth: Dp,
    /** 真正的卡片宽（格位宽 × 0.9，在格位里居中） */
    val cardWidth: Dp
)

fun tvCardStripMetrics(
    availableWidth: Dp,
    targetCardWidth: Dp = 172.dp,
    spacing: Dp = 12.dp
): TvCardStripMetrics {
    val available = availableWidth.coerceAtLeast(1.dp)
    val perPage = (((available + spacing) / (targetCardWidth + spacing)).toInt()).coerceAtLeast(1)
    val slotWidth = ((available - spacing * (perPage - 1)) / perPage).coerceAtLeast(80.dp)
    return TvCardStripMetrics(
        perPage = perPage,
        slotWidth = slotWidth,
        cardWidth = slotWidth * 0.9f
    )
}

/**
 * 页面最外侧的左右边距：电视用 [tvPageSidePadding]（0），手机平板用 [base]。
 *
 * 所有页面都走这一个函数，保证「歌单页留一条白边、媒体库却贴边」这类不一致不会再出现。
 */
@Composable
fun pageSidePaddingOr(base: Dp): Dp =
    if (LocalIsTelevision.current) tvPageSidePadding() else base

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
