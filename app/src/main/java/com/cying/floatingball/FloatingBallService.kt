package com.cying.floatingball

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.view.accessibility.AccessibilityManagerCompat
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.cying.lightorm.LightORM
import org.jetbrains.anko.dip
import org.jetbrains.anko.toast
import java.lang.reflect.Field

/**
 * Created by Cying on 17/9/27.
 */

private const val TAG = "FloatingBallService"
private const val HINT_SKIP = "跳过"
private const val HINT_CLOSE = "关闭"

enum class MockAction(val action: Int) {
    NONE(0) {
        override fun trigger() = false
    },
    BACK(AccessibilityService.GLOBAL_ACTION_BACK) {
        override fun trigger(): Boolean {
            if (FloatingBallService.instance?.stopAutoScroll() == true) {
                return true
            }
            return super.trigger()
        }
    },
    HOME(AccessibilityService.GLOBAL_ACTION_HOME),
    RECENTS(AccessibilityService.GLOBAL_ACTION_RECENTS),
    NOTIFICATIONS(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
    LOCK(-1) {
        override fun trigger(): Boolean {
            val dpm = FloatingBallService.instance?.getDevicePolicyManager()

            return dpm?.run {
                lockNow()
                return true
            } ?: false
        }
    },
    SCROLL(-2) {
        override fun trigger() = FloatingBallService.instance?.autoScroll() ?: false
    },

    ADBLOCK(-3) {
        override fun trigger(): Boolean {
            FloatingBallService.instance?.addAdBlockWindowName()
            return true
        }
    };

    open fun trigger(): Boolean {
        return FloatingBallService.instance?.performGlobalAction(action) ?: false
    }

    companion object {
        fun getByAction(action: Int) = MockAction.values().firstOrNull { it.action == action }
        val actionArray = MockAction.values().map { it.action }.toIntArray()
    }

}

private fun getStatusBarHeight(context: Context): Int {
    var c: Class<*>? = null
    var obj: Any? = null
    var field: Field? = null
    var x = 0
    var statusBarHeight = 0
    try {
        c = Class.forName("com.android.internal.R\$dimen")
        obj = c.newInstance()
        field = c.getField("status_bar_height")
        x = Integer.parseInt(field!!.get(obj).toString())
        statusBarHeight = context.resources.getDimensionPixelSize(x)
    } catch (e1: Exception) {
        e1.printStackTrace()
    }

    return statusBarHeight
}

class FloatingBallService : AccessibilityService() {

    companion object {
        var instance: FloatingBallService? = null
            private set
    }

    private var ball: TrackingBallLayout? = null

    private var STATUS_HEIGHT = 0

    private var isAutoScrolling = false
    private var target: AccessibilityNodeInfoCompat? = null

    private val handler = Handler(Looper.getMainLooper())
    private var lastWindowName = ""
    private var lastPackageName = ""
    private val tempRect = Rect()
    private var times = 0
    private val btnClassNames = arrayOf(
            TextView::class.java.canonicalName,
            Button::class.java.canonicalName,
            ImageView::class.java.canonicalName,
            ImageButton::class.java.canonicalName)

    private var currentAdBlock: AdBlockInfo? = null

