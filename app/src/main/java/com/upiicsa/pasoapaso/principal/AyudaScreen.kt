package com.upiicsa.pasoapaso.principal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.upiicsa.pasoapaso.core.AppContainer
import com.upiicsa.pasoapaso.emergencia.ContactoRepository
import kotlinx.coroutines.launch

@Composable
fun AyudaScreen(
    appContainer: AppContainer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contacto by appContainer.contactoRepository.observarContacto().collectAsState(initial = null)
    val lineas by appContainer.ayudaRepository.observarLineas("MX").collectAsState(initial = emptyList())

    var editandoContacto by remember { mutableStateOf(false) }
    var aliasInput by remember { mutableStateOf("") }
    var telefonoInput by remember { mutableStateOf("") }
    var mensajeInput by remember { mutableStateOf("") }

    fun abrirMarcador(numero: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$numero")
        }
        context.startActivity(intent)
    }

    fun enviarSmsApoyo(numero: String, mensaje: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$numero")
            putExtra("sms_body", mensaje)
        }
        context.startActivity(intent)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Red de Ayuda",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Contacta a alguien de confianza o comunícate con líneas gratuitas de apoyo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sección 1: Contacto de Confianza
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tu contacto de confianza",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    val contactoActual = contacto
                    if (contactoActual != null && !editandoContacto) {
                        Text(
                            text = "${contactoActual.alias} • ${contactoActual.telefono}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Mensaje rápido: \"${contactoActual.mensaje}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { abrirMarcador(contactoActual.telefono) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Llamar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    enviarSmsApoyo(contactoActual.telefono, contactoActual.mensaje)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Enviar SMS")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    aliasInput = contactoActual.alias
                                    telefonoInput = contactoActual.telefono
                                    mensajeInput = contactoActual.mensaje
                                    editandoContacto = true
                                }
                            ) {
                                Text("Editar")
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        appContainer.contactoRepository.eliminarContacto()
                                    }
                                }
                            ) {
                                Text("Eliminar")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = aliasInput,
                            onValueChange = { aliasInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Nombre o alias (ej. Mamá, Hermano, Amiga)") }
                        )
                        OutlinedTextField(
                            value = telefonoInput,
                            onValueChange = { telefonoInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            label = { Text("Teléfono") }
                        )
                        OutlinedTextField(
                            value = mensajeInput,
                            onValueChange = { mensajeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("Mensaje de apoyo (opcional)") },
                            placeholder = { Text(ContactoRepository.MENSAJE_PREDETERMINADO) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (editandoContacto) {
                                OutlinedButton(
                                    onClick = { editandoContacto = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                            }
                            Button(
                                onClick = {
                                    if (aliasInput.isNotBlank() && telefonoInput.isNotBlank()) {
                                        scope.launch {
                                            appContainer.contactoRepository.guardarContacto(
                                                alias = aliasInput,
                                                telefono = telefonoInput,
                                                mensajePersonalizado = mensajeInput
                                            )
                                            aliasInput = ""
                                            telefonoInput = ""
                                            mensajeInput = ""
                                            editandoContacto = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = aliasInput.isNotBlank() && telefonoInput.isNotBlank()
                            ) {
                                Text("Guardar contacto")
                            }
                        }
                    }
                }
            }
        }

        // Sección 2: Líneas de Atención 24/7
        item {
            Text(
                text = "Líneas de atención gratuitas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (lineas.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Línea de la Vida (México)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Atención psicológica y apoyo emocional gratuito las 24 horas.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { abrirMarcador("8009112000") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Llamar al 800 911 2000")
                        }
                    }
                }
            }
        } else {
            items(items = lineas, key = { it.id }) { linea ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = linea.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${linea.institucion} • ${linea.horario}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { abrirMarcador(linea.telefono) }
                        ) {
                            Text("Llamar (${linea.telefono})")
                        }
                    }
                }
            }
        }
    }
}