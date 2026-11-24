package com.upiicsa.pasoapaso.ayuda

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lineas_ayuda")
data class LineaAyudaEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val institucion: String,
    val telefono: String,
    val horario: String,
    val region: String = "MX",
    val verificadaEn: Long
)