    private val closeAdRunnable = object : Runnable {
        override fun run() {
            times++

            currentAdBlock?.let { ad ->
                val success = findSkipAdNode(ad)?.use { node ->
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        true
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            performClickGesture(node.x(), node.y())
                            true
                        } else {
                            false
                        }
                    }
                } ?: false

                if (success) {
                    toast("已帮你自动关闭广告")
                } else if (times < 3) {
                    handler.postDelayed(this, 500)
                }
            }

        }
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        instance = this
        STATUS_HEIGHT = getStatusBarHeight(this)
        val wm = getWindowManager()
        val pm = createSmallWindowParams(wm)
        ball = LayoutInflater.from(this).inflate(R.layout.tracking_ball, null) as TrackingBallLayout?
        ball?.updatePositionCallback = { view, x, y ->
            pm.x = x.toInt() - view.width / 2
            pm.y = y.toInt() - STATUS_HEIGHT - view.height / 2
            wm.updateViewLayout(ball, pm)
            resources.displayMetrics.widthPixels
        }
        wm.addView(ball, pm)
    }


    fun enableDoubleClick(enabled: Boolean) {
        ball?.doubleClickEnabled = enabled
    }

    fun enableAutoCloseAd(enabled: Boolean) {
        val info = serviceInfo ?: return
        info.eventTypes = if (enabled) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0
        serviceInfo = info
    }

    private fun findSkipAdNode(): AccessibilityNodeInfoCompat? {
        return getRootInActiveWindowCompat()?.use { root ->

            var btnNode = root.findFirstNodeOrNullByText(HINT_SKIP, HINT_CLOSE) { item ->
                item.isVisibleToUser
            }

            if (btnNode == null) {
                val screenArea = root.area()
                btnNode = root.findFirstMatchedDescendantByDepth { item ->
                    item.isVisibleToUser
                            && item.isClickable
                            && btnClassNames.any { name -> item.className == name }
                            && item.area() / screenArea < 0.1
                }
            }
            return btnNode
        }
    }

    private fun findSkipAdNode(ad: AdBlockInfo): AccessibilityNodeInfoCompat? {
        return getRootInActiveWindowCompat()?.use { root ->
            //val screenArea = root.area()
            val predicate: (AccessibilityNodeInfoCompat) -> Boolean = { node ->
                node.isVisibleToUser
                        && node.isClickable == ad.buttonClickable
                        && node.className == ad.buttonClassName
                        && node.area() == ad.buttonArea

            }

            if (!TextUtils.isEmpty(ad.buttonText)) {
                root.findFirstNodeOrNullByText(ad.buttonText) { predicate(it) }
            } else {
                root.findFirstMatchedDescendantByDepth(predicate)
            }
        }

    }

    fun addAdBlockWindowName() {
        if (!ActionSettings.autoCloseAd) {
            return
        }


        if (TextUtils.isEmpty(lastWindowName) || lastWindowName.startsWith("android.")) {
            return
        }

        findSkipAdNode()?.let { node ->
            AdBlockInfo().let { ad ->
                ad.windowClassName = lastWindowName
                ad.packageName = lastPackageName
                ad.appName = if (!TextUtils.isEmpty(lastPackageName)) {
                    val appInfo = packageManager.getApplicationInfo(lastPackageName, 0)
                    appInfo.loadLabel(packageManager).toString()
                } else ""
                ad.buttonClassName = node.className?.toString() ?: ""
                ad.buttonArea = node.area()
                ad.buttonContentDesc = node.contentDescription?.toString() ?: ""
                ad.buttonText = node.text?.let {
                    when {
                        it.contains(HINT_SKIP) -> HINT_SKIP
                        it.contains(HINT_CLOSE) -> HINT_CLOSE
                        else -> ""
                    }
                } ?: ""
                ad.buttonClickable = node.isClickable

                val oldAd = App.adBlockInfos.firstOrNull { it.windowClassName == lastWindowName }
                if (oldAd != null) {
                    LightORM.getInstance().delete(oldAd)
                }
                LightORM.getInstance().save(ad)
                App.adBlockInfos.add(ad)
                toast("保存成功：" + lastWindowName)
            }
        }

    }

    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected")
        enableAutoCloseAd(ActionSettings.autoCloseAd)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        instance = null
        if (ball != null) {
            getWindowManager().removeView(ball)
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val windowName = event.className
            val currentWindow = windowName.toString()
            if (lastWindowName != currentWindow) {
                handler.removeCallbacks(closeAdRunnable)
            }
            lastWindowName = currentWindow
            lastPackageName = event.packageName.toString()
            if (TextUtils.isEmpty(windowName)) {
                return
            }
            if (windowName.toString().startsWith("android.")) {
                return
            }

            val adBlock = App.adBlockInfos.firstOrNull { it.windowClassName == currentWindow } ?: return

            currentAdBlock = adBlock
            handler.removeCallbacks(closeAdRunnable)
            times = 0
            handler.post(closeAdRunnable)
        }
    }

    private fun clickCloseAdButton(root: AccessibilityNodeInfoCompat, hint: String): Boolean {

        root.findAccessibilityNodeInfosByText(hint).use {
            it.firstOrNull { it != null && it.isVisibleToUser }?.use {
                val clicked = it.isClickable && it.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                if (!clicked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    it.getBoundsInScreen(tempRect)
                    performClickGesture(tempRect.exactCenterX(), tempRect.exactCenterY())
                }
                return true
            }
        }

        return false
    }

    private fun createSmallWindowParams(wm: WindowManager) = WindowManager.LayoutParams().apply {
        val point = Point()
        wm.defaultDisplay.getSize(point)
        type = WindowManager.LayoutParams.TYPE_PHONE
        format = PixelFormat.RGBA_8888
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        gravity = Gravity.LEFT or Gravity.TOP
        width = dip(56)
        height = dip(56)
        x = point.x - dip(120)
        y = point.y - dip(120)
    }

    fun stopAutoScroll(): Boolean {
        if (isAutoScrolling) {
            handler.removeCallbacksAndMessages(null)
            isAutoScrolling = false
            target?.recycle()
            target = null
            //toast("停止自动滚动")
            return true
        }
        return false
    }

    fun autoScroll(): Boolean {
        stopAutoScroll()
        val root = getRootInActiveWindowCompat() ?: return false
        val rect = Rect()
        root.getBoundsInScreen(rect)
        val screenArea = (rect.width() * rect.height()).toFloat()
        val tempRect = Rect()
        root.use {
            target = it.findFirstMatchedDescendantByBreadth({
                val visible = it.isVisibleToUser
                if (!visible) {
                    false
                } else if (WebView::class.java.canonicalName == it.className) {
                    true
                } else {
                    it.getBoundsInParent(tempRect)
                    val area = (tempRect.width() * tempRect.height()).toFloat()
                    area / screenArea >= 0.5
                }
            }) {
                it.isScrollable
                        && "android.support.v4.view.ViewPager" != it.className
                        && it.actionList.any { it.id == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD }
            }

            if (target != null) {
                val runnable = object : Runnable {
                    override fun run() {
                        if (target?.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) == true) {
                            handler.postDelayed(this, ActionSettings.scrollDelay * 1000L)
                        } else {
                            stopAutoScroll()
                        }
                    }
                }
                handler.post(runnable)
                isAutoScrolling = true
                toast("开始自动滚动")
            }

        }
        return false
    }
}

