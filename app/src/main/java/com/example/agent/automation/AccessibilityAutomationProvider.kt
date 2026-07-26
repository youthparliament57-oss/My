package com.example.agent.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityAutomationProvider @Inject constructor(
    private val registry: AccessibilityServiceRegistry
) : AutomationProvider {

    override fun tap(x: Int, y: Int): Boolean {
        val service = registry.getService() ?: return false
        val root = service.rootInActiveWindow ?: return false
        val node = findNodeAt(root, x, y)
        val result = node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        node?.recycle()
        root.recycle()
        return result
    }

    override fun type(text: String, nodeId: String?): Boolean {
        val service = registry.getService() ?: return false
        val root = service.rootInActiveWindow ?: return false
        val node = if (nodeId != null) findNodeById(root, nodeId) else root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        
        var result = false
        node?.let {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            result = it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            it.recycle()
        }
        root.recycle()
        return result
    }

    override fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        val service = registry.getService() ?: return false
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return service.dispatchGesture(gesture, null, null)
    }

    override fun scroll(direction: ScrollDirection): Boolean {
        val service = registry.getService() ?: return false
        val root = service.rootInActiveWindow ?: return false
        val action = when (direction) {
            ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> {
                root.recycle()
                return false
            }
        }
        val res = root.performAction(action)
        root.recycle()
        return res
    }

    
    override fun takeScreenshot(): Boolean {
        val service = registry.getService() ?: return false
        // Android 11+ allows taking screenshots via accessibility
        var result = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            service.takeScreenshot(android.view.Display.DEFAULT_DISPLAY, java.util.concurrent.Executors.newSingleThreadExecutor(), object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) { result = true }
                override fun onFailure(errorCode: Int) { result = false }
            })
        }
        return result
    }

    override fun getUiTree(): UiTreeSnapshot {
        val service = registry.getService() ?: throw IllegalStateException("Accessibility service not enabled")
        val root = service.rootInActiveWindow ?: throw IllegalStateException("No active window")
        
        val snapshot = UiTreeSnapshot(
            rootNode = mapNode(root),
            packageName = root.packageName?.toString() ?: "unknown"
        )
        root.recycle()
        return snapshot
    }

    private fun mapNode(node: AccessibilityNodeInfo): NodeInfo {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val children = mutableListOf<NodeInfo>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                children.add(mapNode(child))
            } finally {
                child.recycle()
            }
        }
        return NodeInfo(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            viewId = node.viewIdResourceName,
            boundsInScreen = "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]",
            isClickable = node.isClickable || node.isCheckable || node.isFocusable,
            children = children
        )
    }

    private fun findNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.contains(x, y)) return null

        var bestNode: AccessibilityNodeInfo? = null

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeAt(child, x, y)
            if (found != null) {
                if (bestNode != null) {
                    if (found.isClickable && !bestNode.isClickable) {
                        bestNode.recycle()
                        bestNode = found
                    } else {
                        found.recycle()
                    }
                } else {
                    bestNode = found
                }
            }
            if (child != bestNode) {
                child.recycle()
            }
        }
        
        return bestNode ?: AccessibilityNodeInfo.obtain(node)
    }

    private fun findNodeById(node: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == id) return AccessibilityNodeInfo.obtain(node)
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeById(child, id)
            child.recycle()
            if (found != null) return found
        }
        return null
    }
}
