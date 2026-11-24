package com.upiicsa.pasoapaso.seguridad.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.upiicsa.pasoapaso.seguridad.Tratamiento

@Composable
fun RegistroScreen(
    preferenciasSeguras: PreferenciasSeguras,
    databaseKeyProvider: DatabaseKeyProvider,
    onRegistroFinalizado: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val sensorDisponible = remember { BiometricAuthHelper.puedeUsarHuella(context) }

    var pasoActual by remember { mutableIntStateOf(1) }

    // Estado Paso 1
    var codigo by remember { mutableStateOf("") }
    var pista by remember { mutableStateOf("") }
    var tratamientoSeleccionado by remember { mutableStateOf(Tratamiento.NEUTRO) }
    var errorCodigo by remember { mutableStateOf<String?>(null) }

    // Estado Paso 2
    var huellaActivada by remember { mutableStateOf(false) }
    var modoAnonimoActivado by remember { mutableStateOf(false) }
    var mensajeHuella by remember { mutableStateOf<String?>(null) }
    var passphraseGenerada by remember { mutableStateOf<ByteArray?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de progreso de 3 pasos
            LinearProgressIndicator(
                progress = { pasoActual / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            when (pasoActual) {
                1 -> {
                    Text(
                        text = "Paso 1 de 3 · Tu código",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Crea tu código",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Se pedirá cada vez que abras la app. No te pedimos nombre, correo ni teléfono.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { nuevo ->
                            if (nuevo.length <= 6 && nuevo.all { it.isDigit() }) {
                                codigo = nuevo
                                errorCodigo = null
                            }
                        },
                        label = { Text("Código de 4 a 6 dígitos") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = errorCodigo != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorCodigo != null) {
                        Text(
                            text = errorCodigo!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = pista,
                        onValueChange = { pista = it },
                        label = { Text("Pista (Opcional, solo tú la verás)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "¿Cómo prefieres que te escriba? (Opcional)",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Tratamiento.entries.forEach { opcion ->
                            val etiqueta = when (opcion) {
                                Tratamiento.MASCULINO -> "En masculino"
                                Tratamiento.FEMENINO -> "En femenino"
                                Tratamiento.NEUTRO -> "En neutro"
                            }
                            FilterChip(
                                selected = tratamientoSeleccionado == opcion,
                                onClick = { tratamientoSeleccionado = opcion },
                                label = { Text(etiqueta) }
                            )
                        }
                    }

                    Text(
                        text = "Solo cambia la concordancia de los mensajes. Si no eliges, te escribimos en neutro. Se queda en este teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lo que no te pedimos",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ni nombre, ni correo, ni teléfono, ni redes sociales. Nada de esto se envía a un servidor: tu código se guarda cifrado en este teléfono.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (codigo.length !in 4..6) {
                                errorCodigo = "El código debe tener entre 4 y 6 dígitos."
                            } else {
                                preferenciasSeguras.pistaCodigo = pista
                                preferenciasSeguras.tratamiento = tratamientoSeleccionado
                                passphraseGenerada = databaseKeyProvider.inicializarConCodigo(codigo)
                                pasoActual = 2
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                2 -> {
                    Text(
                        text = "Paso 2 de 3 · Protección",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Protege tu libreta",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Dos protecciones opcionales. Puedes cambiarlas en Ajustes.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Tarjeta Huella
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Entrar con mi huella",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Switch(
                                    checked = huellaActivada,
                                    enabled = sensorDisponible,
                                    onCheckedChange = { activar ->
                                        if (activar && activity != null && passphraseGenerada != null) {
                                            val cipher = databaseKeyProvider.prepararKeystoreParaHuella()
                                            BiometricAuthHelper.autenticarConHuella(
                                                activity = activity,
                                                modoAnonimo = modoAnonimoActivado,
                                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                onExito = { result ->
                                                    val authCipher = result.cryptoObject?.cipher ?: cipher
                                                    databaseKeyProvider.completarEnvolturaHuella(
                                                        authCipher,
                                                        passphraseGenerada!!
                                                    )
                                                    huellaActivada = true
                                                    mensajeHuella = null
                                                },
                                                onUsarCodigo = {
                                                    huellaActivada = false
                                                },
                                                onError = { err ->
                                                    huellaActivada = false
                                                    mensajeHuella = err
                                                }
                                            )
                                        } else {
                                            databaseKeyProvider.invalidarCopiaHuella()
                                            huellaActivada = false
                                        }
                                    }
                                )
                            }

                            Text(
                                text = if (sensorDisponible) {
                                    "Usa la huella que ya tiene tu teléfono. No la guardamos ni la podemos ver: Android solo nos responde si eres tú o no."
                                } else {
                                    "Este teléfono no tiene sensor biométrico o no tiene ninguna huella registrada. Tu código queda como única puerta."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (mensajeHuella != null) {
                                Text(
                                    text = mensajeHuella!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Tarjeta Modo Anónimo
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Modo anónimo",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "En tu teléfono, la app se llamará Mis Notas y usará un ícono de libreta. Quien tome tu teléfono no sabrá de qué se trata.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (modoAnonimoActivado) {
                                    Button(
                                        onClick = { modoAnonimoActivado = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Activado")
                                    }
                                    OutlinedButton(
                                        onClick = { modoAnonimoActivado = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Dejarla igual")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { modoAnonimoActivado = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Activar")
                                    }
                                    Button(
                                        onClick = { modoAnonimoActivado = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Dejarla igual")
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            pasoActual = 3
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            if (huellaActivada) databaseKeyProvider.invalidarCopiaHuella()
                            huellaActivada = false
                            modoAnonimoActivado = false
                            pasoActual = 3
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Omitir las dos por ahora")
                    }
                }

                3 -> {
                    Text(
                        text = "Paso 3 de 3 · Listo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ya está protegida",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    BloqueConfirmacion(
                        titulo = "Qué cambió",
                        descripcion = buildString {
                            append(if (huellaActivada) "Entras con tu huella" else "Entras con tu código de seguridad")
                            append(
                                if (modoAnonimoActivado) ", y en tu pantalla de inicio se ve una libreta llamada Mis Notas."
                                else "."
                            )
                        },
                        destacado = true
                    )

                    BloqueConfirmacion(
                        titulo = "Qué no cambió",
                        descripcion = "Tu código, tus notas y todo lo que hay dentro siguen igual. El código es tu respaldo si la huella falla.",
                        destacado = false
                    )

                    BloqueConfirmacion(
                        titulo = "Lo que nunca cambia",
                        descripcion = "Con modo anónimo o sin él, la app nunca te pide nombre, correo ni teléfono, y tus notas no salen de aquí.",
                        destacado = false
                    )

                    Button(
                        onClick = {
                            val key = passphraseGenerada ?: databaseKeyProvider.desbloquearConCodigo(codigo)
                            if (key != null) {
                                preferenciasSeguras.registroCompletado = true
                                onRegistroFinalizado(key)
                                preferenciasSeguras.setModoAnonimo(modoAnonimoActivado)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueConfirmacion(
    titulo: String,
    descripcion: String,
    destacado: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (destacado) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = descripcion, style = MaterialTheme.typography.bodyMedium)
        }
    }
}