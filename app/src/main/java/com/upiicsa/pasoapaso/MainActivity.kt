package com.upiicsa.pasoapaso

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.upiicsa.pasoapaso.core.AppDatabase
import com.upiicsa.pasoapaso.seguridad.DatabaseKeyProvider
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras
import com.upiicsa.pasoapaso.seguridad.ui.BloqueoScreen
import com.upiicsa.pasoapaso.seguridad.ui.RegistroScreen
import com.upiicsa.pasoapaso.ui.theme.PasoAPasoTheme

class MainActivity : FragmentActivity() {

    private lateinit var preferenciasSeguras: PreferenciasSeguras
    private lateinit var databaseKeyProvider: DatabaseKeyProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bloquea capturas de pantalla y oculta contenido en aplicaciones recientes (HU-02)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        preferenciasSeguras = PreferenciasSeguras(this)
        databaseKeyProvider = DatabaseKeyProvider(preferenciasSeguras)

        enableEdgeToEdge()
        setContent {
            PasoAPasoTheme {
                var desbloqueado by remember { mutableStateOf(false) }
                var registroCompletado by remember {
                    mutableStateOf(preferenciasSeguras.registroCompletado)
                }

                when {
                    !registroCompletado -> {
                        RegistroScreen(
                            preferenciasSeguras = preferenciasSeguras,
                            databaseKeyProvider = databaseKeyProvider,
                            onRegistroFinalizado = { passphrase ->
                                AppDatabase.obtenerInstancia(this, passphrase)
                                registroCompletado = true
                                desbloqueado = true
                            }
                        )
                    }

                    !desbloqueado -> {
                        BloqueoScreen(
                            preferenciasSeguras = preferenciasSeguras,
                            databaseKeyProvider = databaseKeyProvider,
                            onDesbloqueado = { passphrase ->
                                AppDatabase.obtenerInstancia(this, passphrase)
                                desbloqueado = true
                            }
                        )
                    }

                    else -> {
                        // Aquí se conectará la navegación principal (Inicio, Ayuda, Mi Historia)
                        // creada por tu compañero en feature/ui-asistente
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Base de datos cifrada abierta",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "Trato elegido: ${preferenciasSeguras.tratamiento.name}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            databaseKeyProvider.cerrarSesion()
            AppDatabase.cerrarInstancia()
        }
    }
}