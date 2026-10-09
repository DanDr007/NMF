package com.example.nmf

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if ((intent.action == Intent.ACTION_BOOT_COMPLETED) || (intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            Log.d(TAG, "BOOT_COMPLETED recibido")

            val wasActive = VpnPreferences.wasVpnActive(context)
            if (wasActive) {
                Log.d(TAG, "Restaurando VPN")
                try {
                    val serviceIntent = Intent(context, MyVpnService::class.java).apply {
                        action = MyVpnService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "VPN restaurada correctamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al restaurar VPN tras reinicio", e)
                }
            } else {
                Log.d(TAG, "La VPN no estaba activa previamente; no se restaurará")
            }
        }
    }
}