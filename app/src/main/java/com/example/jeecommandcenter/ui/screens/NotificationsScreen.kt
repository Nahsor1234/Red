package com.example.jeecommandcenter.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.jeecommandcenter.data.CustomReminder
import com.example.jeecommandcenter.data.CustomReminderRepository
import com.example.jeecommandcenter.data.QuietHours
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
    val customRepository = remember { CustomReminderRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val study = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val dailyLog = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val masterEnabled = remember(refresh) { settings.isMasterEnabled() }
    val recentStudy = remember(refresh) { settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val recentLog = remember(refresh) { settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val customReminders = remember(refresh) { customRepository.all() }
    val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Notifications", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!notificationsAllowed || !appNotificationsEnabled) {
                JeeCard(featured = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, null, tint = PrimaryLight, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notifications are disabled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Allow notifications so scheduled reminders can reach you.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("Open Android notification settings") }
                }
            }

            Text("YOUR REMINDERS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            ReminderSummaryCard(
                title = "Daily study reminder",
                subtitle = "A nudge to begin a focused JEE session",
                icon = Icons.Filled.Alarm,
                rule = study,
                masterEnabled = masterEnabled,
                lastTriggered = recentStudy,
                onRefresh = { refresh++ }
            )
            ReminderSummaryCard(
                title = "Daily Progress reminder",
                subtitle = "Remember to mark today's progress and record what you studied",
                icon = Icons.Filled.Today,
                rule = dailyLog,
                masterEnabled = masterEnabled,
                lastTriggered = recentLog,
                onRefresh = { refresh++ }
            )

            Text("CUSTOM REMINDERS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (customReminders.isEmpty()) {
                Text("No custom reminders created yet.", color = TextMuted, fontSize = 11.sp)
            } else {
                customReminders.forEach { reminder ->
                    CustomReminderSummaryCard(reminder)
                }
            }

            Text("RECENT ACTIVITY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            JeeCard {
                RecentReminderRow("Daily study reminder", recentStudy)
                HorizontalDivider(color = BgDivider.copy(alpha = .6f))
                RecentReminderRow("Daily Progress reminder", recentLog)
                customReminders.filter { it.lastTriggered > 0L }.sortedByDescending { it.lastTriggered }.forEach { reminder ->
                    HorizontalDivider(color = BgDivider.copy(alpha = .6f))
                    RecentReminderRow(reminder.title, reminder.lastTriggered)
                }
            }
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manage reminder settings")
            }
            Text("Reminders run locally on this device. Notification text never reveals your private study notes.", color = TextMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun CustomReminderSummaryCard(reminder: CustomReminder) {
    JeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = PrimarySoft) {
                Icon(Icons.Filled.Notifications, null, tint = PrimaryLight, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (reminder.message.isNotBlank()) {
                    Text(reminder.message, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
                }
                Text(
                    when {
                        !reminder.enabled -> "OFF · not scheduled"
                        else -> "ON · ${formatReminderTime(reminder.hour, reminder.minute)} · ${reminder.repeat}"
                    },
                    color = if (reminder.enabled) PrimaryLight else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReminderSummaryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    rule: ReminderRule,
    masterEnabled: Boolean,
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
                Text(
                    when {
                        !rule.enabled -> "OFF · not scheduled"
                        !masterEnabled -> "PAUSED · master switch is off"
                        else -> "ON · ${formatReminderTime(rule.hour, rule.minute)} daily"
                    },
                    color = if (rule.enabled && masterEnabled) PrimaryLight else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
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
    val customRepository = remember { CustomReminderRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingCustomReminder by remember { mutableStateOf<CustomReminder?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val customReminders = remember(refresh) { customRepository.all() }
    var pendingType by remember { mutableStateOf<String?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val study = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val dailyLog = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val masterEnabled = remember(refresh) { settings.isMasterEnabled() }
    val quietHours = remember(refresh) { settings.getQuietHours() }
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
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JeeCard(featured = true) {
                Text("Build a reliable routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Choose which reminders you receive and when. Times use your device's local time.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
            JeeCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("All reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Pause or resume every scheduled reminder.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { enabled ->
                            settings.setMasterEnabled(enabled)
                            ReminderScheduler(context).scheduleAll()
                            refresh++
                            permissionMessage = null
                        }
                    )
                }
            }
            QuietHoursCard(context, quietHours) { updated ->
                settings.setQuietHours(updated)
                ReminderScheduler(context).scheduleAll()
                refresh++
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
                title = "Daily Progress reminder",
                subtitle = "Prompt yourself to mark today's progress and record what you studied.",
                rule = dailyLog,
                onToggle = { toggleRule(ReminderSettingsRepository.TYPE_DAILY_LOG, it) },
                onTimeClick = { showTimePicker(context, dailyLog.hour, dailyLog.minute) { hour, minute ->
                    saveRule(ReminderSettingsRepository.TYPE_DAILY_LOG, settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG).copy(hour = hour, minute = minute))
                } }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Custom reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Create your own title, message and schedule.", color = TextMuted, fontSize = 11.sp)
                }
                FilledIconButton(
                    onClick = { editingCustomReminder = null; showCustomDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = BgApp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Add custom reminder") }
            }
            if (customReminders.isEmpty()) {
                JeeCard {
                    Text("No custom reminders yet", fontWeight = FontWeight.Medium)
                    Text("Add a reminder for a topic, revision block or personal study goal.", color = TextSecondary, fontSize = 11.sp)
                }
            } else {
                customReminders.forEach { reminder ->
                    CustomReminderCard(
                        reminder = reminder,
                        onEnabledChange = { enabled ->
                            customRepository.setEnabled(reminder.id, enabled)
                            ReminderScheduler(context).scheduleCustom(reminder.id)
                            refresh++
                        },
                        onEdit = { editingCustomReminder = reminder; showCustomDialog = true },
                        onDelete = {
                            customRepository.delete(reminder.id)
                            ReminderScheduler(context).cancelCustom(reminder.id)
                            refresh++
                        }
                    )
                }
            }
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
    if (showCustomDialog) {
        CustomReminderDialog(
            existing = editingCustomReminder,
            onDismiss = { showCustomDialog = false; editingCustomReminder = null },
            onSave = { reminder ->
                customRepository.save(reminder)
                ReminderScheduler(context).scheduleCustom(reminder.id)
                showCustomDialog = false
                editingCustomReminder = null
                refresh++
            }
        )
    }
}

@Composable
private fun CustomReminderCard(
    reminder: CustomReminder,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    JeeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (reminder.message.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(reminder.message, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 3)
                }
                Spacer(Modifier.height(4.dp))
                Text("${formatReminderTime(reminder.hour, reminder.minute)} · ${reminder.repeat}", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                if (reminder.lastTriggered > 0L) {
                    Text("Last delivered ${formatInstant(reminder.lastTriggered)}", color = TextMuted, fontSize = 10.sp)
                }
            }
            Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit) { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
            TextButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Delete") }
        }
    }
}

@Composable
private fun CustomReminderDialog(
    existing: CustomReminder?,
    onDismiss: () -> Unit,
    onSave: (CustomReminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var message by remember(existing?.id) { mutableStateOf(existing?.message.orEmpty()) }
    var hour by remember(existing?.id) { mutableIntStateOf(existing?.hour ?: 18) }
    var minute by remember(existing?.id) { mutableIntStateOf(existing?.minute ?: 0) }
    var repeat by remember(existing?.id) { mutableStateOf(existing?.repeat ?: CustomReminder.REPEAT_DAILY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create reminder" else "Edit reminder") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Reminder title", color = TextSecondary, fontSize = 11.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CustomReminderRepository.MAX_TITLE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Revise Chemical Bonding") },
                    singleLine = true
                )
                Text("Message", color = TextSecondary, fontSize = 11.sp)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(CustomReminderRepository.MAX_MESSAGE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What should the notification remind you about?") },
                    minLines = 2,
                    maxLines = 4
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Reminder time", color = TextMuted, fontSize = 10.sp)
                        Text(formatReminderTime(hour, minute), color = PrimaryLight, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = { showTimePicker(context, hour, minute) { h, m -> hour = h; minute = m } }) {
                        Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Set time")
                    }
                }
                Text("Repeat", color = TextSecondary, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CustomReminder.repeatOptions.forEach { option ->
                        FilterChip(
                            selected = repeat == option,
                            onClick = { repeat = option },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }
                Text(
                    when (repeat) {
                        CustomReminder.REPEAT_ONCE -> "One-time reminders run at the next scheduled time, then turn themselves off."
                        CustomReminder.REPEAT_WEEKDAYS -> "Repeats Monday to Friday."
                        else -> "Repeats every day at this time."
                    },
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val reminder = CustomReminder(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title.trim(),
                        message = message.trim(),
                        hour = hour,
                        minute = minute,
                        repeat = repeat,
                        enabled = existing?.enabled ?: true,
                        lastTriggered = existing?.lastTriggered ?: 0L
                    )
                    onSave(reminder)
                }
            ) { Text("Save reminder") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuietHoursCard(context: Context, hours: QuietHours, onChange: (QuietHours) -> Unit) {
    JeeCard {
        Text("Quiet hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("Alerts are suppressed during this window. Reminders scheduled inside it move to the quiet-hours end time.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Start", color = TextMuted, fontSize = 10.sp)
                OutlinedButton(
                    onClick = { showTimePicker(context, hours.startHour, hours.startMinute) { h, m -> onChange(hours.copy(startHour = h, startMinute = m)) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(formatReminderTime(hours.startHour, hours.startMinute), fontSize = 11.sp, maxLines = 1) }
            }
            Column(Modifier.weight(1f)) {
                Text("End", color = TextMuted, fontSize = 10.sp)
                OutlinedButton(
                    onClick = { showTimePicker(context, hours.endHour, hours.endMinute) { h, m -> onChange(hours.copy(endHour = h, endMinute = m)) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(formatReminderTime(hours.endHour, hours.endMinute), fontSize = 11.sp, maxLines = 1) }
            }
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
