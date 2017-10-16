package com.cying.floatingball

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Vibrator
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager

/**
 * Created by Cying on 17/9/28.
 */
fun Context.getAccessibilityServiceManager() = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager


fun Context.getDevicePolicyManager() = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager


fun Context.getWindowManager() = getSystemService(Context.WINDOW_SERVICE) as WindowManager


fun Context.getVibrator() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator