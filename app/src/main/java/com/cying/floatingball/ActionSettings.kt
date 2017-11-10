package com.cying.floatingball

import android.content.SharedPreferences

/**
 * Created by Cying on 17/10/4.
 */

object ActionSettings : Preferences {
    override val preferences: SharedPreferences by lazy {
        App.instance().getSharedPreferences("action-settings", 0)
    }

    var needVibrate: Boolean by preference("needVibrate")
    var scrollDelay: Int by preference("scrollDelay", 1)
    var autoCloseAd: Boolean by preference("autoCloseAd") { property, newValue ->
        FloatingBallService.instance?.enableAutoCloseAd(newValue)
    }
}

enum class GESTURE(val key: String, val label: String) : Preferences by ActionSettings {


    CLICK("click", "点击") {
        override var action: Int by preference(MockAction.BACK)
    },
    DOUBLE_CLICK("double_click", "双击") {
        override var action: Int by preference(key, MockAction.NONE.action) { _, newValue ->
            FloatingBallService.instance?.enableDoubleClick(newValue != MockAction.NONE.action)
        }
    },
    SWIPE_LEFT("left", "左滑") {
        override var action: Int by preference(MockAction.RECENTS)
    },
    SWIPE_TOP("top", "上滑") {
        override var action: Int by preference(MockAction.HOME)
    },
    SWIPE_RIGHT("right", "右滑") {
        override var action: Int by preference(MockAction.NOTIFICATIONS)
    },
    SWIPE_BOTTOM("bottom", "下滑") {
        override var action: Int by preference(MockAction.LOCK)
    };

    abstract var action: Int

    fun trigger(): Boolean = MockAction.getByAction(action)?.trigger() ?: false

    protected fun preference(defaultAction: MockAction) = preference(key, defaultAction.action)

}



