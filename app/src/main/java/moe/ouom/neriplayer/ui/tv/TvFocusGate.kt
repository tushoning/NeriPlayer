package moe.ouom.neriplayer.ui.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.invalidateFocusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.ModifierNodeElement

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: moe.ouom.neriplayer.ui.tv/TvFocusGate
 * Created: 2026/10/29
 */

/**
 * 电视：焦点**不能靠左右方向键进入**被这一层包住的控件（典型用法是顶部/页面内的标签栏）。
 *
 * 根因：Compose 的方向搜索是「按层级逐级向上找候选」的几何搜索，它不认容器边界。
 * 内容区最左边的卡片按左键时，本层里没有候选，搜索就往上跑到根节点那一层，
 * 而这一层的候选里就包含标签栏的格子 —— 于是焦点莫名"漂"到标签栏上。
 * （`focusProperties { enter / exit }` 拦不住：那个只在焦点事务里生效，不参与几何搜索。）
 *
 * 这里用 `canFocus = false` 把整层从搜索候选里摘掉：
 *  - 横向按键按下 → 立刻关闸（[TvFocusGate.close]），搜索看不到标签栏，焦点原地不动；
 *  - 纵向按键按下 / 任何按键抬起 → 开闸，按上键依然能进标签栏；
 *  - 焦点就在这一层里时永远开闸，所以在标签栏内部左右移动不受影响。
 *
 * 开关必须是**同步生效**的：如果走 Compose 状态，重新组合要等下一帧，
 * 而焦点搜索就发生在同一次按键里，根本来不及。所以这里直接对节点调
 * [invalidateFocusProperties]，不经过重组。
 */
object TvFocusGate {

    private val nodes = ArrayList<TvFocusGateNode>()

    @Volatile
    private var closed = false

    /** 闸门当前是否关着（关着 = 横向搜索看不到挂了闸的层） */
    val isClosed: Boolean get() = closed

    internal fun register(node: TvFocusGateNode) {
        nodes.add(node)
    }

    internal fun unregister(node: TvFocusGateNode) {
        nodes.remove(node)
    }

    /** 横向移动期间关掉所有闸；已生效时不重复下发 */
    fun close() {
        if (closed) return
        closed = true
        dispatch()
    }

    /** 开闸（纵向移动、按键抬起、程序化 requestFocus 之前都要先开） */
    fun open() {
        if (!closed) return
        closed = false
        dispatch()
    }

    private fun dispatch() {
        // invalidate 有可能反过来改到 nodes，用快照遍历
        val snapshot = nodes.toTypedArray()
        for (node in snapshot) {
            // 节点可能正处于卸载流程（没有 owner），刷新会抛异常，这里一律吞掉
            runCatching { if (node.node.isAttached) with(node) { invalidateFocusProperties() } }
        }
    }
}

/** 挂到整棵界面的根节点上：按方向键时同步开关闸门。 */
@Composable
fun Modifier.tvDirectionKeyGate(): Modifier {
    if (!LocalIsTelevision.current) return this
    return this.onPreviewKeyEvent { event ->
        when (event.type) {
            KeyEventType.KeyDown -> when (event.key) {
                Key.DirectionLeft, Key.DirectionRight -> TvFocusGate.close()
                else -> TvFocusGate.open()
            }
            KeyEventType.KeyUp -> TvFocusGate.open()
            else -> Unit
        }
        // 只开关闸门，不吞按键：焦点该怎么走还怎么走
        false
    }
}

/** 把这一层（以及它里面的所有控件）挂到闸门上。 */
@Composable
fun Modifier.tvFocusGate(): Modifier {
    if (!LocalIsTelevision.current) return this
    // 暂时停用：开了之后出现「移动几下焦点就完全卡死」——
    // 关闸时 canFocus=false 会把当前焦点节点失效掉，焦点被清空后就再也动不了。
    // 先保证能用，闸门逻辑另找更安全的实现方式。
    return this
}

private object TvFocusGateElement : ModifierNodeElement<TvFocusGateNode>() {
    override fun create(): TvFocusGateNode = TvFocusGateNode()
    override fun update(node: TvFocusGateNode) = Unit
    override fun hashCode(): Int = TvFocusGateElement::class.java.hashCode()
    override fun equals(other: Any?): Boolean = other === this
}

internal class TvFocusGateNode : Modifier.Node(), FocusPropertiesModifierNode, FocusEventModifierNode {

    private var containsFocus = false

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = containsFocus || !TvFocusGate.isClosed
    }

    override fun onFocusEvent(focusState: FocusState) {
        val now = focusState.isFocused
        if (now == containsFocus) return
        containsFocus = now
        // 只在「拿到焦点」时刷新。丢焦点发生在节点卸载流程里（退出页面、Activity 销毁、
        // 组合被清除），那时节点可能已经没有 owner，再 invalidate 会直接抛
        // IllegalStateException("This node does not have an owner") 把进程打崩。
        // 而且丢焦点本来也不需要立刻刷新：开/关闸门由下一次按键统一触发。
        if (!now) return
        runCatching { if (node.isAttached) invalidateFocusProperties() }
    }

    override fun onAttach() {
        TvFocusGate.register(this)
    }

    override fun onDetach() {
        TvFocusGate.unregister(this)
    }
}
