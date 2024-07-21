package io.github.sgpublic.callpilot

import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

class Application: Application() {
    override fun onCreate() {
        super.onCreate()

        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder("default_channel", NotificationManager.IMPORTANCE_DEFAULT)
                .setName(getString(R.string.notify_channel))
                .build()
        )
    }
}