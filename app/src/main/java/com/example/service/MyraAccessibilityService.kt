package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent

class MyraAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyraAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor accessibility events for context if needed
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun performScroll(scrollUp: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        return performScrollOnNode(root, scrollUp)
    }

    private fun performScrollOnNode(node: AccessibilityNodeInfo, scrollUp: Boolean): Boolean {
        if (node.isScrollable) {
            val action = if (scrollUp) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            if (node.performAction(action)) {
                node.recycle()
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (performScrollOnNode(child, scrollUp)) {
                node.recycle()
                return true
            }
            child.recycle()
        }
        node.recycle()
        return false
    }

    fun performTapByLabel(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findNodeByText(root, label)
        if (target != null) {
            val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK) || target.performAction(AccessibilityNodeInfo.ACTION_SELECT)
            target.recycle()
            root.recycle()
            return clicked
        }
        root.recycle()
        return false
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        if (nodeText.contains(text, ignoreCase = true) || contentDesc.contains(text, ignoreCase = true)) {
            if (node.isClickable) return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) {
                node.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
