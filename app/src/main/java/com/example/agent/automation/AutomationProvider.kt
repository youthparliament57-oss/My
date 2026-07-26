package com.example.agent.automation

import android.view.accessibility.AccessibilityNodeInfo

interface AutomationProvider {
    fun tap(x: Int, y: Int): Boolean
    fun type(text: String, nodeId: String? = null): Boolean
    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean
    fun scroll(direction: ScrollDirection): Boolean
    fun getUiTree(): UiTreeSnapshot
    fun takeScreenshot(): Boolean
}

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

data class UiTreeSnapshot(
    val rootNode: NodeInfo,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NodeInfo(
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val viewId: String?,
    val boundsInScreen: String,
    val isClickable: Boolean,
    val children: List<NodeInfo> = emptyList()
)
