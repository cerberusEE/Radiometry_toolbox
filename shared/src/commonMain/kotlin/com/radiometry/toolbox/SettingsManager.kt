package com.radiometry.toolbox

import androidx.compose.runtime.Composable

interface SettingsManager {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class InMemorySettingsManager : SettingsManager {
    private val map = mutableMapOf<String, Any>()

    override fun getString(key: String, defaultValue: String): String {
        return map[key] as? String ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return map[key] as? Int ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        map[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return map[key] as? Boolean ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }
}

@Composable
expect fun rememberSettingsManager(): SettingsManager
