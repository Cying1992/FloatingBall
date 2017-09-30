package com.cying.floatingball

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.support.v4.view.accessibility.AccessibilityManagerCompat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import org.jetbrains.anko.dip
import java.lang.reflect.Field

/**
 * Created by Cying on 17/9/27.
 */

private const val TAG = "FloatingBallService"


private var instance: AccessibilityService? = null


enum class MockAction(private val action: Int) {
    HOME(AccessibilityService.GLOBAL_ACTION_HOME),
    BACK(AccessibilityService.GLOBAL_ACTION_BACK),
    RECENTS(AccessibilityService.GLOBAL_ACTION_RECENTS),
    NOTIFICATIONS(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
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

fun getStatusBarHeight(context: Context): Int {
    var c: Class<*>? = null
    var obj: Any? = null
    var field: Field? = null
    var x = 0
    var statusBarHeight = 0
    try {
        c = Class.forName("com.android.internal.R\$dimen")
        obj = c!!.newInstance()
        field = c.getField("status_bar_height")
        x = Integer.parseInt(field!!.get(obj).toString())
        statusBarHeight = context.resources.getDimensionPixelSize(x)
    } catch (e1: Exception) {
        e1.printStackTrace()
    }

    return statusBarHeight
}

class FloatingBallService : AccessibilityService() {

    private var ball: TrackingBallLayout? = null

    private var STATUS_HEIGHT = 0

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        instance = this
        STATUS_HEIGHT = getStatusBarHeight(this)
        val wm = getWindowManager()
        val pm = createSmallWindowParams(wm)
        ball = LayoutInflater.from(this).inflate(R.layout.tracking_ball, null) as TrackingBallLayout?
        ball?.updatePositionCallback = object : UpdatePositionCallback {
            override fun update(view: View, x: Float, y: Float) {
                pm.x = x.toInt() - view.width / 2
                pm.y = y.toInt() - STATUS_HEIGHT - view.height / 2
                wm.updateViewLayout(ball, pm)
                resources.displayMetrics.widthPixels
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

    private fun createSmallWindowParams(wm: WindowManager): WindowManager.LayoutParams {
        val point = Point()
        wm.defaultDisplay.getSize(point)
        val smallWindowParams = WindowManager.LayoutParams()
        smallWindowParams.type = WindowManager.LayoutParams.TYPE_PHONE
        smallWindowParams.format = PixelFormat.RGBA_8888
        smallWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        smallWindowParams.gravity = Gravity.LEFT or Gravity.TOP
        smallWindowParams.width = dip(48)
        smallWindowParams.height = dip(48)
        smallWindowParams.x = point.x - dip(120)
        smallWindowParams.y = point.y - dip(120)
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
