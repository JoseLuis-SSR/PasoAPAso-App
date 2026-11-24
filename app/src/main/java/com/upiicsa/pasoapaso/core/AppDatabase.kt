package com.upiicsa.pasoapaso.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.upiicsa.pasoapaso.ayuda.LineaAyudaDao
import com.upiicsa.pasoapaso.ayuda.LineaAyudaEntity
import com.upiicsa.pasoapaso.diario.DetonanteEntity
import com.upiicsa.pasoapaso.diario.NotaDao
import com.upiicsa.pasoapaso.diario.NotaEmocionalEntity
import com.upiicsa.pasoapaso.emergencia.ContactoDao
import com.upiicsa.pasoapaso.emergencia.ContactoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        DetonanteEntity::class,
        NotaEmocionalEntity::class,
        ContactoEntity::class,
        LineaAyudaEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notaDao(): NotaDao
    abstract fun contactoDao(): ContactoDao
    abstract fun lineaAyudaDao(): LineaAyudaDao

    companion object {
        private const val DB_NAME = "paso_a_paso_encrypted.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Abre la base de datos cifrada con SQLCipher usando la contraseña maestra
         * liberada por DatabaseKeyProvider (huella o código).
         */
        fun obtenerInstancia(context: Context, passphraseBytes: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: construirBaseDeDatos(context.applicationContext, passphraseBytes).also {
                    INSTANCE = it
                }
            }
        }

        private fun construirBaseDeDatos(
            appContext: Context,
            passphraseBytes: ByteArray
        ): AppDatabase {
            // Carga nativa de SQLCipher requerida por sqlcipher-android 4.9.x
            System.loadLibrary("sqlcipher")

            val factory = SupportOpenHelperFactory(passphraseBytes)

            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { database ->
                                precargarDatosBase(database)
                            }
                        }
                    }
                })
                .build()
        }

        private suspend fun precargarDatosBase(db: AppDatabase) {
            // 1. Detonantes del sistema (Sección 7.3)
            val detonantesIniciales = listOf(
                DetonanteEntity(etiqueta = "Escuela", esDelSistema = true),
                DetonanteEntity(etiqueta = "Familia", esDelSistema = true),
                DetonanteEntity(etiqueta = "Estrés", esDelSistema = true),
                DetonanteEntity(etiqueta = "Soledad", esDelSistema = true),
                DetonanteEntity(etiqueta = "Ganas de consumir", esDelSistema = true)
            )
            db.notaDao().insertarDetonantes(detonantesIniciales)

            // 2. Líneas oficiales precargadas en el APK (HU-04, PO-01)
            val ahora = System.currentTimeMillis()
            val lineasBase = listOf(
                LineaAyudaEntity(
                    id = "01_emergencias_911",
                    nombre = "Emergencias 911",
                    institucion = "Sistema Nacional de Atención de Llamadas de Emergencia",
                    telefono = "911",
                    horario = "24 horas, 365 días",
                    region = "MX",
                    verificadaEn = ahora
                ),
                LineaAyudaEntity(
                    id = "02_linea_de_la_vida",
                    nombre = "Línea de la Vida",
                    institucion = "CONASAMA · Secretaría de Salud",
                    telefono = "8009112000",
                    horario = "Gratuita, confidencial · 24 horas, 365 días",
                    region = "MX",
                    verificadaEn = ahora
                )
            )
            db.lineaAyudaDao().upsertTodas(lineasBase)
        }

        fun cerrarInstancia() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}