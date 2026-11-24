package com.upiicsa.pasoapaso.emergencia

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Query("SELECT * FROM contacto_confianza WHERE id = 1 LIMIT 1")
    fun obtener(): Flow<ContactoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(contacto: ContactoEntity)

    @Query("DELETE FROM contacto_confianza")
    suspend fun eliminar()
}