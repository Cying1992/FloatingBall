package com.cying.floatingball

import android.app.Application
import com.cying.lightorm.DatabaseConfiguration
import com.cying.lightorm.LightORM
import kotlin.properties.Delegates

/**
 * Created by Cying on 17/10/4.
 */
const val DATABASE_NAME = "floatingball"

class App : Application() {

    companion object {
        private var instance: App by Delegates.notNull()
        val adBlockInfos = mutableListOf<AdBlockInfo>()
        fun instance() = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initORM()
    }

    private fun initORM() {
        val config = DatabaseConfiguration(DATABASE_NAME, 1)
        LightORM.init(this, config)
        adBlockInfos.addAll(LightORM.getInstance().where(AdBlockInfo::class.java).findAll())
    }
}
