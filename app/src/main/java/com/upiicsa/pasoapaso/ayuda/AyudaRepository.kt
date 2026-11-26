package com.upiicsa.pasoapaso.ayuda

import kotlinx.coroutines.flow.Flow

class AyudaRepository(
    private val lineaAyudaDao: LineaAyudaDao
) {
    fun observarLineas(region: String = "MX"): Flow<List<LineaAyudaEntity>> {
        return lineaAyudaDao.observarPorRegion(region)
    }

    suspend fun actualizarCatalogo(nuevasLineas: List<LineaAyudaEntity>) {
        if (nuevasLineas.isNotEmpty()) {
            lineaAyudaDao.upsertTodas(nuevasLineas)
        }
    }
}