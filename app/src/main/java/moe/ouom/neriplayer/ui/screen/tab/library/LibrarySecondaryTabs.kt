package moe.ouom.neriplayer.ui.screen.tab.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.ouom.neriplayer.ui.tv.pageSidePaddingOr
import moe.ouom.neriplayer.ui.tv.LocalIsTelevision
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import moe.ouom.neriplayer.ui.tv.tvClampRowEnds
import moe.ouom.neriplayer.ui.tv.tvRowStart
import moe.ouom.neriplayer.ui.tv.tvFocusRing
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.component.common.CleanSegmentedTabs
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface

private val LibrarySecondaryTabShape = RoundedCornerShape(24.dp)

/**
 * 媒体库内的二级分类（歌单/艺人/专辑等）
 *
 * 只用文字，层级低于页面顶部的平台标签；左右边距与搜索框一致
 */
@Composable
internal fun LibrarySecondaryTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (NeriDesign.isClean) {
        CleanSegmentedTabs(
            labels = labels,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            modifier = modifier.padding(vertical = 6.dp)
        )
        return
    }
    AdvancedGlassSurface(
        role = AdvancedGlassRole.ScreenTopTab,
        modifier = modifier
            .fillMaxWidth()
            // 横向边距由外面的列表 contentPadding 提供（媒体库各页是 pageSidePaddingOr(8dp)），
            // 这里再加一层会变成两层叠加 —— 标签行就比下面的内容短一截
            .padding(vertical = 6.dp)
            .clip(LibrarySecondaryTabShape),
        shape = LibrarySecondaryTabShape,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        tintColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            labels.forEachIndexed { index, label ->
                Tab(
                    selected = selectedIndex == index,
                    // 电视：走到这一行两端就停住（否则按左会跳到上面的主标签栏/内容里）；
                    // 焦点框以前是默认指示器（看不清），这里补一个胶囊环
                    modifier = Modifier
                        .tvRowStart(index == 0)
                        .tvClampRowEnds(index, labels.lastIndex)
                        .tvFocusRing(color = MaterialTheme.colorScheme.primary),
                    onClick = { onSelected(index) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text(label, style = MaterialTheme.typography.titleSmall) }
                )
            }
        }
    }
}
