package moe.ouom.neriplayer.ui.screen.tab.settings.page

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import moe.ouom.neriplayer.ui.screen.tab.settings.component.rememberSettingsRingIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import moe.ouom.neriplayer.ui.theme.LocalHasCustomBackground
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.tv.TvFocusIndication
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import moe.ouom.neriplayer.ui.tv.tvPageSidePadding
import moe.ouom.neriplayer.ui.tv.tvBringIntoViewOnFocus
import moe.ouom.neriplayer.ui.tv.tvClampLeft
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.delay
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.ui.navigation.LocalMiniPlayerHeight
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassBackdropRegistrationEnabled
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassController
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.SettingsPageTransitionHost
import moe.ouom.neriplayer.ui.util.currentWindowWidthDp
import moe.ouom.neriplayer.ui.util.shouldAllowCollapsingTopAppBar
import kotlin.time.Duration.Companion.milliseconds

private val MiuixCardShape = RoundedCornerShape(16.dp)
private val MiuixHighlightShape = RoundedCornerShape(18.dp)
private val MiuixSettingsContentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
private val MiuixPageRowPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
private val MiuixSettingsTabletMaxWidth = 920.dp

/**
 * 电视上设置页正文的边距，直接对齐歌单详情页「全部播放」那一行（start 18 / end 22 / 上下 8）。
 */
internal val tvSettingsContentStart: Dp = 18.dp
internal val tvSettingsContentEnd: Dp = 22.dp
internal val tvSettingsContentVertical: Dp = 8.dp

internal const val SETTINGS_SPLIT_NAVIGATION_PANE_TAG = "settings-split-navigation-pane"
internal const val SETTINGS_SPLIT_DETAIL_PANE_TAG = "settings-split-detail-pane"

internal fun settingsPageRowTestTag(page: SettingsPage): String = "settings-page-row-${page.name}"

private val settingsCardShape: Shape
    @Composable get() = if (NeriDesign.isClean) NeriDesign.tokens.groupShape else MiuixCardShape

/** Clean style uses opaque inset-grouped cards; Material keeps the translucent glass tint. */
private val settingsCardFallbackColor: Color
    @Composable get() = if (NeriDesign.isClean) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(
            alpha = if (LocalHasCustomBackground.current) CleanCardOverBackgroundAlpha else 1f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.56f)
    }

private val settingsCardTintColor: Color
    @Composable get() = if (NeriDesign.isClean) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

/** Clean cards drop the glass pass on a plain canvas but keep it over a custom background. */
private val settingsCardGlassEnabled: Boolean
    @Composable get() = !NeriDesign.isClean || LocalHasCustomBackground.current

private const val CleanCardOverBackgroundAlpha = 0.66f

private val settingsListSpacing: Dp
    @Composable get() = if (NeriDesign.isClean) 16.dp else 12.dp

