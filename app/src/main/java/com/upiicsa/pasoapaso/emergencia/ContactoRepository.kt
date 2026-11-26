package com.upiicsa.pasoapaso.emergencia

import kotlinx.coroutines.flow.Flow

class ContactoRepository(
    private val contactoDao: ContactoDao
) {
    fun observarContacto(): Flow<ContactoEntity?> = contactoDao.obtener()

    suspend fun guardarContacto(
        alias: String,
        telefono: String,
        mensajePersonalizado: String?
    ) {
        val mensajeFinal = mensajePersonalizado
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: MENSAJE_PREDETERMINADO

        val entidad = ContactoEntity(
            id = 1L,
            alias = alias.trim(),
            telefono = telefono.trim(),
            mensaje = mensajeFinal
        )
        contactoDao.guardar(entidad)
    }

    suspend fun eliminarContacto() {
        contactoDao.eliminar()
    }

    companion object {
        const val MENSAJE_PREDETERMINADO =
            "Hola, estoy pasando por un momento difícil y necesito apoyo. ¿Podemos hablar?"
    }
}