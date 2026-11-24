package com.upiicsa.pasoapaso.diario

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detonantes")
data class DetonanteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val etiqueta: String,
    val esDelSistema: Boolean
)