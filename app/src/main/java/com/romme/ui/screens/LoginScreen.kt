package com.romme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romme.BuildConfig
import com.romme.network.ConnectionState
import com.romme.ui.GameViewModel

@Composable
fun LoginScreen(
    viewModel: GameViewModel,
    onLoginSuccess: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val savedSettings = remember { viewModel.getLoginSettings() }

    var serverUrl by remember {
        mutableStateOf(savedSettings.serverUrl.ifEmpty { BuildConfig.SERVER_URL })
    }
    var socketPath by remember {
        mutableStateOf(savedSettings.socketPath.ifEmpty { BuildConfig.SOCKET_PATH })
    }
    var nextcloudUrl by remember {
        mutableStateOf(savedSettings.nextcloudUrl.ifEmpty { BuildConfig.NEXTCLOUD_URL })
    }
    var nextcloudUsername by remember { mutableStateOf(savedSettings.nextcloudUsername) }
    var nextcloudPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.tryAutoLogin()
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onLoginSuccess()
        }
    }

    val isConnecting = connectionState == ConnectionState.CONNECTING
    val canLogin = !isConnecting &&
            serverUrl.isNotBlank() &&
            nextcloudUrl.isNotBlank() &&
            nextcloudUsername.isNotBlank() &&
            nextcloudPassword.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "♠ ♥ ♦ ♣", fontSize = 48.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Open Romme",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server-URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnecting
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nextcloudUrl,
            onValueChange = { nextcloudUrl = it },
            label = { Text("Nextcloud-URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnecting
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nextcloudUsername,
            onValueChange = { nextcloudUsername = it },
            label = { Text("Nextcloud-Benutzername") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnecting
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nextcloudPassword,
            onValueChange = { nextcloudPassword = it },
            label = { Text("App-Passwort") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnecting
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = socketPath,
            onValueChange = { socketPath = it },
            label = { Text("Socket-Pfad") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnecting
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isConnecting -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Verbinde mit Server...", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            }
            connectionState == ConnectionState.ERROR -> {
                val msg = errorMessage ?: "Verbindung fehlgeschlagen. Bitte Einstellungen prüfen."
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            else -> {}
        }

        Button(
            onClick = {
                viewModel.clearError()
                viewModel.loginWithAppPassword(
                    serverUrl = serverUrl.trimEnd('/'),
                    socketPath = socketPath.trim(),
                    nextcloudUrl = nextcloudUrl.trimEnd('/'),
                    username = nextcloudUsername.trim(),
                    password = nextcloudPassword,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            enabled = canLogin
        ) {
            Text(
                "Anmelden",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
