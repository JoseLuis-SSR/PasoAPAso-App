package com.upiicsa.pasoapaso.diario

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class NotaConDetonante(
    @Embedded val nota: NotaEmocionalEntity,
    @Relation(
        parentColumn = "detonanteId",
        entityColumn = "id"
    )
    val detonante: DetonanteEntity?
)

@Dao
interface NotaDao {

    @Transaction
    @Query("SELECT * FROM notas_emocionales ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<NotaConDetonante>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(nota: NotaEmocionalEntity): Long

    @Query("SELECT * FROM detonantes ORDER BY esDelSistema DESC, id ASC")
    fun observarDetonantes(): Flow<List<DetonanteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarDetonantes(detonantes: List<DetonanteEntity>)

    /**
     * Cuenta entradas con malestar >= nivel desde una marca de tiempo dada (últimos 7 días),
     * agrupadas o filtradas por franja horaria para PatronesAnalyzer (HU-07).
     */
    @Query(
        """
        SELECT COUNT(*) FROM notas_emocionales 
        WHERE fechaHora >= :desde AND nivelMalestar >= :nivel
        """
    )
    suspend fun contarAltasDesde(desde: Long, nivel: Int = 4): Int

    @Query(
        """
        SELECT fechaHora FROM notas_emocionales 
        WHERE fechaHora >= :desde AND nivelMalestar >= :nivel
        ORDER BY fechaHora DESC
        """
    )
    suspend fun obtenerFechasMalestarAlto(desde: Long, nivel: Int = 4): List<Long>
}