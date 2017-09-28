package com.cying.floatingball

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.support.v4.view.accessibility.AccessibilityManagerCompat
import android.util.Log
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Created by Cying on 17/9/27.
 */

private const val TAG = "FloatingBallService"


private var instance: AccessibilityService? = null


enum class MockAction(private val action: Int) {
    HOME(AccessibilityService.GLOBAL_ACTION_HOME),
    BACK(AccessibilityService.GLOBAL_ACTION_BACK),
    RECENTS(AccessibilityService.GLOBAL_ACTION_RECENTS),
    LOCK(0) {
        override fun trigger(): Boolean {
            val dpm = instance?.getDevicePolicyManager()

            return dpm?.run {
                lockNow()
                return true
            } ?: false
        }
    };

    open fun trigger(): Boolean {
        return instance?.performGlobalAction(action) ?: false
    }
}

class FloatingBallService : AccessibilityService() {

    private var ball: FloatingBallView? = null
    override fun onCreate() {
        Log.i(TAG, "onCreate")
        instance = this
        val wm = getWindowManager()
        val pm = createSmallWindowParams()
        ball = FloatingBallView(this)
        ball?.setBackgroundColor(resources.getColor(R.color.bg))
        ball?.updatePositionCallback = object : UpdatePositionCallback {
            override fun update(x: Float, y: Float) {
                pm.x = x.toInt()
                pm.y = y.toInt()
                wm.updateViewLayout(ball, pm)
            }
        }
        wm.addView(ball, pm)
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind")
        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "onRebind")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        instance = null
        if (ball != null) {
            getWindowManager().removeView(ball)
        }
    }

    override fun onInterrupt() {
        Log.i(TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.i(TAG, "onAccessibilityEvent")
    }

    private fun createSmallWindowParams(): WindowManager.LayoutParams {
        val smallWindowParams = WindowManager.LayoutParams()
        smallWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE
        smallWindowParams.format = PixelFormat.RGBA_8888
        smallWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowParams.gravity = Gravity.LEFT or Gravity.TOP
        smallWindowParams.width = 120
        smallWindowParams.height = 120
        smallWindowParams.x = 480
        smallWindowParams.y = 480
        return smallWindowParams
    }
}


fun isFloatingBallServiceEnabled(context: Context): Boolean {
    if (instance == null) {
        return false
    }
    val list = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(context.getAccessibilityServiceManager(), AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return list?.any { it?.settingsActivityName == MainActivity::class.java.name } ?: false
}
