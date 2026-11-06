package moe.ouom.neriplayer.ui.tv


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 电视：内容区按右键时焦点不许往页面外面跑。
 *
 * 右：指向一个**没有挂到任何节点上**的 FocusRequester —— 请求一个不存在的落点会失败，
 * 焦点就原地不动，避免跑进 `MainTabLayerHost` 里正在淡出的隐藏层。
 *
 * 左：**什么都不做**。左边就是导航侧栏，要让 Compose 的几何搜索自己走过去。
 *
 * 这里刻意不套 `focusGroup()`：套上之后这一层会变成独立的搜索边界，按左时搜索到这一层
 * 就停了，不会继续向上找同层 Row 里的侧栏兄弟节点 —— 表现就是「按左完全没反应」。
 * 早期顶栏在屏幕上方时需要这个边界，导航改到左侧之后它就是纯粹的阻碍。
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun Modifier.tvKeepFocusHorizontally(): Modifier {
    if (!LocalIsTelevision.current) return this
    val blocked = remember { FocusRequester() }
    return this
        .focusProperties {
            exit = { direction ->
                if (direction == FocusDirection.Right) blocked else FocusRequester.Default
            }
        }
}

/**
 * 电视：内容区最左边再按左时，把焦点交给左侧导航栏 —— 通过 [onLeftEdge]。
 *
 * 为什么必须拦按键，而不是靠 `focusProperties { exit }`：
 * 系统默认的方向键处理在这里走不通（焦点节点自身的 `focusable` 调完搜索、找不到
 * 左侧的跨容器候选就结束了，不会去问外层的 `exit`）。而 `onPreviewKeyEvent` 跑在
 * Initial pass，能抢在任何子组件之前主动调 `focusManager.moveFocus`。
 *
 * 先自己走一次左移：
 *  - 走得动 → 消费掉（内容区内部的左右移动完全不受影响，行为与系统默认一致）
 *  - 走不动 → 说明已经在最左，才调 [onLeftEdge]
 *
 * 用 `moveFocus` 的布尔返回值判定，而不是靠出口回调 —— `requestFocus` 失败是静默的，
 * 唯一可靠的信号就是 moveFocus 自己返回的 true/false。
 */
@Composable
fun Modifier.tvLeftEdgeToNavigation(onLeftEdge: () -> Unit): Modifier {
    if (!LocalIsTelevision.current) return this
    val focusManager = LocalFocusManager.current
    val currentOnLeftEdge by rememberUpdatedState(onLeftEdge)
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
            return@onPreviewKeyEvent false
        }
        // 这一项已经声明自己是「行首（第 0 列）」：直接交给侧栏本页标签。
        // 否则几何左搜索会先跳到上方那排更靠左的文字标签（归集里要按 3 下才到得了侧栏）。
        if (TvFocusEdge.isAtRowStart()) {
            TvFocusEdge.clear()
            currentOnLeftEdge()
            return@onPreviewKeyEvent true
        }
        if (focusManager.moveFocus(FocusDirection.Left)) {
            return@onPreviewKeyEvent true
        }
        currentOnLeftEdge()
        true
    }
}

/**
 * 电视：「当前焦点是不是落在某一行的最左边缘」。
 *
 * 只能由 item 自己的 `onFocusChanged` 写入 —— 左键的 preview 由内容区根节点
 * ([tvLeftEdgeToNavigation]) 先执行并消费，item 级的按键监听永远拿不到左键
 * （`tvClampLeft` 就是这么变成死代码的）。
 *
 * 必须是 @Volatile 普通字段，不能包 `mutableStateOf`：根节点在按键回调里读它，
 * 同一快照内读 State 可能拿到旧值（同 [moe.ouom.neriplayer.ui.component.navigation.TvTopBarFocus.focusedTabRoute]
 * 踩过的坑）。
 */
object TvFocusEdge {
    @Volatile
    private var rowStartOwner: Any? = null

    fun setRowStart(owner: Any, atRowStart: Boolean) {
        if (atRowStart) {
            rowStartOwner = owner
        } else if (rowStartOwner === owner) {
            rowStartOwner = null
        }
    }

    fun isAtRowStart(): Boolean = rowStartOwner != null

    fun clear() {
        rowStartOwner = null
    }
}

