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

fun <T : Any> preference(name: String, default: T): Preference<T> = Preference(name, default)

//参照理解，t始终是引用类型，即包装类型，所以T也应该始终是引用类型才能让下列等式成立
inline fun <reified T : Any> foo(t: T) {
    assert(T::class.java == t.javaClass)
}

//不能直接比较T::class，因为reified T返回的是包装类型，而Int::class返回的是基本类型，所以比较他们的javaObjectType
inline fun <reified T : Any> preference(name: String): Preference<T> = when (T::class.javaObjectType) {
    Int::class.javaObjectType -> preference(name, 0 as T)
    String::class.javaObjectType -> preference(name, "" as T)
    Long::class.javaObjectType -> preference(name, 0L as T)
    Boolean::class.javaObjectType -> preference(name, false as T)

    Float::class.javaObjectType -> preference(name, 0F as T)
    else -> {
        //Log.i("类型", "${T::class} , ${Boolean::class == T::class}")
        throw IllegalArgumentException("This type is not supported")
    }
}