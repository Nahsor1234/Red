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
import com.example.jeecommandcenter.R
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

data class ReminderRule(val enabled: Boolean, val hour: Int, val minute: Int)
data class QuietHours(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int)

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

    fun isMasterEnabled(): Boolean = prefs.getBoolean("master_enabled", true)
    fun setMasterEnabled(enabled: Boolean) { prefs.edit().putBoolean("master_enabled", enabled).apply() }

    fun getQuietHours(): QuietHours = QuietHours(
        startHour = prefs.getInt("quiet_start_hour", 22).coerceIn(0, 23),
        startMinute = prefs.getInt("quiet_start_minute", 0).coerceIn(0, 59),
        endHour = prefs.getInt("quiet_end_hour", 7).coerceIn(0, 23),
        endMinute = prefs.getInt("quiet_end_minute", 0).coerceIn(0, 59)
    )

    fun setQuietHours(hours: QuietHours) {
        prefs.edit()
            .putInt("quiet_start_hour", hours.startHour.coerceIn(0, 23))
            .putInt("quiet_start_minute", hours.startMinute.coerceIn(0, 59))
            .putInt("quiet_end_hour", hours.endHour.coerceIn(0, 23))
            .putInt("quiet_end_minute", hours.endMinute.coerceIn(0, 59))
            .apply()
    }

    fun isQuietNow(now: LocalTime = LocalTime.now()): Boolean = isQuietAt(now)

    fun isQuietAt(time: LocalTime): Boolean {
        val hours = getQuietHours()
        val start = LocalTime.of(hours.startHour, hours.startMinute)
        val end = LocalTime.of(hours.endHour, hours.endMinute)
        if (start == end) return false
        return if (start.isBefore(end)) !time.isBefore(start) && time.isBefore(end)
        else !time.isBefore(start) || time.isBefore(end)
    }

    fun getLastTriggered(type: String): Long = prefs.getLong("last_$type", 0L)
    fun recordTriggered(type: String, timeMillis: Long) { prefs.edit().putLong("last_$type", timeMillis).apply() }

    companion object {
        const val PREFS = "reminder_settings_v1"
        const val TYPE_DAILY_STUDY = "daily_study"
        const val TYPE_DAILY_LOG = "daily_log"
    }
}

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val settings = ReminderSettingsRepository(appContext)
    private val customReminders = CustomReminderRepository(appContext)

    fun scheduleAll() {
        schedule(ReminderSettingsRepository.TYPE_DAILY_STUDY)
        schedule(ReminderSettingsRepository.TYPE_DAILY_LOG)
        customReminders.all().forEach { scheduleCustom(it.id) }
    }

    fun schedule(type: String) {
        if (type != ReminderSettingsRepository.TYPE_DAILY_STUDY &&
            type != ReminderSettingsRepository.TYPE_DAILY_LOG
        ) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(type)
        val rule = settings.getRule(type)
        if (!settings.isMasterEnabled() || !rule.enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val configuredTime = LocalTime.of(rule.hour, rule.minute)
        val trigger = Calendar.getInstance().apply {
            if (settings.isQuietAt(configuredTime)) {
                val quiet = settings.getQuietHours()
                set(Calendar.HOUR_OF_DAY, quiet.endHour)
                set(Calendar.MINUTE, quiet.endMinute)
            } else {
                set(Calendar.HOUR_OF_DAY, rule.hour)
                set(Calendar.MINUTE, rule.minute)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
    }

    fun scheduleCustom(id: String) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = customPendingIntent(id)
        val reminder = customReminders.find(id)
        if (!settings.isMasterEnabled() || reminder == null || !reminder.enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val configuredTime = LocalTime.of(reminder.hour, reminder.minute)
        val quiet = settings.getQuietHours()
        val useQuietEnd = settings.isQuietAt(configuredTime)
        val scheduledDate = if (reminder.repeat == CustomReminder.REPEAT_ONCE) {
            reminder.scheduledDate?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        } else null
        if (reminder.repeat == CustomReminder.REPEAT_ONCE && !reminder.scheduledDate.isNullOrBlank() && scheduledDate == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val trigger = Calendar.getInstance().apply {
            if (scheduledDate != null) {
                set(Calendar.YEAR, scheduledDate.year)
                set(Calendar.MONTH, scheduledDate.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, scheduledDate.dayOfMonth)
                if (useQuietEnd) {
                    val quietEnd = LocalTime.of(quiet.endHour, quiet.endMinute)
                    if (!quietEnd.isAfter(configuredTime)) add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, quiet.endHour)
                    set(Calendar.MINUTE, quiet.endMinute)
                } else {
                    set(Calendar.HOUR_OF_DAY, reminder.hour)
                    set(Calendar.MINUTE, reminder.minute)
                }
            } else {
                if (useQuietEnd) {
                    set(Calendar.HOUR_OF_DAY, quiet.endHour)
                    set(Calendar.MINUTE, quiet.endMinute)
                } else {
                    set(Calendar.HOUR_OF_DAY, reminder.hour)
                    set(Calendar.MINUTE, reminder.minute)
                }
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (scheduledDate == null && timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            if (scheduledDate == null && reminder.repeat == CustomReminder.REPEAT_WEEKDAYS) {
                while (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }.timeInMillis

        // A date-specific reminder never silently rolls over to another date if its time has passed.
        if (scheduledDate != null && trigger <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
    }

    fun cancelCustom(id: String) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(customPendingIntent(id))
    }

    private fun reminderPendingIntent(type: String): PendingIntent {
        val intent = Intent(appContext, DailyReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_REMINDER_TYPE, type)
        return PendingIntent.getBroadcast(
            appContext, requestCode(type), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun customPendingIntent(id: String): PendingIntent {
        val intent = Intent(appContext, DailyReminderReceiver::class.java)
            .setAction(ACTION_CUSTOM_REMINDER)
            .putExtra(EXTRA_CUSTOM_REMINDER_ID, id)
        return PendingIntent.getBroadcast(
            appContext, customRequestCode(id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REMINDER = "com.sigmaje.app.ACTION_DAILY_REMINDER"
        const val ACTION_CUSTOM_REMINDER = "com.sigmaje.app.ACTION_CUSTOM_REMINDER"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_CUSTOM_REMINDER_ID = "custom_reminder_id"
        fun requestCode(type: String): Int = if (type == ReminderSettingsRepository.TYPE_DAILY_STUDY) 7411 else 7412
        fun customRequestCode(id: String): Int = 20000 + (id.hashCode() and 0x3fffffff)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val settings = ReminderSettingsRepository(context)
        if (!settings.isMasterEnabled()) return

        val customId = intent?.getStringExtra(ReminderScheduler.EXTRA_CUSTOM_REMINDER_ID)
        if (!customId.isNullOrBlank()) {
            val repository = CustomReminderRepository(context)
            val reminder = repository.find(customId) ?: return
            if (!reminder.enabled) return
            if (settings.isQuietNow()) {
                ReminderScheduler(context).scheduleCustom(customId)
                return
            }
            val delivered = ReminderNotificationPublisher.showCustom(context, reminder)
            if (delivered) repository.markTriggered(customId, System.currentTimeMillis())
            if (reminder.repeat == CustomReminder.REPEAT_ONCE && delivered) {
                ReminderScheduler(context).cancelCustom(customId)
            } else {
                // If Android blocks the notification, keep the one-time reminder queued.
                ReminderScheduler(context).scheduleCustom(customId)
            }
            return
        }

        val type = intent?.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE) ?: return
        if (type != ReminderSettingsRepository.TYPE_DAILY_STUDY &&
            type != ReminderSettingsRepository.TYPE_DAILY_LOG
        ) return
        if (!settings.getRule(type).enabled) return
        if (settings.isQuietNow()) {
            ReminderScheduler(context).schedule(type)
            return
        }
        if (ReminderNotificationPublisher.show(context, type)) {
            settings.recordTriggered(type, System.currentTimeMillis())
        }
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
    private const val CHANNEL_ID = "sigma_jee_reminders_v2"

    fun show(context: Context, type: String): Boolean {
        val isProgress = type == ReminderSettingsRepository.TYPE_DAILY_LOG
        val title = if (isProgress) "Close today's study loop" else "Your JEE session is waiting"
        val body = if (isProgress) {
            "Mark today's progress and jot down what you covered."
        } else {
            "Pick up your next topic and make progress in one focused study block."
        }
        val target = if (isProgress) "daily_progress" else "timer"
        return post(context, notificationId(type), title, body, target, if (isProgress) "Open progress" else "Start session")
    }

    fun showCustom(context: Context, reminder: CustomReminder): Boolean {
        val title = reminder.title.ifBlank { "Sigma JEE reminder" }
        val body = reminder.message.ifBlank { "Your scheduled reminder is ready." }
        return post(context, notificationId(reminder.id), title, body, "home", "Open Sigma JEE")
    }

    private fun post(context: Context, notificationId: Int, title: String, body: String, target: String, actionTitle: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        createChannel(context)
        val openIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TARGET, target)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            context, 30000 + (notificationId and 0x3fffffff), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val actionIntent = PendingIntent.getActivity(
            context, 50000 + (notificationId and 0x3fffffff), Intent(openIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sigma)
            .setColor(android.graphics.Color.parseColor("#C2E58D"))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_notification_sigma, actionTitle, actionIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun notificationId(key: String): Int = 90000 + (key.hashCode() and 0x3fffffff)

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Sigma JEE reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Personal JEE study and progress reminders"
                }
            )
        }
    }
}
