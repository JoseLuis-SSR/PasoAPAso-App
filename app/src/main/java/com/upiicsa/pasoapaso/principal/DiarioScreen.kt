package com.upiicsa.pasoapaso.principal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upiicsa.pasoapaso.core.AppContainer
import com.upiicsa.pasoapaso.diario.ResultadoGuardadoNota
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiarioScreen(
    appContainer: AppContainer,
    onIrAAyuda: () -> Unit
) {
    val notas by appContainer.diarioRepository.observarNotas().collectAsState(initial = emptyList())
    val detonantes by appContainer.diarioRepository.observarDetonantes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var nivelMalestar by remember { mutableIntStateOf(3) }
    var detonanteSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var textoNota by remember { mutableStateOf("") }
    var mostrarAlertaCrisis by remember { mutableStateOf(false) }

    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    if (mostrarAlertaCrisis) {
        AlertDialog(
            onDismissRequest = { mostrarAlertaCrisis = false },
            title = { Text("Estamos contigo") },
            text = {
                Text(
                    "Notamos que estás atravesando un momento muy difícil. Tu nota se guardó de forma segura, pero queremos acercarte opciones de apoyo inmediato y gratuito."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarAlertaCrisis = false
                        onIrAAyuda()
                    }
                ) {
                    Text("Ver líneas y contactos de ayuda")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaCrisis = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Diario Emocional",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Todo lo que escribes aquí se guarda cifrado en tu dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nivel de malestar actual (1 = tranquilo, 5 = muy intenso)",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..5).forEach { nivel ->
                            FilterChip(
                                selected = nivelMalestar == nivel,
                                onClick = { nivelMalestar = nivel },
                                label = { Text("$nivel") }
                            )
                        }
                    }

                    if (detonantes.isNotEmpty()) {
                        Text(
                            text = "Detonante (opcional)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            detonantes.forEach { detonante ->
                                FilterChip(
                                    selected = detonanteSeleccionadoId == detonante.id,
                                    onClick = {
                                        detonanteSeleccionadoId =
                                            if (detonanteSeleccionadoId == detonante.id) null else detonante.id
                                    },
                                    label = { Text(detonante.etiqueta) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = textoNota,
                        onValueChange = { textoNota = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("¿Qué estás sintiendo o pensando hoy?") }
                    )

                    Button(
                        onClick = {
                            if (textoNota.isNotBlank()) {
                                val textoAGuardar = textoNota.trim()
                                val detonanteId = detonanteSeleccionadoId
                                scope.launch {
                                    val resultado = appContainer.diarioRepository.guardarNota(
                                        nivelMalestar = nivelMalestar,
                                        detonanteId = detonanteId,
                                        texto = textoAGuardar
                                    )
                                    textoNota = ""
                                    detonanteSeleccionadoId = null
                                    if (resultado is ResultadoGuardadoNota.GuardadaConAlertaCrisis) {
                                        mostrarAlertaCrisis = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = textoNota.isNotBlank()
                    ) {
                        Text("Guardar entrada cifrada")
                    }
                }
            }
        }

        item {
            Text(
                text = "Historial (${notas.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(items = notas, key = { it.nota.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Malestar: ${item.nota.nivelMalestar}/5",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatoFecha.format(Date(item.nota.fechaHora)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.detonante?.let { det ->
                        Text(
                            text = "Detonante: ${det.etiqueta}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = item.nota.texto,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}