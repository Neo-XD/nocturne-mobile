package com.nocturne.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nocturne.music.sync.SyncConnectionStatus
import com.nocturne.music.ui.theme.*
import com.nocturne.music.ui.viewmodel.SyncViewModel

@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val connectedHost by viewModel.connectedHost.collectAsState()

    var hostIp by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Playback Sync",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect directly to your PC (via Local Wi-Fi or Tailscale IP) to control playback and sync listening.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NocturneDarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (status == SyncConnectionStatus.CONNECTED) Icons.Default.CheckCircle else Icons.Default.Wifi,
                        contentDescription = "Status",
                        tint = if (status == SyncConnectionStatus.CONNECTED) NocturneTeal else NocturnePurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${status.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = NocturneRed, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (status == SyncConnectionStatus.CONNECTED) {
                    Text(
                        text = "Connected to ${connectedHost ?: "PC Client"}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = NocturneTeal)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = NocturneRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    OutlinedTextField(
                        value = hostIp,
                        onValueChange = { hostIp = it },
                        label = { Text("PC IP or Tailscale Address") },
                        placeholder = { Text("e.g. 100.x.y.z or 192.168.1.50") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NocturnePurple,
                            unfocusedBorderColor = TextDisabled
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("Security PIN (4-6 digits)") },
                        placeholder = { Text("1234") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "PIN", tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NocturnePurple,
                            unfocusedBorderColor = TextDisabled
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.connect(hostIp.trim(), pin.trim()) },
                        enabled = hostIp.isNotBlank() && pin.length >= 4 && status != SyncConnectionStatus.CONNECTING,
                        colors = ButtonDefaults.buttonColors(containerColor = NocturnePurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (status == SyncConnectionStatus.CONNECTING || status == SyncConnectionStatus.AUTHENTICATING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NocturneDarkBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Pair & Connect")
                        }
                    }
                }
            }
        }
    }
}
