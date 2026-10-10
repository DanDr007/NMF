package com.example.nmf

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nmf.ui.FocusFilterViewModel
import com.example.nmf.ui.components.HoldToConfirmButton
import com.example.nmf.ui.screens.BlockedSitesScreen

/** Identificadores de pantallas principales */
enum class AppScreen {
    MAIN,
    BLOCKED_SITES
}

/** Paleta de colores serena y reflexiva para autocontrol */
private val PeacefulColorScheme = lightColorScheme(
    primary = Color(0xFF2C5E6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2F0F3),
    onPrimaryContainer = Color(0xFF0F323A),
    secondary = Color(0xFF4C6367),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEE8EC),
    onSecondaryContainer = Color(0xFF081F23),
    surface = Color(0xFFF7FAF9),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFE0E8EA),
    onSurfaceVariant = Color(0xFF3F484A),
    error = Color(0xFF8B4242),
    onError = Color.White,
    errorContainer = Color(0xFFF9E8E8),
    onErrorContainer = Color(0xFF410002),
)

class MainActivity : ComponentActivity() {

    // Launcher para solicitar el permiso oficial de VpnService a Android
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = PeacefulColorScheme) {
                FocusFilterApp(
                    onStartVpn = { requestStartVpn() },
                    onStopVpn = { stopVpnService() },
                    onOpenVpnSettings = { openVpnSettings() },
                )
            }
        }
    }

    /** Solicitar preparación del VpnService o iniciar directamente */
    private fun requestStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    /** Iniciar el servicio MyVpnService */
    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /** Detener el servicio MyVpnService */
    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)
    }

    /** Abrir la pantalla del sistema para la configuración de VPN Always-On */
    private fun openVpnSettings() {
        try {
            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            // Manejar en dispositivos sin la actividad estándar de VPN
        }
    }
}

@Composable
fun FocusFilterApp(
    viewModel: FocusFilterViewModel = viewModel(),
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onOpenVpnSettings: () -> Unit,
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.MAIN -> MainScreen(
                viewModel = viewModel,
                onStartVpn = onStartVpn,
                onStopVpn = onStopVpn,
                onOpenVpnSettings = onOpenVpnSettings,
                onNavigateToBlockedSites = { currentScreen = AppScreen.BLOCKED_SITES },
            )
            AppScreen.BLOCKED_SITES -> BlockedSitesScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MAIN },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FocusFilterViewModel,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onNavigateToBlockedSites: () -> Unit,
) {
    val context = LocalContext.current
    val isVpnActive by VpnState.isRunning.collectAsState()
    val blockedSites by viewModel.blockedSites.collectAsState()

    // Estado persistido de Always-On VPN: determina si el botón "Iniciar VPN" está activado
    var isAlwaysOnConfigured by remember {
        mutableStateOf(VpnPreferences.isAlwaysOnPromptCompleted(context))
    }

    // Modal 1: Guía en la pantalla para abrir la configuración del sistema
    var showFirstVpnModal by remember { mutableStateOf(false) }

    // Modal 2: Confirmación tras volver de la configuración del sistema
    var showConfirmationModal by remember { mutableStateOf(false) }

    // Iniciar la VPN PRIMERO para que Android registre el servicio en la lista de VPNs del sistema
    // y luego abrir la pantalla de ajustes de VPN de Android con la opción Always-On habilitada.
    fun launchAlwaysOnSetupFlow() {
        if (!isVpnActive) {
            onStartVpn()
        }
        onOpenVpnSettings()
        showConfirmationModal = true
    }

    // Modal 1: Guía informativa
    if (showFirstVpnModal) {
        AlertDialog(
            onDismissRequest = { /* No descartar tocando fuera o atrás */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = {
                Text(
                    text = "Protección permanente requerida",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = "Para poder iniciar Focus Filter y garantizar que el filtro continúe funcionando incluso después de reiniciar el teléfono, debes activar la opción 'VPN siempre activa' en Android.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFirstVpnModal = false
                        launchAlwaysOnSetupFlow()
                    },
                ) {
                    Text("Abrir configuración de VPN")
                }
            },
        )
    }

    // Modal 2: Confirmación de activación
    if (showConfirmationModal) {
        AlertDialog(
            onDismissRequest = {
                showConfirmationModal = false
            },
            title = {
                Text(
                    text = "¿Has activado VPN siempre activa?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = "Al activar esta opción se habilitará el botón para iniciar Focus Filter y mantendrás tu espacio libre de distracciones.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        VpnPreferences.setAlwaysOnPromptCompleted(context, true)
                        VpnPreferences.setFirstVpnStartCompleted(context, true)
                        isAlwaysOnConfigured = true
                        showConfirmationModal = false
                    },
                ) {
                    Text("Sí, ya la activé")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationModal = false
                    },
                ) {
                    Text("Lo haré después")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Focus Filter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Tarjeta de estado de la VPN
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVpnActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Estado del filtro",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isVpnActive) "VPN Activa" else "VPN Inactiva",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isVpnActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isVpnActive) {
                                "Protección contra distracciones activada"
                            } else if (isAlwaysOnConfigured) {
                                "Activa el filtro para iniciar tu tiempo de enfoque"
                            } else {
                                "Configura Always-On VPN para habilitar el inicio"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Indicador visual de Always-On VPN
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlwaysOnConfigured) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isAlwaysOnConfigured) {
                                "✅ Protección permanente configurada"
                            } else {
                                "⚠️ Se requiere activar VPN siempre activa"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isAlwaysOnConfigured) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                        )
                    }
                }

                // Opción para abrir/configurar Always-On VPN en ajustes del sistema
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configuración VPN Always-On",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Requisito para habilitar el botón de inicio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { launchAlwaysOnSetupFlow() },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajustes")
                        }
                    }
                }

                // Contador y acceso a Sitios Bloqueados
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Sitios bloqueados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${blockedSites.size} dominios en la lista",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToBlockedSites,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver lista")
                        }
                    }
                }
            }

            // Sección de controles para Iniciar o Detener VPN (con protección de 60s)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isVpnActive) {
                    Button(
                        onClick = onStartVpn,
                        enabled = true, // Desactivado hasta configurar Always-On VPN
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        ),
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar VPN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (!isAlwaysOnConfigured) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para activar la VPN, primero debes configurar 'VPN siempre activa' pulsando en Ajustes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    HoldToConfirmButton(
                        text = "Detener VPN (mantener 60s)",
                        targetDurationSeconds = 60,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        progressColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onConfirmed = onStopVpn,
                    )
                }
            }
        }
    }
}