package com.securenotes.app.core.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.securenotes.app.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val context: Context,
) {
    fun schedule(noteId: String, title: String, atMillis: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }
        }
    }

    fun cancel(noteId: String) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            noteId.hashCode(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    companion object {
        const val CHANNEL_ID = "securenotes_reminders"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService<NotificationManager>() ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Reminders you set on your notes" }
            manager.createNotificationChannel(channel)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Note reminder"
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return

        ReminderScheduler.ensureChannel(context)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("⏰ $title")
            .setContentText("Tap to open your note in SecureNotes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching {
            androidx.core.app.NotificationManagerCompat.from(context)
                .notify(noteId.hashCode(), notification)
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_TITLE = "note_title"
    }
}
