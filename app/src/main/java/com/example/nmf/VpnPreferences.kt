package com.example.nmf

import android.content.Context

object VpnPreferences {
    private const val PREFS_NAME = "focus_filter_prefs"
    private const val KEY_WAS_ACTIVE = "was_vpn_active"

    fun setVpnActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAS_ACTIVE, active)
            .apply()
    }

    fun wasVpnActive(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WAS_ACTIVE, false)
    }
}