/**
 * 电视：内容区里「用户按过多少下键」的计数器。
 *
 * 页面切过来之后，有几处逻辑会「逐帧重试 requestFocus」来补偿初始焦点（设置二级页
 * 要把光标送到返回键）。用户一旦自己按键，这些补偿必须立刻停手 —— 否则就会出现
 * 「按左已经把光标送进侧栏，下一帧又被拽回返回键」的闪烁。
 *
 * 判断方式不是「最近 N 毫秒内按过键」（进页面的那一下确定键本身就会命中），
 * 而是「重试开始之后有没有新按键」：调用方记下一次计数值，循环里比对。
 *
 * 写入点是内容区根节点的按键监听（[tvSealVerticalEdges]），内容区里任何一次按键
 * 都会经过它。必须是 @Volatile 普通字段：按键回调与协程要读到同一份最新值。
 */
object TvUserInput {
    @Volatile
    var keyDownTick: Int = 0
        private set

    fun stampKeyDown() {
        keyDownTick++
    }
}

/**
 * 把这个可聚焦项标记为「所在行的第 0 列」。
 *
 * 必须和它的 `clickable` 挂在同一条修饰符链上，`isFocused` 才精确等于「这一项自己有焦点」，
 * 不会把卡片内部的「更多」按钮误判成行首。
 *
 * 非电视、非行首直接返回原 Modifier（不影响手机平板，也不影响行内其它格子）。
 */
@Composable
fun Modifier.tvRowStart(atRowStart: Boolean = true): Modifier {
    if (!LocalIsTelevision.current || !atRowStart) return this
    val owner = remember { Any() }
    // 列表回收这一项时不会再补一次失焦事件，自己清掉，否则会留下陈旧标记：
    // 表现是「站在第二列按左，却直接跳到侧栏」
    DisposableEffect(owner) {
        onDispose { TvFocusEdge.setRowStart(owner, false) }
    }
    return this.onFocusChanged { state -> TvFocusEdge.setRowStart(owner, state.isFocused) }
}

/**
 * 电视：内容区「封顶 / 封底」。
 *
 * 内容区最上面一行按上、最下面一行按下时，方向搜索在组内找不到候选会继续往组外找
 * —— 左侧那条贯穿整屏高度的导航栏永远是几何上最近的一个，于是焦点跳到「首页/归集」
 * 标签上。只挂 `focusGroup()` 挡不住这件事：组只是个搜索边界，不是禁令。
 *
 * 做法照抄 [moe.ouom.neriplayer.ui.tv.TvFocusNav] 里的 `tvScrollPane`：
 * 先用 preview 记下「这一下按的是不是上下键」（`onExit` 回调拿不到方向），
 * 再在出口处把这次焦点变更取消掉，焦点原地不动。
 *
 * 三个部分缺一不可：只挂焦点组时，二维搜索会直接跨组命中侧栏，`onExit` 根本不触发。
 * 这里只记录，**绝不**在里面 moveFocus —— 否则会抢在页面自己的上键处理（如首页
 * 「先把网格滚回顶部」）之前，把那套节奏打乱。
 *
 * ⚠️ 标记必须「用完就清」：`onExit` 拦的是**所有**离开内容区的焦点变更，包括返回键
 * 主动把光标收回本页标签那一次。曾经只在左右键时清零，于是按过上下之后再按返回，
 * 收回标签的请求也被当成「上下键逃逸」取消了 —— 表现就是「返回键回不到本页标签」。
 */
@Composable
fun Modifier.tvSealVerticalEdges(): Modifier {
    if (!LocalIsTelevision.current) return this
    // 0 = 没有正在进行的上下键，-1 = 上，1 = 下
    val lastVertical = remember { mutableIntStateOf(0) }
    return this
        .onPreviewKeyEvent { event ->
            when {
                event.type == KeyEventType.KeyDown -> {
                    // 任何一次按键都算「用户已接管光标」：页面里那些逐帧重试
                    // requestFocus 的补偿逻辑靠这个计数及时收手（见 [TvUserInput]）
                    TvUserInput.stampKeyDown()
                    lastVertical.intValue = when (event.key) {
                        Key.DirectionUp -> -1
                        Key.DirectionDown -> 1
                        // 其它任何键（返回键、左右键、确定键）都清掉上一次的上下标记，
                        // 否则「按过上 → 按返回」会走到下面被误拦
                        else -> 0
                    }
                }
                event.type == KeyEventType.KeyUp -> lastVertical.intValue = 0
            }
            false
        }
        .focusGroup()
        .focusProperties {
            canFocus = false
            onExit = {
                val vertical = lastVertical.intValue
                // 读完立刻清：取消之后可能不再有按键事件来复位，留着就是下一次的误拦
                lastVertical.intValue = 0
                // 水平方向不在这里拦：右由 tvKeepFocusHorizontally 负责，左在 preview 阶段就消费了
                if (vertical != 0) cancelFocusChange()
            }
        }
        .focusTarget()
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
