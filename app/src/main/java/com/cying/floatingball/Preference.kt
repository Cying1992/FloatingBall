package com.cying.floatingball

import android.annotation.SuppressLint
import android.content.SharedPreferences
import java.math.BigDecimal
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by Cying on 17/10/5.
 */

interface Preferences {
    val preferences: SharedPreferences
}

open class Preference<T>(val name: String, val default: T) : ReadWriteProperty<Preferences, T> {

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Preferences, property: KProperty<*>): T = with(thisRef.preferences) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            is BigDecimal -> BigDecimal(getString(name, default.toString()))
            else -> throw IllegalArgumentException(
                    "This type can be saved into Preferences")
        }

        res as T
    }

    @SuppressLint("CommitPrefEdits")
    override fun setValue(thisRef: Preferences, property: KProperty<*>, value: T) {
        with(thisRef.preferences.edit()) {
            when (value) {
                is Long -> putLong(name, value)
                is String -> putString(name, value)
                is Int -> putInt(name, value)
                is Boolean -> putBoolean(name, value)
                is Float -> putFloat(name, value)
                is BigDecimal -> putString(name, value.toString())
                else -> throw IllegalArgumentException("This type can be saved into Preferences")
            }.apply()
        }
        afterChange(property, value)
    }

    protected open fun afterChange(property: KProperty<*>, newValue: T): Unit {}

    companion object {
        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        fun <T : Any> getDefaultValueByType(clazz: KClass<T>): T = when (clazz.javaObjectType) {
            Int::class.javaObjectType -> 0
            String::class.javaObjectType -> ""
            Long::class.javaObjectType -> 0L
            Boolean::class.javaObjectType -> false
            Float::class.javaObjectType -> 0F
            BigDecimal::class.javaObjectType -> BigDecimal.ZERO
            else -> {
                throw IllegalArgumentException("This type is not supported")
            }
        } as T
    }
}


inline fun <reified T : Any> preference(name: String, default: T = Preference.getDefaultValueByType(T::class), crossinline onChange: (property: KProperty<*>, newValue: T) -> Unit)
        : Preference<T> = object : Preference<T>(name, default) {
    override fun afterChange(property: KProperty<*>, newValue: T) = onChange(property, newValue)
}

fun <T> preference(name: String, default: T) = Preference(name, default)

/*参照理解，t始终是引用类型，即包装类型，所以T也应该始终是引用类型才能让下列等式成立
inline fun <reified T : Any> foo(t: T) {
    assert(T::class.java == t.javaClass)

不能直接比较T::class，因为reified T返回的是包装类型，而Int::class返回的是基本类型，所以比较他们的javaObjectType
*/
/*
inline fun <reified T : Any> preference(name: String, crossinline onChange: (property: KProperty<*>, newValue: T) -> Unit)
        : Preference<T> = preference(name, Preference.getDefaultValueByType(T::class), onChange)

*/

inline fun <reified T : Any> preference(name: String)
        : Preference<T> = Preference(name, Preference.getDefaultValueByType(T::class))

