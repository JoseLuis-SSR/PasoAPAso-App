package com.upiicsa.pasoapaso.principal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upiicsa.pasoapaso.core.AppContainer
import com.upiicsa.pasoapaso.diario.PatronDetectado
import com.upiicsa.pasoapaso.seguridad.Tratamiento

@Composable
fun InicioScreen(
    appContainer: AppContainer,
    onIrADiario: () -> Unit,
    onIrAAyuda: () -> Unit
) {
    val notas by appContainer.diarioRepository.observarNotas().collectAsState(initial = emptyList())
    val tratamiento = appContainer.preferenciasSeguras.tratamiento
    var patronDetectado by remember { mutableStateOf<PatronDetectado?>(null) }

    LaunchedEffect(notas.size, tratamiento) {
        patronDetectado = appContainer.patronesAnalyzer.analizarUltimosSieteDias(
            tratamiento = tratamiento
        )
    }

    val saludo = when (tratamiento) {
        Tratamiento.MASCULINO -> "Bienvenido a tu espacio seguro"
        Tratamiento.FEMENINO -> "Bienvenida a tu espacio seguro"
        Tratamiento.NEUTRO -> "Te damos la bienvenida a tu espacio seguro"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paso a Paso",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = saludo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tarjeta de auxilio inmediato
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "¿Necesitas apoyo ahora mismo?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Accede de inmediato a tus contactos de confianza y líneas gratuitas de atención 24/7.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Button(
                    onClick = onIrAAyuda,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Abrir Red de Ayuda")
                }
            }
        }

        // Tarjeta de seguimiento semanal (PatronesAnalyzer)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tu seguimiento (últimos 7 días)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Entradas registradas en tu diario: ${notas.size}",
                    style = MaterialTheme.typography.bodyMedium
                )

                val patron = patronDetectado
                if (patron != null) {
                    Text(
                        text = patron.mensajePreventivo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Escribir cómo te sientes ayuda a identificar qué momentos o detonantes requieren más cuidado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onIrADiario) {
                        Text("Registrar cómo me siento")
                    }
                }
            }
        }
    }
}