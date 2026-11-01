package moe.ouom.neriplayer.ui.tv


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties

/**
 * 电视：焦点走到这一层的左右边缘后就停住，不再往页面外面跳。
 *
 * Compose 的方向搜索是**全局几何搜索**，不认容器边界：内容区最左边的卡片再按左键，
 * 只要上方标签栏里有哪个标签落在它的左边，就会被当成候选并跳上去
 * （「本地歌单按左跑到最近播放标签」「设置二级页按左跑到设置标签」「首页卡片按左回首页标签」
 * 全是这个原因）。
 *
 * 这里给本层套一个焦点组，把「向左/向右离开本组」指向一个**没有挂到任何节点上**的
 * FocusRequester —— 请求一个不存在的落点会失败，焦点就原地不动。
 * 上下方向不受影响，照常进出顶栏/标签栏。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tvKeepFocusHorizontally(): Modifier {
    if (!LocalIsTelevision.current) return this
    val blocked = remember { FocusRequester() }
    return this
        .focusGroup()
        .focusProperties {
            exit = { direction ->
                if (direction == FocusDirection.Left || direction == FocusDirection.Right) {
                    blocked
                } else {
                    FocusRequester.Default
                }
            }
        }
}

/**
 * 电视：焦点不能靠**左右方向**进入这一层（典型用法是顶部的标签栏）。
 *
 * 内容区最左边的卡片再按左键时，Compose 的几何搜索会挑中上方标签栏里最靠左的那个标签
 * （它在左边、距离也近），于是焦点莫名跑到标签栏上。
 * 这里把「从横向进入」指向一个没挂到任何节点上的 FocusRequester —— 请求失败，焦点不动。
 * 从下方按**上**进入标签栏仍然正常。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tvRejectHorizontalEnter(): Modifier {
    if (!LocalIsTelevision.current) return this
    val blocked = remember { FocusRequester() }
    return this
        .focusGroup()
        .focusProperties {
            enter = { direction ->
                if (direction == FocusDirection.Left || direction == FocusDirection.Right) {
                    blocked
                } else {
                    FocusRequester.Default
                }
            }
        }
}

/**
 * 电视：整页详情不许把焦点漏到「下面那一层」。
 *
 * 从播放页跳进来的歌手页 / 专辑页是叠在前一页之上的整页场景。页面里内容稀疏的时候
 * （标签栏切到专辑、正在加载、列表还没回来），光标从标签栏往下按，本页里找不到候选，
 * Compose 的方向搜索就跑到同级的下层场景里去了 —— 表现就是「光标跑到看不见的地方，
 * 遥控器像失灵了」。
 *
 * 这里把四个方向的离组请求都指向一个**没有挂到任何节点上**的 FocusRequester，
 * 请求失败 = 焦点原地不动。页面内部（顶栏、标签栏、列表行）之间的移动完全不受影响。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tvTrapFocus(): Modifier {
    if (!LocalIsTelevision.current) return this
    val blocked = remember { FocusRequester() }
    return this
        .focusGroup()
        .focusProperties {
            exit = { blocked }
        }
}

/**
 * 电视：焦点沿 [direction] 方向离开这一层时，固定落到 [target]（通常是本页顶栏的返回按钮）。
 * 否则列表第一项再按上会直接穿到下一层（或回到上一个页面），跳过返回按钮。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tvFocusExitTo(
    direction: FocusDirection,
    target: FocusRequester
): Modifier {
    if (!LocalIsTelevision.current) return this
    return this
        .focusGroup()
        .focusProperties {
            exit = { dir -> if (dir == direction) target else FocusRequester.Default }
        }
}
