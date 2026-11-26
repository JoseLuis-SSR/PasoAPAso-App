package com.upiicsa.pasoapaso

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.upiicsa.pasoapaso.core.AppContainer
import com.upiicsa.pasoapaso.core.AppDatabase
import com.upiicsa.pasoapaso.diario.NotificacionDiscretaWorker
import com.upiicsa.pasoapaso.seguridad.DatabaseKeyProvider
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras
import com.upiicsa.pasoapaso.seguridad.ui.BloqueoScreen
import com.upiicsa.pasoapaso.seguridad.ui.RegistroScreen
import com.upiicsa.pasoapaso.principal.MainScreen
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
        NotificacionDiscretaWorker.programar(this)

        enableEdgeToEdge()
        setContent {
            PasoAPasoTheme {
                var appContainer by remember { mutableStateOf<AppContainer?>(null) }
                var registroCompletado by remember {
                    mutableStateOf(preferenciasSeguras.registroCompletado)
                }

                when {
                    !registroCompletado -> {
                        RegistroScreen(
                            preferenciasSeguras = preferenciasSeguras,
                            databaseKeyProvider = databaseKeyProvider,
                            onRegistroFinalizado = { passphrase ->
                                appContainer = AppContainer(this@MainActivity, passphrase)
                                registroCompletado = true
                            }
                        )
                    }

                    appContainer == null -> {
                        BloqueoScreen(
                            preferenciasSeguras = preferenciasSeguras,
                            databaseKeyProvider = databaseKeyProvider,
                            onDesbloqueado = { passphrase ->
                                appContainer = AppContainer(this@MainActivity, passphrase)
                            }
                        )
                    }

                    else -> {
                        MainScreen(
                            appContainer = appContainer!!,
                            onCerrarSesion = {
                                databaseKeyProvider.cerrarSesion()
                                AppDatabase.cerrarInstancia()
                                appContainer = null
                            }
                        )
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