@Composable
private fun settingsHorizontalPadding(isTabletLayout: Boolean): Dp = when {
    isTabletLayout -> 28.dp
    NeriDesign.isClean -> 16.dp
    else -> 18.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsHomeScaffold(
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topAppBarState,
        // follow the finger and stay put; a release snap reads as an unwanted bounce
        snapAnimationSpec = null,
        canScroll = {
            shouldAllowCollapsingTopAppBar(
                canScrollForward = listState.canScrollForward,
                canScrollBackward = listState.canScrollBackward,
                collapsedFraction = topAppBarState.collapsedFraction
            )
        }
    )
    val showExpandedTitleMask = scrollBehavior.state.collapsedFraction < 0.5f
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val horizontalPadding = settingsHorizontalPadding(isTabletLayout)

    // 电视：顶部已经有标签栏，这一层 Scaffold 的 topBar 是空的。topBar 为空时
    // Scaffold 传给内容的约束会把列表一次性铺开（焦点能走出屏幕，列表自己不滚），
    // 所以电视上绕开 Scaffold，直接用一个铺满的 Box 装列表。
    if (LocalIsTelevision.current) {
        // 电视上要居中：这个 Box 原来没有 contentAlignment，列表被限宽后就贴在左边，
        // 调过缩放后看着就是「整页往左偏」。
        // 设置页单独一档边距：页面边距 + 自己那 28dp 内容边距，比别的页面更松一点。
        val sidePadding = tvPageSidePadding()
        val tvContentWidth = (currentWindowWidthDp() - sidePadding * 2)
            .coerceAtMost(MiuixSettingsTabletMaxWidth)
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(tvContentWidth)
                    .fillMaxHeight(),
                state = listState,
                // 正文边距跟歌单详情页「全部播放」那一行一致：start 18 / end 22 / 上下 8。
                // 原来用的是 28dp 的平板档 + 上下 10/18，和上面的返回按钮差一截。
                contentPadding = PaddingValues(
                    start = tvSettingsContentStart,
                    end = tvSettingsContentEnd,
                    top = tvSettingsContentVertical,
                    bottom = tvSettingsContentVertical + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(settingsListSpacing),
                content = content
            )
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        // 底部避让由导航宿主和列表留白负责，滚动视口应延伸到系统导航区
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            // 电视上顶部已有标签栏，设置页不再重复一条大标题
            if (!LocalIsTelevision.current) LargeTopAppBar(
                title = {
                    // large top app bars compose both title slots, but only one is visible
                    val isExpandedTitleSlot = LocalTextStyle.current.fontSize.value > 24f
                    CompositionLocalProvider(
                        LocalAdvancedGlassBackdropRegistrationEnabled provides
                            if (isExpandedTitleSlot) {
                                showExpandedTitleMask
                            } else {
                                !showExpandedTitleMask
                            }
                    ) {
                        title()
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = MiuixSettingsTabletMaxWidth)
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 10.dp,
                    bottom = 18.dp + miniPlayerHeight
                ),
                verticalArrangement = Arrangement.spacedBy(settingsListSpacing),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.settingsHighlightTarget(
    targetId: String,
    highlightTargetId: String?,
    highlightPulse: Int,
    onHighlightFinished: (() -> Unit)? = null
): Modifier {
    val highlighted = targetId == highlightTargetId && highlightPulse > 0
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var highlightActive by remember(targetId) { mutableStateOf(false) }
    var targetSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    // the floating mini player covers the list bottom, so keep the target clear of it
    val bottomClearancePx = with(density) { (LocalMiniPlayerHeight.current + 24.dp).toPx() }
    val topClearancePx = with(density) { 16.dp.toPx() }

    LaunchedEffect(highlighted, highlightPulse) {
        if (!highlighted) {
            highlightActive = false
            return@LaunchedEffect
        }
        repeat(2) {
            bringIntoViewRequester.bringIntoView(
                Rect(
                    left = 0f,
                    top = -topClearancePx,
                    right = targetSize.width.toFloat(),
                    bottom = targetSize.height + bottomClearancePx
                )
            )
            withFrameNanos { }
        }
        delay(80.milliseconds)
        repeat(4) { index ->
            highlightActive = index % 2 == 0
            delay(180.milliseconds)
        }
        highlightActive = false
        onHighlightFinished?.invoke()
    }

    val highlightColor by animateColorAsState(
        targetValue = if (highlightActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        label = "settings_item_highlight"
    )

    return this
        .onSizeChanged { targetSize = it }
        .bringIntoViewRequester(bringIntoViewRequester)
        .clip(MiuixHighlightShape)
        .background(highlightColor)
}

@Composable
internal fun MiuixSettingsSectionIntro(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val clean = NeriDesign.isClean
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = if (clean) 12.dp else 10.dp, end = 16.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (clean) 3.dp else 2.dp)
    ) {
        Text(
            text = title,
            style = if (clean) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = if (clean) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = if (clean) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
            }
        )
    }
}

/**
 * 设置首页各入口的焦点记忆。
 *
 * 设置内部的二级页不是独立路由，返回时整页内容会被替换，Compose 没法自动把光标
 * 还回「刚才点进去的那个入口」，所以这里按入口页记一份请求器，返回时主动送回去。
 */
internal object SettingsEntryFocus {
    private val lock = Any()
    private val requesters = HashMap<SettingsPage, FocusRequester>()

    /** 最近一次获得焦点的入口页，返回设置首页时用它当落点 */
    @Volatile
    var lastFocusedPage: SettingsPage? = null

    fun requesterFor(page: SettingsPage): FocusRequester = synchronized(lock) {
        requesters.getOrPut(page) { FocusRequester() }
    }

    fun request(page: SettingsPage?): Boolean {
        if (page == null) return false
        return runCatching { requesterFor(page).requestFocus() }.getOrDefault(false)
    }
}

@Composable
internal fun MiuixSettingsPageGroupCard(
    pages: List<SettingsPage>,
    onPageClick: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    selectedPage: SettingsPage? = null
) {
    if (pages.isEmpty()) {
        return
    }

    val fallbackColor = if (NeriDesign.isClean) {
        settingsCardFallbackColor
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.58f)
    }
    val cardShape = settingsCardShape

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.SettingsGroup,
            modifier = Modifier.fillMaxWidth(),
            enabled = settingsCardGlassEnabled,
            shape = cardShape,
            fallbackColor = fallbackColor,
            tintColor = settingsCardTintColor
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                pages.forEachIndexed { index, page ->
                    MiuixSettingsPageRow(
                        page = page,
                        selected = selectedPage == page,
                        onClick = { onPageClick(page) }
                    )
                    if (index != pages.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = if (NeriDesign.isClean) 66.dp else 74.dp,
                                end = 16.dp
                            ),
                            thickness = if (NeriDesign.isClean) 0.5.dp else 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = if (NeriDesign.isClean) 0.9f else 0.5f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixSettingsPageRow(
    page: SettingsPage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val clean = NeriDesign.isClean
    val isTelevision = LocalIsTelevision.current
    val entryRequester = remember(page) { SettingsEntryFocus.requesterFor(page) }
    // 一级界面的条目装在一张 16dp 圆角的卡片里：环的圆角跟着卡片走（不是往里缩），
    // 半径会再减 1dp 基础内缩，正好和卡片圆角同心，四个角不会被削。
    // 简洁风格（苹果大圆角）下一级首页的环再放大一点点：16 + 6 = 22dp
    // （+12 那版显得过圆，用户反馈太大）
    val tvRowIndication = rememberSettingsRingIndication(16.dp, cleanExtra = 6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isTelevision) {
                    Modifier
                        .focusRequester(entryRequester)
                        .tvBringIntoViewOnFocus()
                        .onFocusChanged {
                            if (it.isFocused) SettingsEntryFocus.lastFocusedPage = page
                        }
                } else {
                    Modifier
                }
            )
            .heightIn(min = if (clean) 64.dp else 76.dp)
            .testTag(settingsPageRowTestTag(page))
            .semantics {
                this.selected = selected
            }
            .background(
                when {
                    !selected -> Color.Transparent
                    clean -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                }
            )
            // 电视：整行占满宽度，最左边再按左不能穿到页面外的主标签栏
            .tvClampLeft()
            // 一级界面：焦点环两边各往里缩 10dp（上下不动）。
            // 这些行装在一张圆角卡片里，环贴着行边界时四个角会被卡片的圆角削掉；
            // 缩进去之后半径不再被压小（drawTvFocusRing 只减基础内缩），四角保持圆角。
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = tvRowIndication,
                onClick = onClick
            )
            .padding(MiuixPageRowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (clean) 14.dp else 16.dp)
    ) {
        if (clean) {
            SettingsPageGlyphTile(page = page, size = 32.dp)
        } else Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = stringResource(page.titleRes),
                modifier = Modifier.size(22.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
                }
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = stringResource(page.descriptionRes),
                style = if (clean) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    // 电视：顶栏固定不折叠。LargeTopAppBar 的标题在展开态位于底部、随列表滚动才升到返回按钮
    // 那一行，遥控器用户看到的就是「文字慢慢飘上去」，所以用普通 TopAppBar + pinned。
    val isTelevision = LocalIsTelevision.current
    val scrollBehavior = if (isTelevision) {
        TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = topAppBarState,
            // follow the finger and stay put; a release snap reads as an unwanted bounce
            snapAnimationSpec = null,
            canScroll = {
                shouldAllowCollapsingTopAppBar(
                    canScrollForward = listState.canScrollForward,
                    canScrollBackward = listState.canScrollBackward,
                    collapsedFraction = topAppBarState.collapsedFraction
                )
            }
        )
    }
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val horizontalPadding = settingsHorizontalPadding(isTabletLayout)
    // 电视：二级页要和一级页用同一条左边缘（居中内容宽度 + 自身内容边距），
    // 否则返回按钮比一级页的卡片更靠左，看着像「没设置边距」。
    val tvSidePadding = if (isTelevision) tvPageSidePadding() else 0.dp
    val tvContentWidth = if (isTelevision) {
        (currentWindowWidthDp() - tvSidePadding * 2).coerceAtMost(MiuixSettingsTabletMaxWidth)
    } else {
        MiuixSettingsTabletMaxWidth
    }
    // 电视：二级页和歌单详情页同一套边距关系 ——
    // 「返回」那一行只贴页面边距（tvPageSidePadding），正文字段再往里 18/22。
    // 原来顶栏在页面边距之上又叠了一层 28dp，正文只有 10dp，两者对不齐。
    val tvGutter = if (isTelevision) {
        (currentWindowWidthDp() - tvContentWidth) / 2
    } else {
        0.dp
    }
    // 二级页一进来光标默认落在左上角的返回按钮上（标题就是这一页的身份，
    // 换页时 title 变，等于「又进了一个二级页」）。页面刚切过来时焦点节点还没挂好，
    // 单次 requestFocus 会失败，所以逐帧试几帧。
    val backFocusRequester = remember { FocusRequester() }
    val settingsScrollScope = rememberCoroutineScope()
    LaunchedEffect(title, showBackButton, isTelevision) {
        if (!isTelevision || !showBackButton) return@LaunchedEffect
        repeat(6) {
            if (runCatching { backFocusRequester.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
            withFrameNanos { }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            if (isTelevision) {
                TopAppBar(
                    modifier = Modifier.padding(horizontal = tvGutter),
                    title = {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = if (isTelevision) {
                                    Modifier
                                        .focusRequester(backFocusRequester)
                                        // 电视：光标从正文走到返回键时，正文如果还停在中间，
                                        // 顺手把它带回顶部 —— 页面顶部那段说明文字是纯文字、
                                        // 拿不到焦点，不这样处理就永远滚不回来了。
                                        .onFocusChanged { focus ->
                                            if (focus.isFocused && listState.canScrollBackward) {
                                                // 瞬时到位：动画还没跑完就按下键的话，
                                                // 焦点项的 bringIntoView 会再抢一次滚动，页面会抖一下。
                                                settingsScrollScope.launch {
                                                    runCatching { listState.scrollToItem(0) }
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(CoreCommonR.string.action_back)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            } else LargeTopAppBar(
                modifier = Modifier.padding(horizontal = tvGutter),
                title = { Text(title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(CoreCommonR.string.action_back)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    // 电视：和一级页同宽（居中 + 页面边距），正文边距才有参照
                    .then(
                        if (isTelevision) Modifier.width(tvContentWidth)
                        else Modifier.widthIn(max = MiuixSettingsTabletMaxWidth)
                    )
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = if (isTelevision) tvSettingsContentStart else horizontalPadding,
                    end = if (isTelevision) tvSettingsContentEnd else horizontalPadding,
                    top = if (isTelevision) tvSettingsContentVertical else 10.dp,
                    bottom = if (isTelevision) {
                        tvSettingsContentVertical + miniPlayerHeight
                    } else {
                        18.dp + miniPlayerHeight
                    }
                ),
                verticalArrangement = Arrangement.spacedBy(settingsListSpacing),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MiuixSettingsResponsiveDetailScaffold(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    topAppBarState: TopAppBarState,
    splitLayout: Boolean,
    modifier: Modifier = Modifier,
    showSplitDetailBackButton: Boolean = false,
    selectedPage: SettingsPage? = null,
    homeListState: LazyListState,
    homeTopAppBarState: TopAppBarState,
    homeTitle: @Composable () -> Unit,
    homeContent: LazyListScope.() -> Unit,
    detailContent: (LazyListScope.(SettingsPage) -> Unit)? = null,
    detailListStates: Map<SettingsPage, LazyListState>? = null,
    detailTopAppBarStates: Map<SettingsPage, TopAppBarState>? = null,
    content: LazyListScope.() -> Unit
) {
    if (!splitLayout) {
        MiuixSettingsDetailScaffold(
            title = title,
            onBack = onBack,
            listState = listState,
            topAppBarState = topAppBarState,
            modifier = modifier,
            content = content
        )
        return
    }
    val isolateAdvancedGlassTransitions = LocalAdvancedGlassController.current.isEnabled

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .testTag(SETTINGS_SPLIT_NAVIGATION_PANE_TAG)
        ) {
            MiuixSettingsHomeScaffold(
                listState = homeListState,
                topAppBarState = homeTopAppBarState,
                title = homeTitle,
                content = homeContent
            )
        }
        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .testTag(SETTINGS_SPLIT_DETAIL_PANE_TAG)
        ) {
            if (selectedPage == null) {
                MiuixSettingsDetailScaffold(
                    title = title,
                    onBack = onBack,
                    listState = listState,
                    topAppBarState = topAppBarState,
                    showBackButton = false,
                    content = content
                )
            } else {
                SettingsPageTransitionHost(
                    activePage = selectedPage,
                    isolateAdvancedGlassTransitions = isolateAdvancedGlassTransitions,
                    backEnabled = showSplitDetailBackButton,
                    onBack = onBack
                ) { page ->
                    if (page != null) {
                        MiuixSettingsDetailScaffold(
                            title = stringResource(page.titleRes),
                            onBack = onBack,
                            listState = detailListStates?.getValue(page) ?: listState,
                            topAppBarState = detailTopAppBarStates?.getValue(page) ?: topAppBarState,
                            showBackButton = if (page == selectedPage) {
                                showSplitDetailBackButton
                            } else {
                                page.backTargetPage() != null
                            }
                        ) {
                            if (detailContent == null) {
                                content()
                            } else {
                                detailContent(page)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MiuixSettingsHeader(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    page: SettingsPage? = null
) {
    val fallbackColor = if (NeriDesign.isClean) {
        settingsCardFallbackColor
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
    }
    val cardShape = settingsCardShape

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.SettingsHeader,
            modifier = Modifier.fillMaxWidth(),
            enabled = settingsCardGlassEnabled,
            shape = cardShape,
            fallbackColor = fallbackColor,
            tintColor = if (NeriDesign.isClean) settingsCardTintColor else MaterialTheme.colorScheme.primaryContainer
        ) {
            if (NeriDesign.isClean && page != null) {
                CleanSettingsHeroContent(page = page, title = title, description = description)
                return@AdvancedGlassSurface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Page hero in the system-settings style: the section glyph leads, copy sits centred below. */
@Composable
private fun CleanSettingsHeroContent(page: SettingsPage, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsPageGlyphTile(page = page, size = 52.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiuixSettingsSectionCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val fallbackColor = settingsCardFallbackColor
    val cardShape = settingsCardShape
    var highlightActive by remember { mutableStateOf(false) }

    LaunchedEffect(highlighted, highlightPulse) {
        if (!highlighted || highlightPulse <= 0) {
            highlightActive = false
            return@LaunchedEffect
        }
        repeat(4) { index ->
            highlightActive = index % 2 == 0
            delay(180.milliseconds)
        }
        highlightActive = false
        onHighlightFinished?.invoke()
    }

    val highlightColor by animateColorAsState(
        targetValue = if (highlightActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            Color.Transparent
        },
        label = "settings_section_highlight"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.SettingsSection,
            modifier = Modifier.fillMaxWidth(),
            enabled = settingsCardGlassEnabled,
            shape = cardShape,
            fallbackColor = fallbackColor,
            tintColor = settingsCardTintColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(highlightColor)
                    .padding(MiuixSettingsContentPadding)
            ) {
                content()
            }
        }
    }
}

internal fun LazyListScope.miuixSettingsSectionCardItem(
    key: Any,
    highlighted: Boolean = false,
    highlightPulse: Int = 0,
    onHighlightFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    item(key = key) {
        MiuixSettingsSectionCard(
            modifier = Modifier.animateItem(),
            highlighted = highlighted,
            highlightPulse = highlightPulse,
            onHighlightFinished = onHighlightFinished,
            content = content
        )
    }
}

