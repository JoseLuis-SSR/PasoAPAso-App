package com.upiicsa.pasoapaso.seguridad

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    private const val AUTENTICADORES_FUERTES = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /**
     * Devuelve true solo si el dispositivo cuenta con sensor biométrico fuerte
     * y al menos una huella registrada en Android (HU-02, CU-01 A2).
     */
    fun puedeUsarHuella(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTENTICADORES_FUERTES) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Lanza el diálogo nativo de huella digital.
     * Si el modo anónimo está activo, el título dice "Mis Notas" sin revelar "Paso a Paso".
     */
    fun autenticarConHuella(
        activity: FragmentActivity,
        modoAnonimo: Boolean,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onExito: (BiometricPrompt.AuthenticationResult) -> Unit,
        onUsarCodigo: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onExito(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onUsarCodigo()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Android maneja los reintentos automáticamente en el diálogo del sistema
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (modoAnonimo) "Mis Notas" else "Paso a Paso")
            .setSubtitle("Confirma tu identidad para abrir tu espacio")
            .setNegativeButtonText("Usar código")
            .setAllowedAuthenticators(AUTENTICADORES_FUERTES)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    /**
     * Exige huella o PIN/patrón del teléfono antes de restablecer un código olvidado (PS-04).
     */
    fun autenticarParaRestablecerCodigo(
        activity: FragmentActivity,
        onAutorizado: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val autenticadoresRestablecimiento =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAutorizado()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificar identidad")
            .setSubtitle("Usa la huella o el bloqueo de tu teléfono para crear un código nuevo")
            .setAllowedAuthenticators(autenticadoresRestablecimiento)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}