package com.example.jeecommandcenter.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jeecommandcenter.data.ReminderRule
import com.example.jeecommandcenter.data.ReminderScheduler
import com.example.jeecommandcenter.data.ReminderSettingsRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationCenterScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { ReminderSettingsRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val study = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val dailyLog = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val recentStudy = remember(refresh) { settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val recentLog = remember(refresh) { settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Notifications", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JeeCard(featured = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = PrimaryLight, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Study reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (notificationsAllowed && appNotificationsEnabled) "Android notifications are available." else "System permission is needed to show reminders.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) { Text("Check Android notification settings") }
            }

            Text("SCHEDULED", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            ReminderSummaryCard(
                title = "Daily study reminder",
                subtitle = "A nudge to begin a focused JEE session",
                icon = Icons.Filled.Alarm,
                rule = study,
                lastTriggered = recentStudy,
                onRefresh = { refresh++ }
            )
            ReminderSummaryCard(
                title = "Daily Log reminder",
                subtitle = "Remember to complete today's checklist and notes",
                icon = Icons.Filled.Today,
                rule = dailyLog,
                lastTriggered = recentLog,
                onRefresh = { refresh++ }
            )

            Text("RECENT ACTIVITY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            JeeCard {
                RecentReminderRow("Daily study reminder", recentStudy)
                HorizontalDivider(color = BgDivider.copy(alpha = .6f))
                RecentReminderRow("Daily Log reminder", recentLog)
            }
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manage reminder settings")
            }
            Text("Reminders run locally on this device. No study notes are included in notification text.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ReminderSummaryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    rule: ReminderRule,
    lastTriggered: Long,
    onRefresh: () -> Unit
) {
    JeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = PrimarySoft) {
                Icon(icon, null, tint = PrimaryLight, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                Text(if (rule.enabled) "ON · ${formatReminderTime(rule.hour, rule.minute)} daily" else "OFF · not scheduled", color = if (rule.enabled) PrimaryLight else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (lastTriggered > 0L) {
            Spacer(Modifier.height(6.dp))
            Text("Last delivered ${formatInstant(lastTriggered)}", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RecentReminderRow(title: String, timestamp: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (timestamp > 0) Icons.Filled.CheckCircle else Icons.Filled.Notifications, null, tint = if (timestamp > 0) PrimaryLight else TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(if (timestamp > 0) formatInstant(timestamp) else "No reminder delivered yet", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { ReminderSettingsRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var pendingType by remember { mutableStateOf<String?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val study = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val dailyLog = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val type = pendingType
        pendingType = null
        if (granted && type != null) {
            val current = settings.getRule(type)
            settings.setRule(type, current.copy(enabled = true))
            ReminderScheduler(context).schedule(type)
            permissionMessage = "Reminder enabled."
            refresh++
        } else if (!granted) {
            permissionMessage = "Permission wasn't granted. The reminder stays off until notifications are allowed."
        }
    }

    fun saveRule(type: String, rule: ReminderRule) {
        settings.setRule(type, rule)
        ReminderScheduler(context).schedule(type)
        refresh++
        permissionMessage = null
    }

    fun toggleRule(type: String, enabled: Boolean) {
        if (!enabled) {
            saveRule(type, settings.getRule(type).copy(enabled = false))
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingType = type
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            saveRule(type, settings.getRule(type).copy(enabled = true))
        }
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Reminder settings", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JeeCard(featured = true) {
                Text("Build a reliable routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Choose which reminders you receive and when. Times use your device's local time.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
            ReminderSettingCard(
                title = "Daily study reminder",
                subtitle = "Prompt yourself to begin a focused study session.",
                rule = study,
                onToggle = { toggleRule(ReminderSettingsRepository.TYPE_DAILY_STUDY, it) },
                onTimeClick = { showTimePicker(context, study.hour, study.minute) { hour, minute ->
                    saveRule(ReminderSettingsRepository.TYPE_DAILY_STUDY, settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY).copy(hour = hour, minute = minute))
                } }
            )
            ReminderSettingCard(
                title = "Daily Log reminder",
                subtitle = "Prompt yourself to tick today's checklist and record what you studied.",
                rule = dailyLog,
                onToggle = { toggleRule(ReminderSettingsRepository.TYPE_DAILY_LOG, it) },
                onTimeClick = { showTimePicker(context, dailyLog.hour, dailyLog.minute) { hour, minute ->
                    saveRule(ReminderSettingsRepository.TYPE_DAILY_LOG, settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG).copy(hour = hour, minute = minute))
                } }
            )
            permissionMessage?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
            val permissionGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                JeeCard {
                    Text("Notifications are blocked by Android", fontWeight = FontWeight.SemiBold)
                    Text("Allow JeE notifications in system settings. If permission is denied, scheduled alarms won't display notifications.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Open system settings") }
                }
            }
            Text("No exact-alarm permission is requested. Android may deliver reminders a little later to protect battery life.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ReminderSettingCard(
    title: String,
    subtitle: String,
    rule: ReminderRule,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    JeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Reminder time", color = TextMuted, fontSize = 10.sp)
                Text(formatReminderTime(rule.hour, rule.minute), color = PrimaryLight, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onTimeClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Change time")
            }
        }
    }
}

private fun showTimePicker(context: Context, hour: Int, minute: Int, onTime: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, selectedHour, selectedMinute -> onTime(selectedHour, selectedMinute) }, hour, minute, DateFormat.is24HourFormat(context)).show()
}

private fun formatReminderTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (val h = hour % 12) { 0 -> 12; else -> h }
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
}

private fun formatInstant(timeMillis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timeMillis))
}.getOrDefault("Recently")
