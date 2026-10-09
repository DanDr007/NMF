package com.example.nmf

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nmf.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object VpnState {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }
}

class MyVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.nmf.START_VPN"
        const val ACTION_STOP = "com.example.nmf.STOP_VPN"
        private const val CHANNEL_ID = "focus_filter_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "MyVpnService"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private var dbJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var activeBlockedDomains = setOf<String>()

    override fun onCreate() {
        super.onCreate()
        observeBlockedDomains()
    }

    private fun observeBlockedDomains() {
        dbJob?.cancel()
        val dao = AppDatabase.getDatabase(applicationContext).blockedSiteDao()
        dbJob = serviceScope.launch {
            dao.getAllSites().collect { sites ->
                activeBlockedDomains = sites.map { it.domain.lowercase().trim('.') }.toSet()
                Log.d(TAG, "Lista de dominios en la VPN actualizada: ${activeBlockedDomains.size} dominios")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (VpnState.isRunning.value) return

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        try {
            // Configurar VpnService de captura DNS selectiva.
            // Al rutear únicamente las direcciones IP de servidores DNS (10.0.0.1, 8.8.8.8, 8.8.4.4, 1.1.1.1, 1.0.0.1)
            // en lugar de 0.0.0.0/0, todo el tráfico TCP (HTTPS, YouTube, Google) y UDP no DNS
            // fluye directamente a través de la interfaz física de red (Wi-Fi/Celular) sin perder conectividad.
            val builder = Builder()
                .setSession("Focus Filter")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.1")
                .addDnsServer("8.8.8.8")
                .addRoute("10.0.0.1", 32)
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                VpnState.setRunning(true)
                VpnPreferences.setVpnActive(applicationContext, true)
                startPacketProcessing()
                Log.d(TAG, "VPN DNS iniciada con éxito. Tráfico no-DNS fluye normalmente por la interfaz física.")
            } else {
                Log.e(TAG, "No se pudo establecer la interfaz VPN")
                stopVpn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar VPN", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar la interfaz VPN", e)
        }
        vpnInterface = null

        VpnState.setRunning(false)
        VpnPreferences.setVpnActive(applicationContext, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "VPN detenida")
    }

    private fun startPacketProcessing() {
        val pfd = vpnInterface ?: return

        vpnJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)

            while (VpnState.isRunning.value) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        processPacket(buffer, length, outputStream)
                    }
                } catch (e: Exception) {
                    if (!VpnState.isRunning.value) break
                    Log.e(TAG, "Error procesando paquete: ${e.message}")
                }
            }
        }
    }

    private fun processPacket(buffer: ByteArray, length: Int, outputStream: FileOutputStream) {
        if (length < 20) return

        val versionAndIhl = buffer[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return // Solo IPv4

        val ihl = (versionAndIhl and 0x0F) * 4
        if (length < ihl) return

        val protocol = buffer[9].toInt() and 0xFF

        val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
        val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"

        when (protocol) {
            6 -> { // Protocolo TCP
                if (length >= ihl + 4) {
                    val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
                    val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)
                    Log.d(TAG, "[TCP Recibido] $srcIp:$srcPort -> $dstIp:$dstPort (Longitud: $length bytes)")
                } else {
                    Log.d(TAG, "[TCP Recibido] $srcIp -> $dstIp (Longitud: $length bytes)")
                }
            }
            17 -> { // Protocolo UDP
                if (length >= ihl + 8) {
                    val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
                    val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)

                    if (dstPort == 53) {
                        val dnsOffset = ihl + 8
                        val dnsLength = length - dnsOffset
                        if (dnsLength > 12) {
                            val domain = parseDnsQueryDomain(buffer, dnsOffset, length)
                            if (domain != null) {
                                Log.d(TAG, "[DNS Interceptado] $srcIp:$srcPort -> $dstIp:$dstPort | Consulta dominio: '$domain'")
                                if (isDomainBlocked(domain)) {
                                    Log.i(TAG, "[DNS Bloqueado] Dominio '$domain' en lista negra. Respondiendo con 0.0.0.0")
                                    sendDnsBlockedResponse(buffer, ihl, dnsOffset, dnsLength, outputStream)
                                } else {
                                    Log.d(TAG, "[DNS Reenviado] Dominio '$domain' permitido. Reenviando consulta a 8.8.8.8:53")
                                    forwardDnsQuery(buffer, ihl, dnsOffset, dnsLength, outputStream)
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "[UDP Recibido (no DNS)] $srcIp:$srcPort -> $dstIp:$dstPort (Longitud: $length bytes)")
                    }
                }
            }
            else -> {
                Log.d(TAG, "[Protocolo IP $protocol Recibido] $srcIp -> $dstIp (Longitud: $length bytes)")
            }
        }
    }

    private fun parseDnsQueryDomain(buffer: ByteArray, dnsOffset: Int, totalLength: Int): String? {
        return try {
            var pos = dnsOffset + 12
            val domainBuilder = StringBuilder()

            while (pos < totalLength) {
                val labelLen = buffer[pos].toInt() and 0xFF
                if (labelLen == 0) break
                if (pos + 1 + labelLen > totalLength) return null

                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append(".")
                }

                for (i in 0 until labelLen) {
                    domainBuilder.append(buffer[pos + 1 + i].toInt().toChar())
                }
                pos += 1 + labelLen
            }
            domainBuilder.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun isDomainBlocked(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim('.')
        val currentBlocked = activeBlockedDomains
        return currentBlocked.contains(cleanDomain) || currentBlocked.any { cleanDomain.endsWith(".$it") }
    }

    private fun sendDnsBlockedResponse(
        buffer: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        dnsLength: Int,
        outputStream: FileOutputStream,
    ) {
        val srcIp = ByteArray(4)
        val dstIp = ByteArray(4)
        System.arraycopy(buffer, 12, srcIp, 0, 4)
        System.arraycopy(buffer, 16, dstIp, 0, 4)

        val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
        val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)

        val responseDns = ByteArray(dnsLength + 16)
        System.arraycopy(buffer, dnsOffset, responseDns, 0, dnsLength)

        responseDns[2] = 0x81.toByte()
        responseDns[3] = 0x80.toByte()
        responseDns[6] = 0x00.toByte()
        responseDns[7] = 0x01.toByte()

        var ansPos = dnsLength
        responseDns[ansPos++] = 0xC0.toByte()
        responseDns[ansPos++] = 0x0C.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x01.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x01.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x01.toByte()
        responseDns[ansPos++] = 0x2C.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x04.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos++] = 0x00.toByte()
        responseDns[ansPos] = 0x00.toByte()

        writeUdpResponsePacket(dstIp, srcIp, dstPort, srcPort, responseDns, outputStream)
    }

    private fun forwardDnsQuery(
        buffer: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        dnsLength: Int,
        outputStream: FileOutputStream,
    ) {
        val srcIp = ByteArray(4)
        val dstIp = ByteArray(4)
        System.arraycopy(buffer, 12, srcIp, 0, 4)
        System.arraycopy(buffer, 16, dstIp, 0, 4)

        val srcPort = ((buffer[ihl].toInt() and 0xFF) shl 8) or (buffer[ihl + 1].toInt() and 0xFF)
        val dstPort = ((buffer[ihl + 2].toInt() and 0xFF) shl 8) or (buffer[ihl + 3].toInt() and 0xFF)

        try {
            val socket = DatagramSocket()
            protect(socket)
            socket.soTimeout = 2000

            val dnsQuery = ByteArray(dnsLength)
            System.arraycopy(buffer, dnsOffset, dnsQuery, 0, dnsLength)

            val dnsServer = InetAddress.getByName("8.8.8.8")
            val queryPacket = DatagramPacket(dnsQuery, dnsLength, dnsServer, 53)
            socket.send(queryPacket)

            val respBuffer = ByteArray(1500)
            val respPacket = DatagramPacket(respBuffer, respBuffer.size)
            socket.receive(respPacket)
            socket.close()

            val dnsResp = ByteArray(respPacket.length)
            System.arraycopy(respBuffer, 0, dnsResp, 0, respPacket.length)

            writeUdpResponsePacket(dstIp, srcIp, dstPort, srcPort, dnsResp, outputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error al reenviar consulta DNS", e)
        }
    }

    private fun writeUdpResponsePacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
        outputStream: FileOutputStream,
    ) {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + payload.size
        val packet = ByteArray(totalLen)

        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = (totalLen shr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x00.toByte()
        packet[6] = 0x00.toByte()
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()
        packet[9] = 17.toByte()
        packet[10] = 0x00.toByte()
        packet[11] = 0x00.toByte()

        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        val ipChecksum = computeChecksum(packet, 20)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        packet[20] = (srcPort shr 8).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = (dstPort shr 8).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        val udpLen = udpHeaderLen + payload.size
        packet[24] = (udpLen shr 8).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0x00.toByte()
        packet[27] = 0x00.toByte()

        System.arraycopy(payload, 0, packet, 28, payload.size)

        synchronized(outputStream) {
            outputStream.write(packet)
        }
    }

    private fun computeChecksum(data: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Filter VPN",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notificación de estado del servicio Focus Filter VPN"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Filter")
            .setContentText("Servicio VPN activo - Filtro de sitios web")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        dbJob?.cancel()
        dbJob = null
        stopVpn()
        super.onDestroy()
    }
}