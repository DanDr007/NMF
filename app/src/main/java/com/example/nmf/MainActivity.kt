package com.example.nmf

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nmf.ui.FocusFilterViewModel
import com.example.nmf.ui.components.HoldToConfirmButton
import com.example.nmf.ui.screens.BlockedSitesScreen

enum class AppScreen {
    MAIN,
    BLOCKED_SITES
}

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
                )
            }
        }
    }

    private fun requestStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

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

    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun FocusFilterApp(
    viewModel: FocusFilterViewModel = viewModel(),
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.MAIN -> MainScreen(
                viewModel = viewModel,
                onStartVpn = onStartVpn,
                onStopVpn = onStopVpn,
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
    onNavigateToBlockedSites: () -> Unit,
) {
    val isVpnActive by VpnState.isRunning.collectAsState()
    val blockedSites by viewModel.blockedSites.collectAsState()

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
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
                            } else {
                                "Activa el filtro para iniciar tu tiempo de enfoque"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

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

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isVpnActive) {
                    Button(
                        onClick = onStartVpn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
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