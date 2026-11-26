package com.upiicsa.pasoapaso.diario

import com.upiicsa.pasoapaso.seguridad.CrisisFilter
import kotlinx.coroutines.flow.Flow

sealed interface ResultadoGuardadoNota {
    data class GuardadaNormal(val idNota: Long) : ResultadoGuardadoNota
    data class GuardadaConAlertaCrisis(val idNota: Long) : ResultadoGuardadoNota
}

class DiarioRepository(
    private val notaDao: NotaDao
) {
    fun observarNotas(): Flow<List<NotaConDetonante>> = notaDao.observarTodas()

    fun observarDetonantes(): Flow<List<DetonanteEntity>> = notaDao.observarDetonantes()

    suspend fun guardarNota(
        nivelMalestar: Int,
        detonanteId: Long?,
        texto: String,
        fechaHora: Long = System.currentTimeMillis()
    ): ResultadoGuardadoNota {
        val nota = NotaEmocionalEntity(
            fechaHora = fechaHora,
            nivelMalestar = nivelMalestar.coerceIn(1, 5),
            detonanteId = detonanteId,
            texto = texto.trim()
        )
        val idInsertado = notaDao.insertar(nota)

        return if (CrisisFilter.contieneRiesgo(texto)) {
            ResultadoGuardadoNota.GuardadaConAlertaCrisis(idInsertado)
        } else {
            ResultadoGuardadoNota.GuardadaNormal(idInsertado)
        }
    }
}