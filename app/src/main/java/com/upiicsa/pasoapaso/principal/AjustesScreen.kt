package com.upiicsa.pasoapaso.principal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upiicsa.pasoapaso.core.AppContainer
import com.upiicsa.pasoapaso.seguridad.Tratamiento

@Composable
fun AjustesScreen(
    appContainer: AppContainer,
    onCerrarSesion: () -> Unit
) {
    val prefs = appContainer.preferenciasSeguras

    var tratamientoActual by remember { mutableStateOf(prefs.tratamiento) }
    var huellaActiva by remember { mutableStateOf(prefs.huellaActiva) }
    var ocultarDetalles by remember { mutableStateOf(prefs.ocultarDetallesInicio) }
    var modoAnonimo by remember { mutableStateOf(prefs.modoAnonimoActivo) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ajustes y Privacidad",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Personalización de trato
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "¿Cómo prefieres que te hable la app?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tratamiento.entries.forEach { opcion ->
                        val etiqueta = when (opcion) {
                            Tratamiento.MASCULINO -> "Él"
                            Tratamiento.FEMENINO -> "Ella"
                            Tratamiento.NEUTRO -> "Neutro"
                        }
                        FilterChip(
                            selected = tratamientoActual == opcion,
                            onClick = {
                                tratamientoActual = opcion
                                prefs.tratamiento = opcion
                            },
                            label = { Text(etiqueta) }
                        )
                    }
                }
            }
        }

        // Seguridad y disfraz
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Seguridad y discreción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Desbloqueo con huella", fontWeight = FontWeight.Medium)
                        Text(
                            "Permite entrar usando biometría además de tu PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = huellaActiva,
                        onCheckedChange = {
                            huellaActiva = it
                            prefs.huellaActiva = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ocultar detalles en inicio", fontWeight = FontWeight.Medium)
                        Text(
                            "Muestra mensajes más discretos en la pantalla principal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = ocultarDetalles,
                        onCheckedChange = {
                            ocultarDetalles = it
                            prefs.ocultarDetallesInicio = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo discreto (\"Mis Notas\")", fontWeight = FontWeight.Medium)
                        Text(
                            "Cambia el ícono y nombre de la app en tu teléfono.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = modoAnonimo,
                        onCheckedChange = {
                            modoAnonimo = it
                            prefs.setModoAnonimo(it)
                        }
                    )
                }
            }
        }

        // Botón de bloqueo rápido
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Bloquear aplicación ahora")
        }
    }
}