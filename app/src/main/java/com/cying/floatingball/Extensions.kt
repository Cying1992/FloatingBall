@file:Suppress("NOTHING_TO_INLINE")

package com.cying.floatingball

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager

/**
 * Created by Cying on 17/9/28.
 */
inline fun Context.getAccessibilityServiceManager(): AccessibilityManager {
    return getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
}


inline fun Context.getDevicePolicyManager(): DevicePolicyManager {
    return getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
}

inline fun Context.getWindowManager():WindowManager{
    return getSystemService(Context.WINDOW_SERVICE) as WindowManager
}