package com.upiicsa.pasoapaso.seguridad.ui

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.upiicsa.pasoapaso.seguridad.BiometricAuthHelper
import com.upiicsa.pasoapaso.seguridad.DatabaseKeyProvider
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras

@Composable
fun BloqueoScreen(
    preferenciasSeguras: PreferenciasSeguras,
    databaseKeyProvider: DatabaseKeyProvider,
    onDesbloqueado: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val modoAnonimo = remember { preferenciasSeguras.modoAnonimoActivo }
    val huellaActiva = remember {
        preferenciasSeguras.huellaActiva && BiometricAuthHelper.puedeUsarHuella(context)
    }
    val pistaGuardada = remember { preferenciasSeguras.pistaCodigo }

    var codigoIngresado by remember { mutableStateOf("") }
    var intentosFallidos by remember { mutableIntStateOf(0) }
    var mostrarPista by remember { mutableStateOf(false) }
    var mensajeEstado by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoNuevoCodigo by remember { mutableStateOf(false) }
    var nuevoCodigo by remember { mutableStateOf("") }
    var errorNuevoCodigo by remember { mutableStateOf<String?>(null) }

    fun lanzarHuella() {
        if (activity == null) return
        try {
            val cipher = databaseKeyProvider.obtenerCipherDescifradoHuella() ?: return
            BiometricAuthHelper.autenticarConHuella(
                activity = activity,
                modoAnonimo = modoAnonimo,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onExito = { result ->
                    val authCipher = result.cryptoObject?.cipher ?: cipher
                    val passphrase = databaseKeyProvider.desbloquearConCipherHuella(authCipher)
                    if (passphrase != null) {
                        onDesbloqueado(passphrase)
                    } else {
                        mensajeEstado = "No se pudo verificar con huella. Escribe tu código."
                    }
                },
                onUsarCodigo = { /* El usuario eligió escribir código */ },
                onError = { err -> mensajeEstado = err }
            )
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Escenario PS-10: se borraron huellas del dispositivo
            mensajeEstado = "Cambiaron las huellas del teléfono. Escribe tu código para reactivar el acceso."
        }
    }

    LaunchedEffect(huellaActiva) {
        if (huellaActiva) {
            lanzarHuella()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título neutro en modo anónimo ("Mis Notas") sin marcas de Paso a Paso (HU-02)
            Text(
                text = if (modoAnonimo) "Mis Notas" else "Paso a Paso",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (modoAnonimo) "Tus notas están protegidas." else "Tu espacio está protegido.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (huellaActiva) {
                Button(
                    onClick = { lanzarHuella() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Desbloquear con huella",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "o escribe tu código",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = codigoIngresado,
                onValueChange = { nuevo ->
                    if (nuevo.length <= 6 && nuevo.all { it.isDigit() }) {
                        codigoIngresado = nuevo
                        mensajeEstado = null
                    }
                },
                label = { Text("Código") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (mensajeEstado != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensajeEstado!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val passphrase = databaseKeyProvider.desbloquearConCodigo(codigoIngresado)
                    if (passphrase != null) {
                        onDesbloqueado(passphrase)
                    } else {
                        intentosFallidos++
                        codigoIngresado = ""
                        mensajeEstado = "Código incorrecto."
                        // PS-03: tras tres intentos fallidos se ofrece la pista
                        if (intentosFallidos >= 3 && !pistaGuardada.isNullOrBlank()) {
                            mostrarPista = true
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!pistaGuardada.isNullOrBlank()) {
                if (mostrarPista) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pista: $pistaGuardada",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    TextButton(onClick = { mostrarPista = true }) {
                        Text("Ver pista")
                    }
                }
            }

            TextButton(
                onClick = {
                    if (activity != null) {
                        BiometricAuthHelper.autenticarParaRestablecerCodigo(
                            activity = activity,
                            onAutorizado = {
                                mostrarDialogoNuevoCodigo = true
                            },
                            onError = { err ->
                                mensajeEstado = "No se pudo verificar tu identidad: $err"
                            }
                        )
                    }
                }
            ) {
                Text("Olvidé mi código")
            }
        }
    }

    if (mostrarDialogoNuevoCodigo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevoCodigo = false },
            title = { Text("Crear código nuevo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tu diario cifrado se conservará intacto. Escribe un código nuevo de 4 a 6 dígitos:")
                    OutlinedTextField(
                        value = nuevoCodigo,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                nuevoCodigo = it
                                errorNuevoCodigo = null
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        label = { Text("Nuevo código") }
                    )
                    if (errorNuevoCodigo != null) {
                        Text(
                            text = errorNuevoCodigo!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nuevoCodigo.length !in 4..6) {
                            errorNuevoCodigo = "Debe tener entre 4 y 6 dígitos."
                        } else {
                            // Si se autorizó con huella/credencial, re-envuelve Copia B
                            val cipher = runCatching { databaseKeyProvider.obtenerCipherDescifradoHuella() }.getOrNull()
                            if (cipher != null && activity != null) {
                                BiometricAuthHelper.autenticarConHuella(
                                    activity = activity,
                                    modoAnonimo = modoAnonimo,
                                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                    onExito = { res ->
                                        val authCipher = res.cryptoObject?.cipher ?: cipher
                                        val key = databaseKeyProvider.desbloquearConCipherHuella(authCipher)
                                        if (key != null) {
                                            databaseKeyProvider.cambiarCodigo(nuevoCodigo, key)
                                            mostrarDialogoNuevoCodigo = false
                                            onDesbloqueado(key)
                                        }
                                    },
                                    onUsarCodigo = {},
                                    onError = { errorNuevoCodigo = it }
                                )
                            } else {
                                errorNuevoCodigo = "Activa primero la huella para recuperar sin perder tus notas."
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNuevoCodigo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}