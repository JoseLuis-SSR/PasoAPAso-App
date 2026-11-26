package com.upiicsa.pasoapaso.core

import android.content.Context
import com.upiicsa.pasoapaso.ayuda.AyudaRepository
import com.upiicsa.pasoapaso.diario.DiarioRepository
import com.upiicsa.pasoapaso.diario.PatronesAnalyzer
import com.upiicsa.pasoapaso.emergencia.ContactoRepository
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras

class AppContainer(
    context: Context,
    passphrase: ByteArray
) {
    private val appContext = context.applicationContext

    val preferenciasSeguras: PreferenciasSeguras = PreferenciasSeguras(appContext)

    val database: AppDatabase = AppDatabase.obtenerInstancia(
        context = appContext,
        passphraseBytes = passphrase
    )

    val diarioRepository: DiarioRepository = DiarioRepository(database.notaDao())
    val contactoRepository: ContactoRepository = ContactoRepository(database.contactoDao())
    val ayudaRepository: AyudaRepository = AyudaRepository(database.lineaAyudaDao())
    val patronesAnalyzer: PatronesAnalyzer = PatronesAnalyzer(database.notaDao())
}