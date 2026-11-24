package com.upiicsa.pasoapaso.seguridad

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenciasSeguras(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey: MasterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        "paso_a_paso_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var registroCompletado: Boolean
        get() = prefs.getBoolean(KEY_REGISTRO_COMPLETADO, false)
        set(value) = prefs.edit().putBoolean(KEY_REGISTRO_COMPLETADO, value).apply()

    var pistaCodigo: String?
        get() = prefs.getString(KEY_PISTA_CODIGO, null)
        set(value) = prefs.edit().putString(KEY_PISTA_CODIGO, value?.takeIf { it.isNotBlank() }).apply()

    var huellaActiva: Boolean
        get() = prefs.getBoolean(KEY_HUELLA_ACTIVA, false)
        set(value) = prefs.edit().putBoolean(KEY_HUELLA_ACTIVA, value).apply()

    var tratamiento: Tratamiento
        get() {
            val nombre = prefs.getString(KEY_TRATAMIENTO, Tratamiento.NEUTRO.name)
            return runCatching { Tratamiento.valueOf(nombre ?: Tratamiento.NEUTRO.name) }
                .getOrDefault(Tratamiento.NEUTRO)
        }
        set(value) = prefs.edit().putString(KEY_TRATAMIENTO, value.name).apply()

    var ocultarDetallesInicio: Boolean
        get() = prefs.getBoolean(KEY_OCULTAR_DETALLES, false)
        set(value) = prefs.edit().putBoolean(KEY_OCULTAR_DETALLES, value).apply()

    val modoAnonimoActivo: Boolean
        get() = prefs.getBoolean(KEY_MODO_ANONIMO, false)

    /**
     * Activa o desactiva el disfraz "Mis Notas" alternando los activity-alias del Manifiesto (HU-02).
     */
    fun setModoAnonimo(activar: Boolean) {
        val estadoAnterior = modoAnonimoActivo
        prefs.edit().putBoolean(KEY_MODO_ANONIMO, activar).apply()

        // Si el estado no cambió (ej. se dejó "Dejarla igual"), no tocamos el PackageManager
        if (estadoAnterior == activar) return

        val pm = appContext.packageManager
        val aliasNormal = ComponentName(appContext, "com.upiicsa.pasoapaso.AliasPasoAPaso")
        val aliasAnonimo = ComponentName(appContext, "com.upiicsa.pasoapaso.AliasMisNotas")

        if (activar) {
            pm.setComponentEnabledSetting(
                aliasAnonimo,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                aliasNormal,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                aliasNormal,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                aliasAnonimo,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    companion object {
        private const val KEY_REGISTRO_COMPLETADO = "registro_completado"
        private const val KEY_PISTA_CODIGO = "pista_codigo"
        private const val KEY_HUELLA_ACTIVA = "huella_activa"
        private const val KEY_TRATAMIENTO = "tratamiento"
        private const val KEY_OCULTAR_DETALLES = "ocultar_detalles"
        private const val KEY_MODO_ANONIMO = "modo_anonimo"
    }
}