package com.upiicsa.pasoapaso.emergencia

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacto_confianza")
data class ContactoEntity(
    @PrimaryKey
    val id: Long = 1L, // Un solo registro de contacto de confianza en el MVP
    val alias: String,
    val telefono: String,
    val mensaje: String
)