package com.upiicsa.pasoapaso

import com.upiicsa.pasoapaso.diario.DetonanteEntity
import com.upiicsa.pasoapaso.diario.FranjaHoraria
import com.upiicsa.pasoapaso.diario.NotaConDetonante
import com.upiicsa.pasoapaso.diario.NotaDao
import com.upiicsa.pasoapaso.diario.NotaEmocionalEntity
import com.upiicsa.pasoapaso.diario.PatronesAnalyzer
import com.upiicsa.pasoapaso.seguridad.Tratamiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PatronesAnalyzerTest {

    private class FakeNotaDao(private val fechasAltas: List<Long>) : NotaDao {
        override suspend fun insertar(nota: NotaEmocionalEntity): Long = 1L
        override fun observarTodas(): Flow<List<NotaConDetonante>> = emptyFlow()
        override fun observarDetonantes(): Flow<List<DetonanteEntity>> = emptyFlow()
        override suspend fun insertarDetonantes(detonantes: List<DetonanteEntity>) {}
        override suspend fun obtenerFechasMalestarAlto(desde: Long, nivel: Int): List<Long> = fechasAltas
        override suspend fun contarAltasDesde(desde: Long, nivel: Int): Int = fechasAltas.size
    }

    @Test
    fun detectaPatronNocturnoCuandoHayDosRegistrosAltosEnLaNoche() = runBlocking {
        val zona = ZoneId.systemDefault()
        val dia1Noche = LocalDateTime.of(2026, 11, 24, 22, 15).atZone(zona).toInstant().toEpochMilli()
        val dia2Noche = LocalDateTime.of(2026, 11, 25, 21, 40).atZone(zona).toInstant().toEpochMilli()
        val ahora = LocalDateTime.of(2026, 11, 25, 23, 0).atZone(zona).toInstant().toEpochMilli()

        val analyzer = PatronesAnalyzer(FakeNotaDao(listOf(dia1Noche, dia2Noche)))
        val resultado = analyzer.analizarUltimosSieteDias(Tratamiento.FEMENINO, ahora)

        assertNotNull(resultado)
        assertEquals(FranjaHoraria.NOCHE, resultado!!.franja)
        assertEquals(2, resultado.totalRegistrosAltos)
        assertTrue(resultado.mensajePreventivo.contains("acompañada"))
    }

    @Test
    fun noDisparaPatronConUnSoloRegistroAlto() = runBlocking {
        val ahora = System.currentTimeMillis()
        val analyzer = PatronesAnalyzer(FakeNotaDao(listOf(ahora - 3_600_000L)))

        val resultado = analyzer.analizarUltimosSieteDias(Tratamiento.NEUTRO, ahora)
        assertNull(resultado)
    }
}