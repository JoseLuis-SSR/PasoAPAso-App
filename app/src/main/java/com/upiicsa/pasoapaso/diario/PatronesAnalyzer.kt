package com.upiicsa.pasoapaso.diario

import com.upiicsa.pasoapaso.seguridad.Tratamiento
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class FranjaHoraria(val etiqueta: String) {
    MADRUGADA("en la madrugada"),
    MANANA("por la mañana"),
    TARDE("por la tarde"),
    NOCHE("por la noche");

    companion object {
        fun desdeHora(hora: Int): FranjaHoraria = when (hora) {
            in 0..5 -> MADRUGADA
            in 6..11 -> MANANA
            in 12..18 -> TARDE
            else -> NOCHE
        }
    }
}

data class PatronDetectado(
    val franja: FranjaHoraria,
    val totalRegistrosAltos: Int,
    val mensajePreventivo: String
)

class PatronesAnalyzer(
    private val notaDao: NotaDao
) {
    /**
     * Evalúa los últimos 7 días en busca de repetición de malestar alto (>= 4).
     * Devuelve el patrón dominante si hay al menos 2 registros altos en la misma franja.
     */
    suspend fun analizarUltimosSieteDias(
        tratamiento: Tratamiento,
        ahoraMillis: Long = System.currentTimeMillis()
    ): PatronDetectado? {
        val haceSieteDias = ahoraMillis - TimeUnit.DAYS.toMillis(7)
        val fechasAltas = notaDao.obtenerFechasMalestarAlto(desde = haceSieteDias, nivel = 4)

        if (fechasAltas.size < 2) return null

        val conteoPorFranja = fechasAltas
            .map { timestamp ->
                val hora = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
                ).hour
                FranjaHoraria.desdeHora(hora)
            }
            .groupingBy { it }
            .eachCount()

        val dominante = conteoPorFranja.maxByOrNull { it.value } ?: return null
        if (dominante.value < 2) return null

        val mensaje = construirMensaje(dominante.key, tratamiento)
        return PatronDetectado(
            franja = dominante.key,
            totalRegistrosAltos = dominante.value,
            mensajePreventivo = mensaje
        )
    }

    private fun construirMensaje(franja: FranjaHoraria, tratamiento: Tratamiento): String {
        val acompanado = when (tratamiento) {
            Tratamiento.MASCULINO -> "acompañado"
            Tratamiento.FEMENINO -> "acompañada"
            Tratamiento.NEUTRO -> "con compañía"
        }
        return "Notamos que últimamente te has sentido con más carga ${franja.etiqueta}. " +
                "Recuerda que puedes hacer una pausa para respirar o escribir cómo te sientes; sigues $acompanado."
    }
}