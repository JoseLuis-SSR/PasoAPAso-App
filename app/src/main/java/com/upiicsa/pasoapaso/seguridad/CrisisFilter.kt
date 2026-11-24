package com.upiicsa.pasoapaso.seguridad

import java.text.Normalizer

object CrisisFilter {

    /**
     * Evalúa si el texto contiene indicadores de riesgo de suicidio o autolesión.
     * Calibrado hacia el falso positivo: prioriza mostrar la ruta de ayuda (Línea de la Vida,
     * 911 y contacto de confianza) antes que omitir una señal real (HU-07, Sección 6).
     */
    fun contieneRiesgo(texto: String): Boolean {
        if (texto.isBlank()) return false

        val normalizadoConEspacios = normalizarManteniendoPalabras(texto)
        val compactoSinSeparadores = normalizarCompacto(texto)

        return PATRONES_PALABRA.any { regex ->
            regex.containsMatchIn(normalizadoConEspacios)
        } || PATRONES_COMPACTOS.any { patron ->
            compactoSinSeparadores.contains(patron)
        }
    }

    /**
     * Convierte a minúsculas, elimina acentos/diéresis y reemplaza signos de puntuación
     * por espacios para detectar frases completas sin falsos cortes.
     */
    fun normalizarManteniendoPalabras(texto: String): String {
        val sinAcentos = quitarAcentos(texto.lowercase())
        return sinAcentos
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Colapsa todos los espacios y separadores intercalados (ej. "s.u.i.c.i.d.i.o",
     * "m e  q u i e r o  m o r i r") para cubrir el escenario de prueba PR-04.
     */
    fun normalizarCompacto(texto: String): String {
        val sinAcentos = quitarAcentos(texto.lowercase())
        return sinAcentos.replace(Regex("[^a-z0-9]"), "")
    }

    private fun quitarAcentos(input: String): String {
        val descompuesto = Normalizer.normalize(input, Normalizer.Form.NFD)
        return descompuesto.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private val PATRONES_PALABRA = listOf(
        Regex("\\bsuicid\\w*\\b"),
        Regex("\\bquitarme la vida\\b"),
        Regex("\\bacabar con mi vida\\b"),
        Regex("\\bterminar con mi vida\\b"),
        Regex("\\bme quiero morir\\b"),
        Regex("\\bquiero morir\\b"),
        Regex("\\bno quiero vivir\\b"),
        Regex("\\bya no quiero seguir\\b"),
        Regex("\\bno vale la pena vivir\\b"),
        Regex("\\bautolesion\\w*\\b"),
        Regex("\\bhacerme dano\\b"),
        Regex("\\bme quiero hacer dano\\b"),
        Regex("\\bcortarme\\b"),
        Regex("\\bme corto\\b"),
        Regex("\\bmatarme\\b"),
        Regex("\\bme voy a matar\\b"),
        Regex("\\bsobredosis\\b"),
        Regex("\\bahorcarme\\b"),
        Regex("\\baventarme\\b"),
        Regex("\\btirarme del\\b")
    )

    private val PATRONES_COMPACTOS = listOf(
        "suicidio",
        "suicidarme",
        "quitarmelavida",
        "acabarconmivida",
        "terminarconmivida",
        "mequieromorir",
        "noquierovivir",
        "yanoquieroseguir",
        "novalelapenavivir",
        "autolesion",
        "hacermedano",
        "mequierohacerdano",
        "cortarme",
        "matarme",
        "mevoyamatar",
        "sobredosis"
    )
}