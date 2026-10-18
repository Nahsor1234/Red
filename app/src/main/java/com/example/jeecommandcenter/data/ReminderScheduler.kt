package com.example.jeecommandcenter.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.jeecommandcenter.MainActivity
import java.util.Calendar

data class ReminderRule(val enabled: Boolean, val hour: Int, val minute: Int)

class ReminderSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRule(type: String): ReminderRule {
        val defaultHour = if (type == TYPE_DAILY_STUDY) 18 else 21
        return ReminderRule(
            enabled = prefs.getBoolean("enabled_$type", false),
            hour = prefs.getInt("hour_$type", defaultHour).coerceIn(0, 23),
            minute = prefs.getInt("minute_$type", 0).coerceIn(0, 59)
        )
    }

    fun setRule(type: String, rule: ReminderRule) {
        prefs.edit()
            .putBoolean("enabled_$type", rule.enabled)
            .putInt("hour_$type", rule.hour.coerceIn(0, 23))
            .putInt("minute_$type", rule.minute.coerceIn(0, 59))
            .apply()
    }

    fun getLastTriggered(type: String): Long = prefs.getLong("last_$type", 0L)

    fun recordTriggered(type: String, timeMillis: Long) {
        prefs.edit().putLong("last_$type", timeMillis).apply()
    }

    companion object {
        const val PREFS = "reminder_settings_v1"
        const val TYPE_DAILY_STUDY = "daily_study"
        const val TYPE_DAILY_LOG = "daily_log"
    }
}

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val settings = ReminderSettingsRepository(appContext)

    fun scheduleAll() {
        schedule(ReminderSettingsRepository.TYPE_DAILY_STUDY)
        schedule(ReminderSettingsRepository.TYPE_DAILY_LOG)
    }

    fun schedule(type: String) {
        if (type != ReminderSettingsRepository.TYPE_DAILY_STUDY &&
            type != ReminderSettingsRepository.TYPE_DAILY_LOG
        ) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(type)
        val rule = settings.getRule(type)
        if (!rule.enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, rule.hour)
            set(Calendar.MINUTE, rule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        // These are user reminders, not time-critical alarms. Inexact idle-safe scheduling avoids
        // requesting Android's restricted exact-alarm permission.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
    }

    private fun reminderPendingIntent(type: String): PendingIntent {
        val intent = Intent(appContext, DailyReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_REMINDER_TYPE, type)
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REMINDER = "com.sigmaje.app.ACTION_DAILY_REMINDER"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        fun requestCode(type: String): Int =
            if (type == ReminderSettingsRepository.TYPE_DAILY_STUDY) 7411 else 7412
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val type = intent?.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE) ?: return
        if (type != ReminderSettingsRepository.TYPE_DAILY_STUDY &&
            type != ReminderSettingsRepository.TYPE_DAILY_LOG
        ) return

        val settings = ReminderSettingsRepository(context)
        if (!settings.getRule(type).enabled) return
        settings.recordTriggered(type, System.currentTimeMillis())
        ReminderNotificationPublisher.show(context, type)
        ReminderScheduler(context).schedule(type)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> ReminderScheduler(context).scheduleAll()
        }
    }
}

object ReminderNotificationPublisher {
    private const val CHANNEL_ID = "study_reminders"

    fun show(context: Context, type: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val isDailyLog = type == ReminderSettingsRepository.TYPE_DAILY_LOG
        val title = if (isDailyLog) "Log today's study" else "Time for your JEE study block"
        val body = if (isDailyLog) {
            "Mark today's Daily Log and note what you studied."
        } else {
            "Start a focused session and make progress on your next JEE topic."
        }
        createChannel(context)
        val target = if (isDailyLog) "daily_log" else "timer"
        val openIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TARGET, target)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context,
            if (isDailyLog) 7512 else 7511,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(if (isDailyLog) 7512 else 7511, notification)
        } catch (_: SecurityException) {
            // The notification permission can be revoked between the check and post.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Scheduled JEE study and Daily Log reminders"
                }
            )
        }
    }
}
