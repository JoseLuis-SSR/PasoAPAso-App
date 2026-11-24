package com.upiicsa.pasoapaso.ayuda

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LineaAyudaDao {

    @Query("SELECT * FROM lineas_ayuda WHERE region = :region OR region = 'MX' ORDER BY id ASC")
    fun observarPorRegion(region: String): Flow<List<LineaAyudaEntity>>

    @Upsert
    suspend fun upsertTodas(lineas: List<LineaAyudaEntity>)
}