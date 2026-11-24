package com.upiicsa.pasoapaso.diario

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notas_emocionales",
    foreignKeys = [
        ForeignKey(
            entity = DetonanteEntity::class,
            parentColumns = ["id"],
            childColumns = ["detonanteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["detonanteId"]), Index(value = ["fechaHora"])]
)
data class NotaEmocionalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fechaHora: Long,
    val nivelMalestar: Int, // 1 a 5 (5 = Muy mal)
    val detonanteId: Long? = null,
    val texto: String
)