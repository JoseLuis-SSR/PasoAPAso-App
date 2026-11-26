package com.upiicsa.pasoapaso

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.upiicsa.pasoapaso.core.AppDatabase
import com.upiicsa.pasoapaso.diario.DiarioRepository
import com.upiicsa.pasoapaso.diario.ResultadoGuardadoNota
import com.upiicsa.pasoapaso.seguridad.DatabaseKeyProvider
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SeguridadDatabaseTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferenciasSeguras
    private lateinit var keyProvider: DatabaseKeyProvider
    private val dbName = "test_paso_a_paso_cifrada.db"

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
        context.getSharedPreferences("paso_a_paso_seguras", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = PreferenciasSeguras(context)
        keyProvider = DatabaseKeyProvider(prefs)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun ps01_archivoBaseDatosEstaCifradoYNoEsLegibleEnTextoPlano() = runBlocking {
        val passphrase = keyProvider.inicializarConCodigo("1234")
        val textoSecreto = "Nota ultra privada de prueba SQLCipher 2026"

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()

        val repo = DiarioRepository(db.notaDao())
        val resultado = repo.guardarNota(
            nivelMalestar = 3,
            detonanteId = null,
            texto = textoSecreto
        )
        assertTrue(resultado is ResultadoGuardadoNota.GuardadaNormal)
        db.close()

        // Inspeccionar bytes crudos del archivo en disco
        val dbFile: File = context.getDatabasePath(dbName)
        assertTrue(dbFile.exists())
        val bytesCrudos = dbFile.readBytes()
        val contenidoCrudo = String(bytesCrudos, Charsets.ISO_8859_1)

        // Una SQLite sin cifrar siempre empieza con "SQLite format 3"
        assertFalse(
            "El archivo .db NO debe tener encabezado SQLite en claro",
            contenidoCrudo.startsWith("SQLite format 3")
        )
        assertFalse(
            "El texto de la nota NO debe aparecer legible en el archivo .db",
            contenidoCrudo.contains(textoSecreto)
        )
    }

    @Test
    fun ps04_cambiarCodigoConservaClaveMaestraYNotasIntactas() = runBlocking {
        val passphraseInicial = keyProvider.inicializarConCodigo("1234")

        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(passphraseInicial))
            .build()

        val repo1 = DiarioRepository(db1.notaDao())
        repo1.guardarNota(nivelMalestar = 4, detonanteId = null, texto = "Seguimiento antes de cambiar PIN")
        db1.close()

        // Cambiar código de 1234 a 9876 usando la misma clave maestra
        keyProvider.cambiarCodigo("9876", passphraseInicial)

        // El código anterior ya no debe abrir
        assertNull(keyProvider.desbloquearConCodigo("1234"))

        // El código nuevo debe devolver exactamente la misma clave maestra
        val passphraseNueva = keyProvider.desbloquearConCodigo("9876")
        assertNotNull(passphraseNueva)

        val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(passphraseNueva!!))
            .build()

        val notasGuardadas = db2.notaDao().observarTodas().first()
        assertEquals(1, notasGuardadas.size)
        assertEquals("Seguimiento antes de cambiar PIN", notasGuardadas.first().nota.texto)
        db2.close()
    }

    @Test
    fun detectarRiesgoAlGuardarNotaActivaAlertaCrisis() = runBlocking {
        val passphrase = keyProvider.inicializarConCodigo("2580")
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()

        val repo = DiarioRepository(db.notaDao())
        val resultado = repo.guardarNota(
            nivelMalestar = 5,
            detonanteId = null,
            texto = "Siento que ya no quiero vivir así"
        )

        assertTrue(resultado is ResultadoGuardadoNota.GuardadaConAlertaCrisis)
        db.close()
    }
}