private fun AccessibilityService.getRootInActiveWindowCompat(): AccessibilityNodeInfoCompat? =
        rootInActiveWindow?.let { AccessibilityNodeInfoCompat(it) }

private fun AccessibilityNodeInfoCompat.findFirstMatchedDescendantByBreadth(pickChildPredicate: (AccessibilityNodeInfoCompat) -> Boolean, predicate: (AccessibilityNodeInfoCompat) -> Boolean): AccessibilityNodeInfoCompat? {
    val childCount = childCount
    if (childCount == 0) {
        return null
    }

    val tempList = mutableListOf<AccessibilityNodeInfoCompat>()
    for (i in 0 until childCount) {
        getChild(i)?.run {
            if (pickChildPredicate(this)) {
                if (predicate(this)) {
                    tempList.forEach { it.recycle() }
                    return this
                }
                tempList.add(this)
            } else {
                this.recycle()
            }
        }
    }


    tempList.forEach {

        val target = it.findFirstMatchedDescendantByBreadth(pickChildPredicate, predicate)
        if (target != null) {
            tempList.forEach { it.recycle() }
            return target
        }

    }

    tempList.forEach { it.recycle() }
    return null
}

private fun AccessibilityNodeInfoCompat.findFirstMatchedDescendantByDepth(predicate: (AccessibilityNodeInfoCompat) -> Boolean): AccessibilityNodeInfoCompat? {
    val childCount = childCount
    if (childCount == 0) {
        return null
    }
    for (i in 0 until childCount) {
        getChild(i)?.run {
            if (predicate(this)) {
                return this
            } else {
                val target = this.use {
                    it.findFirstMatchedDescendantByDepth(predicate)
                }
                if (target != null) {
                    return target
                }
            }
        }
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.N)
private fun AccessibilityService.performSwipeGesture(x: Float, y: Float, direction: Int): GestureDescription {
    var targetX = x
    var targetY = y
    when (direction) {
        AccessibilityService.GESTURE_SWIPE_LEFT -> {
            targetX -= 300
        }
        AccessibilityService.GESTURE_SWIPE_UP -> {
            targetY -= 300
        }
        AccessibilityService.GESTURE_SWIPE_RIGHT -> {
            targetX += 300
        }

        AccessibilityService.GESTURE_SWIPE_DOWN -> {
            targetY += 300
        }
        else -> {
            throw IllegalArgumentException("滑动方向不正确")
        }
    }
    return GestureDescription.Builder().apply {
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(targetX, targetY)
        addStroke(GestureDescription.StrokeDescription(path, 0, 50))
    }.build()
}

private fun isFloatingBallServiceEnabled(context: Context): Boolean {
    if (FloatingBallService.instance == null) {
        return false
    }
    val list = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(context.getAccessibilityServiceManager(), AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return list?.any { it?.settingsActivityName == MainActivity::class.java.name } ?: false
}

@RequiresApi(Build.VERSION_CODES.N)
fun AccessibilityService.performClickGesture(x: Float, y: Float) {
    val clickGesture = generateTouchGesture(x, y, ViewConfiguration.getTapTimeout())
    dispatchGesture(clickGesture, null, null)
}

@RequiresApi(Build.VERSION_CODES.N)
private fun generateTouchGesture(x: Float, y: Float, duration: Int): GestureDescription {
    val path = Path()
    path.moveTo(x, y)
    return GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, duration.toLong())).build()
}
