package com.cying.accessibility

/**
 * Created by Cying on 17/10/17.
 */
interface AccessibilityTrigger {
    var isRunning: Boolean
    fun trigger(): Boolean
}