package com.cying.floatingball

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.graphics.Rect
import android.os.Vibrator
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager

/**
 * Created by Cying on 17/9/28.
 */

private val tempRect = Rect()

fun Context.getAccessibilityServiceManager() = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager


fun Context.getDevicePolicyManager() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager


fun Context.getWindowManager() = getSystemService(Context.WINDOW_SERVICE) as WindowManager


fun Context.getVibrator() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


inline fun <R> AccessibilityNodeInfoCompat.use(block: (AccessibilityNodeInfoCompat) -> R): R {
    val result = block(this)
    this.recycle()
    return result
}

inline fun <R> Collection<AccessibilityNodeInfoCompat?>.use(block: (Collection<AccessibilityNodeInfoCompat?>) -> R): R {
    val result = block(this)
    this.forEach { it?.recycle() }
    return result
}

fun AccessibilityNodeInfoCompat.findFirstNodeOrNullByText(vararg texts: String, predicate: (AccessibilityNodeInfoCompat) -> Boolean): AccessibilityNodeInfoCompat? {
    texts.forEach { text ->
        val list = findAccessibilityNodeInfosByText(text)
        var target: AccessibilityNodeInfoCompat? = null

        list.forEach { item ->
            if (item != null) {
                if (target == null && predicate(item)) {
                    target = item
                } else {
                    item.recycle()
                }
            }
        }

        if (target != null) {
            return target
        }
    }
    return null
}

fun AccessibilityNodeInfoCompat.area(): Float {
    getBoundsInScreen(tempRect)
    return tempRect.width() * tempRect.height().toFloat()
}

fun AccessibilityNodeInfoCompat.x(): Float = tempRect.exactCenterX()

fun AccessibilityNodeInfoCompat.y(): Float = tempRect.exactCenterY()