package com.um.eventosmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.um.eventosmobile.shared.AuthApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authApi: AuthApi,
    onLoginSuccess: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Sesión") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Eventos Mobile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    error = null
                },
                label = { Text("Usuario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                enabled = !loading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    error = null
                },
                label = { Text("Contraseña") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                visualTransformation = PasswordVisualTransformation(),
                enabled = !loading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            error?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "Por favor complete todos los campos"
                        return@Button
                    }
                    
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val token = authApi.login(username.trim(), password)
                            loading = false
                            onLoginSuccess(token)
                        } catch (e: com.um.eventosmobile.shared.AuthException) {
                            // Usar el mensaje amigable de la excepción personalizada
                            loading = false
                            error = e.message ?: "Error al iniciar sesión"
                        } catch (e: Exception) {
                            loading = false
                            // Mensajes amigables para otros errores
                            error = when {
                                e.message?.contains("401", ignoreCase = true) == true ||
                                e.message?.contains("incorrectos", ignoreCase = true) == true ||
                                e.message?.contains("inválidas", ignoreCase = true) == true -> {
                                    "Usuario o contraseña incorrectos"
                                }
                                e.message?.contains("403", ignoreCase = true) == true -> {
                                    "Acceso denegado"
                                }
                                e.message?.contains("conexión", ignoreCase = true) == true ||
                                e.message?.contains("conexion", ignoreCase = true) == true ||
                                e.message?.contains("internet", ignoreCase = true) == true ||
                                e.message?.contains("network", ignoreCase = true) == true -> {
                                    "Error de conexión. Verifique su conexión a internet"
                                }
                                e.message?.contains("timeout", ignoreCase = true) == true -> {
                                    "Tiempo de espera agotado. Intente nuevamente"
                                }
                                else -> {
                                    "Error al iniciar sesión. Intente nuevamente"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Ingresar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

