package com.upiicsa.pasoapaso

import com.upiicsa.pasoapaso.seguridad.CrisisFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisFilterTest {

    @Test
    fun pr01_expresionDirecta_detectaRiesgo() {
        assertTrue(CrisisFilter.contieneRiesgo("Ya no quiero seguir, me quiero hacer daño"))
    }

    @Test
    fun pr02_expresionEnMayusculas_detectaRiesgo() {
        assertTrue(CrisisFilter.contieneRiesgo("ME QUIERO MORIR YA NO AGUANTO"))
    }

    @Test
    fun pr03_expresionSinAcentos_detectaRiesgo() {
        assertTrue(CrisisFilter.contieneRiesgo("Quiero hacerme dano y acabar con mi vida"))
    }

    @Test
    fun pr04_caracteresSeparadosOIntercalados_detectaRiesgo() {
        assertTrue(CrisisFilter.contieneRiesgo("Pienso en el s.u.i.c.i.d.i.o todos los dias"))
        assertTrue(CrisisFilter.contieneRiesgo("m e  q u i e r o  m o r i r"))
    }

    @Test
    fun pr05_textoNeutroOAnsiedadLeve_noDisparaRiesgo() {
        assertFalse(CrisisFilter.contieneRiesgo("Tengo mucha ansiedad por los examenes de la escuela"))
        assertFalse(CrisisFilter.contieneRiesgo("Hoy me senti con algo de tension pero voy a respirar"))
    }
}