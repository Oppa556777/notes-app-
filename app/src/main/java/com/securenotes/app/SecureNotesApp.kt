package com.securenotes.app

import android.app.Application
import com.securenotes.app.core.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.ensureChannel(this)
    }
}
