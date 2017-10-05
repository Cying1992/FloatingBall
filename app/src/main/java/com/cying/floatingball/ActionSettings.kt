package com.cying.floatingball

import android.content.SharedPreferences

/**
 * Created by Cying on 17/10/4.
 */
enum class GESTURE(val key: String, val label: String) {
    CLICK("click", "点击"),
    DOUBLE_CLICK("double_click", "双击"),
    SWIPE_LEFT("left", "左滑"),
    SWIPE_TOP("top", "上滑"),
    SWIPE_RIGHT("right", "右滑"),
    SWIPE_BOTTOM("bottom", "下滑");

    fun trigger(): Boolean {
        val action = when (this) {
            CLICK -> ActionSettings.click
            DOUBLE_CLICK -> ActionSettings.doubleClick
            SWIPE_LEFT -> ActionSettings.left
            SWIPE_TOP -> ActionSettings.top
            SWIPE_RIGHT -> ActionSettings.right
            SWIPE_BOTTOM -> ActionSettings.bottom
        }

        return MockAction.getByAction(action)?.trigger() ?: false
    }

    fun getAction(): Int = when (this) {
        CLICK -> ActionSettings.click
        DOUBLE_CLICK -> ActionSettings.doubleClick
        SWIPE_LEFT -> ActionSettings.left
        SWIPE_TOP -> ActionSettings.top
        SWIPE_RIGHT -> ActionSettings.right
        SWIPE_BOTTOM -> ActionSettings.bottom
    }


    fun setAction(action: Int) {
        when (this) {
            CLICK -> ActionSettings.click = action
            DOUBLE_CLICK -> {
                ActionSettings.doubleClick = action
                FloatingBallService.instance?.enableDoubleClick(action != MockAction.NONE.action)
            }
            SWIPE_LEFT -> ActionSettings.left = action
            SWIPE_TOP -> ActionSettings.top = action
            SWIPE_RIGHT -> ActionSettings.right = action
            SWIPE_BOTTOM -> ActionSettings.bottom = action
        }
    }
}

object ActionSettings : Preferences {

    override val preferences: SharedPreferences by lazy {
        App.instance().getSharedPreferences("action-settings", 0)
    }

    var click: Int by preference(GESTURE.CLICK.key, MockAction.BACK.action)
    var left: Int by preference(GESTURE.SWIPE_LEFT.key, MockAction.RECENTS.action)
    var top: Int  by preference(GESTURE.SWIPE_TOP.key, MockAction.HOME.action)
    var right: Int by preference(GESTURE.SWIPE_RIGHT.key, MockAction.NOTIFICATIONS.action)
    var bottom: Int by preference(GESTURE.SWIPE_BOTTOM.key, MockAction.LOCK.action)
    var doubleClick: Int by preference(GESTURE.DOUBLE_CLICK.key, MockAction.NONE.action)
    var needVibrate: Boolean by preference("needVibrate", false)

}
