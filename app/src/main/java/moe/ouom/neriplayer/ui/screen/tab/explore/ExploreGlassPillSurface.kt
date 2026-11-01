package moe.ouom.neriplayer.ui.screen.tab.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.tv.tvFocusRing
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface

private val ExplorePillShape = RoundedCornerShape(999.dp)

@Composable
internal fun ExploreGlassPillSurface(
    fallbackColor: Color,
    tintColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier,
    /** 电视（遥控器）不需要 48dp 最小触控尺寸，关掉后才能把整行压到和搜索框一样高 */
    enforceMinInteractiveSize: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (enforceMinInteractiveSize) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            // 胶囊按钮的焦点环：不传圆角 = 半径取短边一半，环贴着胶囊边框内侧走
            .tvFocusRing(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        AdvancedGlassSurface(
            role = AdvancedGlassRole.ExploreTag,
            shape = ExplorePillShape,
            fallbackColor = fallbackColor,
            tintColor = tintColor
        ) {
            Surface(
                modifier = Modifier
                    .clip(ExplorePillShape)
                    .indication(interactionSource, ripple()),
                shape = ExplorePillShape,
                color = Color.Transparent,
                contentColor = contentColor,
                border = border,
                content = content
            )
        }
    }
}
