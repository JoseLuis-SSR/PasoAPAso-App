package com.upiicsa.pasoapaso.seguridad

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DatabaseKeyProvider(
    private val preferenciasSeguras: PreferenciasSeguras
) {
    private val prefs: SharedPreferences = preferenciasSeguras.prefs

    // Contraseña maestra en memoria únicamente mientras dure la sesión desbloqueada
    private var passphraseEnSesion: ByteArray? = null

    /**
     * Paso 1 del registro inicial (CU-01):
     * Genera 32 bytes aleatorios con SecureRandom y guarda la Copia B (envuelta con el código).
     * Devuelve la contraseña en claro en memoria para abrir Room por primera vez.
     */
    fun inicializarConCodigo(codigo: String): ByteArray {
        val nuevaPassphrase = ByteArray(32).apply {
            SecureRandom().nextBytes(this)
        }
        guardarCopiaConCodigo(nuevaPassphrase, codigo)
        passphraseEnSesion = nuevaPassphrase.copyOf()
        return nuevaPassphrase
    }

    /**
     * Paso 2 del registro (o al activar huella en Ajustes / tras KeyPermanentlyInvalidatedException):
     * Crea o regenera la llave en Android Keystore que exige autenticación biométrica fuerte.
     */
    fun prepararKeystoreParaHuella(): Cipher {
        val secretKey = crearOObtenerLlaveKeystore()
        return Cipher.getInstance(TRANSFORMATION_AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
    }

    /**
     * Se invoca tras autenticar con BiometricPrompt en modo ENCRYPT para envolver
     * la Copia A de la contraseña maestra de SQLCipher.
     */
    fun completarEnvolturaHuella(cipherAutenticado: Cipher, passphraseActual: ByteArray) {
        val ciphertext = cipherAutenticado.doFinal(passphraseActual)
        val iv = cipherAutenticado.iv
        prefs.edit()
            .putString(KEY_COPIA_HUELLA_DATA, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_COPIA_HUELLA_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        preferenciasSeguras.huellaActiva = true
    }

    /**
     * Prepara el Cipher de descifrado antes de lanzar BiometricPrompt al abrir la app.
     * Lanza KeyPermanentlyInvalidatedException si el usuario borró todas sus huellas (PS-10).
     */
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun obtenerCipherDescifradoHuella(): Cipher? {
        val ivBase64 = prefs.getString(KEY_COPIA_HUELLA_IV, null) ?: return null
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val secretKey = obtenerLlaveKeystoreExistente() ?: return null

        return try {
            Cipher.getInstance(TRANSFORMATION_AES_GCM).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Limpiamos la copia inválida para que se regenere al entrar con el código
            invalidarCopiaHuella()
            throw e
        }
    }

    /**
     * Libera los 32 bytes de SQLCipher después de que BiometricPrompt confirmó la huella.
     */
    fun desbloquearConCipherHuella(cipherAutenticado: Cipher): ByteArray? {
        val dataBase64 = prefs.getString(KEY_COPIA_HUELLA_DATA, null) ?: return null
        return try {
            val ciphertext = Base64.decode(dataBase64, Base64.NO_WRAP)
            val passphrase = cipherAutenticado.doFinal(ciphertext)
            passphraseEnSesion = passphrase.copyOf()
            passphrase
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Libera los 32 bytes de SQLCipher verificando el código de 4 a 6 dígitos (Copia B).
     * Devuelve null si el código es incorrecto (PS-02).
     */
    fun desbloquearConCodigo(codigo: String): ByteArray? {
        val dataBase64 = prefs.getString(KEY_COPIA_CODIGO_DATA, null) ?: return null
        val ivBase64 = prefs.getString(KEY_COPIA_CODIGO_IV, null) ?: return null
        val saltBase64 = prefs.getString(KEY_COPIA_CODIGO_SALT, null) ?: return null

        return try {
            val ciphertext = Base64.decode(dataBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

            val llaveDerivada = derivarLlaveDesdeCodigo(codigo, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM).apply {
                init(Cipher.DECRYPT_MODE, llaveDerivada, GCMParameterSpec(128, iv))
            }
            val passphrase = cipher.doFinal(ciphertext)
            passphraseEnSesion = passphrase.copyOf()
            passphrase
        } catch (e: Exception) {
            // AEADBadTagException ocurre si el código es incorrecto -> acceso denegado
            null
        }
    }

    /**
     * Permite crear un código nuevo tras verificar huella o credencial del dispositivo (PS-04),
     * re-cifrando la Copia B sin perder ninguna entrada de Room.
     */
    fun cambiarCodigo(nuevoCodigo: String, passphraseActual: ByteArray) {
        guardarCopiaConCodigo(passphraseActual, nuevoCodigo)
        passphraseEnSesion = passphraseActual.copyOf()
    }

    fun obtenerPassphraseActiva(): ByteArray? = passphraseEnSesion?.copyOf()

    fun cerrarSesion() {
        passphraseEnSesion?.fill(0)
        passphraseEnSesion = null
    }

    fun invalidarCopiaHuella() {
        prefs.edit()
            .remove(KEY_COPIA_HUELLA_DATA)
            .remove(KEY_COPIA_HUELLA_IV)
            .apply()
        preferenciasSeguras.huellaActiva = false
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
        }
    }

    private fun guardarCopiaConCodigo(passphrase: ByteArray, codigo: String) {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val llaveDerivada = derivarLlaveDesdeCodigo(codigo, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, llaveDerivada)
        }
        val ciphertext = cipher.doFinal(passphrase)
        val iv = cipher.iv

        prefs.edit()
            .putString(KEY_COPIA_CODIGO_DATA, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_COPIA_CODIGO_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_COPIA_CODIGO_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    private fun derivarLlaveDesdeCodigo(codigo: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(codigo.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, KeyProperties.KEY_ALGORITHM_AES)
    }

    private fun obtenerLlaveKeystoreExistente(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
    }

    private fun crearOObtenerLlaveKeystore(): SecretKey {
        obtenerLlaveKeystoreExistente()?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(false)

        // Bifurcación por versión de API documentada en la sección 7.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "paso_a_paso_biometric_master_key"
        private const val TRANSFORMATION_AES_GCM = "AES/GCM/NoPadding"
        private const val PBKDF2_ITERATIONS = 100_000

        private const val KEY_COPIA_CODIGO_DATA = "db_key_pin_data"
        private const val KEY_COPIA_CODIGO_IV = "db_key_pin_iv"
        private const val KEY_COPIA_CODIGO_SALT = "db_key_pin_salt"

        private const val KEY_COPIA_HUELLA_DATA = "db_key_bio_data"
        private const val KEY_COPIA_HUELLA_IV = "db_key_bio_iv"
    }
}