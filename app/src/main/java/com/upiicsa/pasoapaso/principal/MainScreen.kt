package com.upiicsa.pasoapaso.principal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.upiicsa.pasoapaso.core.AppContainer

private enum class SeccionPrincipal(
    val titulo: String,
    val icono: ImageVector
) {
    INICIO("Inicio", Icons.Default.Home),
    DIARIO("Diario", Icons.Default.Edit),
    AYUDA("Ayuda", Icons.Default.Favorite),
    AJUSTES("Ajustes", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    appContainer: AppContainer,
    onCerrarSesion: () -> Unit
) {
    var seccionSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val secciones = SeccionPrincipal.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                secciones.forEachIndexed { index, seccion ->
                    NavigationBarItem(
                        selected = seccionSeleccionada == index,
                        onClick = { seccionSeleccionada = index },
                        icon = { Icon(seccion.icono, contentDescription = seccion.titulo) },
                        label = { Text(seccion.titulo) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (secciones[seccionSeleccionada]) {
                SeccionPrincipal.INICIO -> InicioScreen(
                    appContainer = appContainer,
                    onIrADiario = { seccionSeleccionada = 1 },
                    onIrAAyuda = { seccionSeleccionada = 2 }
                )
                SeccionPrincipal.DIARIO -> DiarioScreen(
                    appContainer = appContainer,
                    onIrAAyuda = { seccionSeleccionada = 2 }
                )
                SeccionPrincipal.AYUDA -> AyudaScreen(
                    appContainer = appContainer
                )
                SeccionPrincipal.AJUSTES -> AjustesScreen(
                    appContainer = appContainer,
                    onCerrarSesion = onCerrarSesion
                )
            }
        }
    }
}