package com.upiicsa.pasoapaso.diario

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.upiicsa.pasoapaso.MainActivity
import com.upiicsa.pasoapaso.R
import com.upiicsa.pasoapaso.seguridad.PreferenciasSeguras
import java.util.concurrent.TimeUnit

class NotificacionDiscretaWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferenciasSeguras(applicationContext)
        if (!prefs.registroCompletado) return Result.success()

        mostrarNotificacionNeutra(prefs.modoAnonimoActivo)
        return Result.success()
    }

    private fun mostrarNotificacionNeutra(modoAnonimo: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        crearCanalSiNoExiste()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Texto 100 % neutro para proteger privacidad en pantalla bloqueada (HU-02, HU-07)
        val titulo = if (modoAnonimo) "Mis Notas" else "Paso a Paso"
        val texto = "Tienes un recordatorio pendiente en tus notas."

        val notificacion = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notificacion)
    }

    private fun crearCanalSiNoExiste() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val canal = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios generales",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avisos discretos de actividad"
                }
                manager.createNotificationChannel(canal)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "canal_recordatorios_discretos"
        private const val NOTIFICATION_ID = 701
        private const val WORK_NAME = "recordatorio_discreto_diario"

        fun programar(context: Context) {
            val solicitud = PeriodicWorkRequestBuilder<NotificacionDiscretaWorker>(
                24,
                TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                solicitud
            )
        }
    }
}