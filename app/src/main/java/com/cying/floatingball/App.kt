package com.cying.floatingball

import android.app.Application
import kotlin.properties.Delegates

/**
 * Created by Cying on 17/10/4.
 */
class App : Application() {

    companion object {
        private var instance: App by Delegates.notNull<App>()
        fun instance() = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
