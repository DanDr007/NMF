package com.example.nmf

import android.content.Context

/**
 * Gestor de preferencias persistentes en SharedPreferences para Focus Filter.
 * Almacena el estado previo de la VPN y el progreso de configuración de Always-On VPN.
 */
object VpnPreferences {
    private const val PREFS_NAME = "focus_filter_prefs"
    private const val KEY_WAS_ACTIVE = "was_vpn_active"
    private const val KEY_ALWAYS_ON_COMPLETED = "always_on_prompt_completed"
    private const val KEY_FIRST_VPN_START_COMPLETED = "first_vpn_start_completed"

    /** Guardar si la VPN estaba activa antes del último cierre/apagado */
    fun setVpnActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAS_ACTIVE, active)
            .apply()
    }

    /** Consultar si la VPN estaba activa */
    fun wasVpnActive(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WAS_ACTIVE, false)
    }

    /** Guardar si el usuario confirmó haber activado Always-On VPN */
    fun setAlwaysOnPromptCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ALWAYS_ON_COMPLETED, completed)
            .apply()
    }

    /** Consultar si la guía Always-On VPN fue completada */
    fun isAlwaysOnPromptCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALWAYS_ON_COMPLETED, false)
    }

    /** Guardar si ya se realizó la primera activación de la VPN */
    fun setFirstVpnStartCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_VPN_START_COMPLETED, completed)
            .apply()
    }

    /** Consultar si ya se ejecutó la primera activación de la VPN */
    fun isFirstVpnStartCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_VPN_START_COMPLETED, false)
    }
}