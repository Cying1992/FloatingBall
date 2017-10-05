package com.cying.floatingball

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Cying on 17/10/5.
 */

interface Preferences {
    val preferences: SharedPreferences
}

class Preference<T>(val name: String, val default: T) : ReadWriteProperty<Preferences, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Preferences, property: KProperty<*>): T = with(thisRef.preferences) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            else -> throw IllegalArgumentException(
                    "This type can be saved into Preferences")
        }

        res as T
    }

    @SuppressLint("CommitPrefEdits")
    override fun setValue(thisRef: Preferences, property: KProperty<*>, value: T) = with(thisRef.preferences.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can be saved into Preferences")
        }.apply()
    }

}

fun <T : Any> preference(name: String, default: T) = Preference(name, default)