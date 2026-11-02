package com.example.jeecommandcenter.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jeecommandcenter.data.CustomReminder
import com.example.jeecommandcenter.data.CustomReminderRepository
import com.example.jeecommandcenter.data.QuietHours
import com.example.jeecommandcenter.data.ReminderRule
import com.example.jeecommandcenter.data.ReminderScheduler
import com.example.jeecommandcenter.data.ReminderNotificationPublisher
import com.example.jeecommandcenter.data.ReminderSettingsRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeFilterChip
import com.example.jeecommandcenter.ui.components.JeePageHeader
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.components.SectionHeader
import com.example.jeecommandcenter.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationCenterScreen(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    NotificationManagementScreen(onBack = onBack)
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
                Text(reminder.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (reminder.message.isNotBlank()) {
                    Text(reminder.message, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
                }
                Text(
                    if (reminder.repeat == CustomReminder.REPEAT_ONCE && !reminder.scheduledDate.isNullOrBlank()) {
                        "${formatReminderDate(reminder.scheduledDate)} · ${formatReminderTime(reminder.hour, reminder.minute)}"
                    } else {
                        formatReminderTime(reminder.hour, reminder.minute)
                    },
                    color = PrimaryLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                Text(
                    when {
                        !rule.enabled -> "OFF · not scheduled"
                        !masterEnabled -> "PAUSED · master switch is off"
                        else -> "ON · ${formatReminderTime(rule.hour, rule.minute)} daily"
                    },
                    color = if (rule.enabled && masterEnabled) PrimaryLight else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
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
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(if (timestamp > 0) formatInstant(timestamp) else "No reminder delivered yet", color = TextMuted, fontSize = 11.sp)
        }
    }
}

 
private data class DailyTimelineItem(
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int
)

@Composable
private fun DailyReminderTimeline(
    study: ReminderRule,
    dailyLog: ReminderRule,
    masterEnabled: Boolean,
    customReminders: List<CustomReminder>
) {
    val items = buildList {
        if (masterEnabled && study.enabled) {
            add(DailyTimelineItem("Daily study", "Start a focused JEE study block.", study.hour, study.minute))
        }
        if (masterEnabled && dailyLog.enabled) {
            add(DailyTimelineItem("Daily Progress", "Log today's work and close the loop.", dailyLog.hour, dailyLog.minute))
        }
        if (masterEnabled) {
            customReminders
                .filter { it.enabled && it.repeat == CustomReminder.REPEAT_DAILY }
                .forEach { reminder ->
                    add(
                        DailyTimelineItem(
                            reminder.title,
                            reminder.message.ifBlank { "Your scheduled study reminder." },
                            reminder.hour,
                            reminder.minute
                        )
                    )
                }
        }
    }.sortedWith(compareBy<DailyTimelineItem> { it.hour }.thenBy { it.minute }.thenBy { it.title.lowercase() })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("DAILY TIMELINE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Your recurring reminders, in time order", color = TextSecondary, fontSize = 11.sp)
        }
        JeeCard {
            if (items.isEmpty()) {
                Text("No active daily reminders yet.", color = TextSecondary, fontSize = 12.sp)
            } else {
                items.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            formatReminderTime(item.hour, item.minute),
                            modifier = Modifier.width(76.dp).padding(top = 1.dp),
                            color = PrimaryLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Column(
                            Modifier.width(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(PrimaryLight))
                            if (index < items.lastIndex) {
                                Spacer(Modifier.width(1.dp).height(34.dp).background(BgDivider))
                            }
                        }
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.message, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
                        }
                    }
                    if (index < items.lastIndex) HorizontalDivider(color = BgDivider.copy(alpha = .35f))
                }
            }
        }
    }
}

@Composable
private fun NotificationManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { ReminderSettingsRepository(context) }
    val customRepository = remember { CustomReminderRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var editingCustomReminder by remember { mutableStateOf<CustomReminder?>(null) }
    var confirmDeleteReminder by remember { mutableStateOf<CustomReminder?>(null) }
    var testNotificationStatus by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val customReminders = remember(refresh) { customRepository.all() }
    val masterEnabled = remember(refresh) { settings.isMasterEnabled() }
    val study = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
    val dailyLog = remember(refresh) { settings.getRule(ReminderSettingsRepository.TYPE_DAILY_LOG) }
    val timelineItems = remember(refresh) {
        buildTimelineItems(
            study = study,
            dailyLog = dailyLog,
            customReminders = customReminders,
            lastStudyTriggered = settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_STUDY),
            lastLogTriggered = settings.getLastTriggered(ReminderSettingsRepository.TYPE_DAILY_LOG)
        )
    }
    val nextUpcoming = if (masterEnabled) {
        timelineItems.filter { it.enabled && it.at.isAfter(LocalDateTime.now()) }.minByOrNull { it.at }
    } else null
    val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val reminderChannelEnabled = ReminderNotificationPublisher.isReminderChannelEnabled(context)
    val exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    var pendingSystemType by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val type = pendingSystemType
        pendingSystemType = null
        if (granted && type != null) {
            val current = settings.getRule(type)
            settings.setRule(type, current.copy(enabled = true))
            ReminderScheduler(context).schedule(type)
            refresh++
        } else {
            if (granted) ReminderScheduler(context).scheduleAll()
            refresh++
        }
    }

    fun toggleCustomReminder(reminder: CustomReminder, enabled: Boolean) {
        customRepository.setEnabled(reminder.id, enabled)
        ReminderScheduler(context).scheduleCustom(reminder.id)
        refresh++
    }

    fun toggleSystemReminder(type: String, enabled: Boolean) {
        if (!enabled) {
            settings.setRule(type, settings.getRule(type).copy(enabled = false))
            ReminderScheduler(context).schedule(type)
            refresh++
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSystemType = type
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            settings.setRule(type, settings.getRule(type).copy(enabled = true))
            ReminderScheduler(context).schedule(type)
            refresh++
        }
    }

    fun changeSystemReminderTime(type: String) {
        val current = settings.getRule(type)
        showTimePicker(context, current.hour, current.minute) { hour, minute ->
            settings.setRule(type, current.copy(hour = hour, minute = minute))
            ReminderScheduler(context).schedule(type)
            refresh++
        }
    }

    fun editReminder(reminder: CustomReminder) {
        editingCustomReminder = reminder
        showCustomDialog = true
    }

    Scaffold(containerColor = BgApp, topBar = { JeeTopBar(title = "Notifications", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JeePageHeader(
                title = "Study reminders",
                subtitle = "A clear view of delivery readiness and your upcoming routine.",
                eyebrow = "NOTIFICATIONS"
            )

            SectionHeader(title = "Delivery readiness")
            JeeCard(modifier = Modifier.animateContentSize()) {
                ReadinessRow("Notification permission", when {
                    Build.VERSION.SDK_INT < 33 -> "Not required on this Android version"
                    notificationsAllowed -> "Allowed"
                    else -> "Blocked"
                }, notificationsAllowed)
                ReadinessRow("App notifications", if (appNotificationsEnabled) "On" else "Off", appNotificationsEnabled)
                ReadinessRow("Reminder channel", if (reminderChannelEnabled) "On" else "Off", reminderChannelEnabled)
                ReadinessRow("Precise alarm access", when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> "Not required on this Android version"
                    exactAlarmsAllowed -> "Allowed"
                    else -> "Off · Android may delay reminders"
                }, Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmsAllowed)
                if (!notificationsAllowed || !appNotificationsEnabled || !reminderChannelEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Text("Scheduled reminders may not be delivered until Android notifications are enabled.", color = Warning, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingSystemType = null
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) "Allow notifications" else "Open notification settings")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            }.onFailure {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Enable precise timing") }
                }
            }

            SectionHeader(title = "Master controls")
            JeeCard(modifier = Modifier.animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("All reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (masterEnabled) "Your saved schedule is active."
                            else "The schedule is paused; individual reminders are unchanged.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Switch(
                            checked = masterEnabled,
                            modifier = Modifier.semantics { contentDescription = "All reminders" },
                            onCheckedChange = { enabled ->
                                settings.setMasterEnabled(enabled)
                                ReminderScheduler(context).scheduleAll()
                                refresh++
                            }
                        )
                    }
                }
                HorizontalDivider(color = BgDivider.copy(alpha = .6f))
                TextButton(
                    onClick = {
                        val posted = ReminderNotificationPublisher.showTest(context)
                        testNotificationStatus = if (posted) {
                            "Test notification requested. Check your notification shade."
                        } else {
                            "Test was blocked. Check notification permission and the Sigma JEE reminders channel."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send test notification")
                }
                testNotificationStatus?.let {
                    Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionHeader(title = if (masterEnabled) "Up next" else "Schedule paused")
            JeeCard(featured = true, modifier = Modifier.animateContentSize()) {
                when {
                    !masterEnabled -> {
                        Text("Nothing will notify you yet", style = MaterialTheme.typography.titleLarge)
                        Text("Turn All reminders back on to resume your saved schedule.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    nextUpcoming == null -> {
                        Text("Nothing scheduled", style = MaterialTheme.typography.titleLarge)
                        Text("Add a reminder to place your next study block on the timeline.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        Text(nextUpcoming.title, style = MaterialTheme.typography.titleLarge)
                        if (nextUpcoming.message.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(nextUpcoming.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(formatReminderTime(nextUpcoming.at.hour, nextUpcoming.at.minute), style = MaterialTheme.typography.titleMedium, color = Primary)
                            Spacer(Modifier.width(10.dp))
                            Text(formatTimelineDate(nextUpcoming.at.toLocalDate()), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            SectionHeader(title = "Routine reminders")
            ReminderSettingCard(
                title = "Daily study",
                subtitle = "A nudge to start a focused JEE study block.",
                rule = study,
                onToggle = { toggleSystemReminder(ReminderSettingsRepository.TYPE_DAILY_STUDY, it) },
                onTimeClick = { changeSystemReminderTime(ReminderSettingsRepository.TYPE_DAILY_STUDY) }
            )
            ReminderSettingCard(
                title = "Daily Progress",
                subtitle = "A reminder to record today's work and close the loop.",
                rule = dailyLog,
                onToggle = { toggleSystemReminder(ReminderSettingsRepository.TYPE_DAILY_LOG, it) },
                onTimeClick = { changeSystemReminderTime(ReminderSettingsRepository.TYPE_DAILY_LOG) }
            )

            SectionHeader(title = "Custom reminders")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Saved reminders", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                FilledIconButton(
                    onClick = { editingCustomReminder = null; showCustomDialog = true },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = BgCard)
                ) { Icon(Icons.Filled.Add, contentDescription = "Add reminder") }
            }
            if (customReminders.isEmpty()) {
                JeeCard {
                    Text("No custom reminders yet", style = MaterialTheme.typography.titleSmall)
                    Text("Add a one-time or daily reminder for a study task.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                customReminders.forEach { reminder ->
                    CustomReminderCard(
                        reminder = reminder,
                        onToggleEnabled = { toggleCustomReminder(reminder, it) },
                        onEdit = { editReminder(reminder) },
                        onDelete = { confirmDeleteReminder = reminder }
                    )
                }
            }

            SectionHeader(title = "Timeline")
            Text("Saved reminders, ordered by scheduled date and time.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            ReminderTimeline(items = timelineItems, masterEnabled = masterEnabled)
            Spacer(Modifier.height(12.dp))
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
                testNotificationStatus = null
                refresh++
            }
        )
    }

    confirmDeleteReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { confirmDeleteReminder = null },
            title = { Text("Delete reminder?") },
            text = { Text("Delete " + reminder.title + "? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        customRepository.delete(reminder.id)
                        ReminderScheduler(context).cancelCustom(reminder.id)
                        confirmDeleteReminder = null
                        refresh++
                    }
                ) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteReminder = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReadinessRow(label: String, value: String, ready: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            color = if (ready) Success else Warning,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CustomReminderCard(
    reminder: CustomReminder,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    JeeCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (reminder.message.isNotBlank()) {
                    Text(reminder.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                }
                Text(
                    if (reminder.repeat == CustomReminder.REPEAT_ONCE && !reminder.scheduledDate.isNullOrBlank()) {
                        "${formatReminderDate(reminder.scheduledDate)} · ${formatReminderTime(reminder.hour, reminder.minute)}"
                    } else {
                        "${formatReminderTime(reminder.hour, reminder.minute)} · ${reminder.repeat.lowercase(Locale.getDefault())}"
                    },
                    color = if (reminder.enabled) Primary else TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.semantics { contentDescription = "${reminder.title} reminder" }
                )
            }
        }
        HorizontalDivider(color = BgDivider.copy(alpha = .6f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${reminder.title}", tint = Primary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${reminder.title}", tint = Danger)
            }
        }
    }
}

@Composable
private fun CustomReminderActions(
    reminder: CustomReminder,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember(reminder.id) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Reminder options", tint = TextSecondary, modifier = Modifier.size(19.dp))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { menuExpanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(if (reminder.enabled) "Turn off" else "Turn on") },
                leadingIcon = { Icon(Icons.Filled.Notifications, null) },
                onClick = { menuExpanded = false; onToggleEnabled(!reminder.enabled) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Danger) },
                onClick = { menuExpanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun SystemReminderActions(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onChangeTime: () -> Unit
) {
    var expanded by remember(title) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "$title options", tint = TextSecondary, modifier = Modifier.size(19.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Change time") },
                leadingIcon = { Icon(Icons.Filled.Alarm, null) },
                onClick = { expanded = false; onChangeTime() }
            )
            DropdownMenuItem(
                text = { Text(if (enabled) "Turn off" else "Turn on") },
                leadingIcon = { Icon(Icons.Filled.Notifications, null) },
                onClick = { expanded = false; onToggle(!enabled) }
            )
        }
    }
}

@Composable
private fun DailyReminderTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    rule: ReminderRule,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    JeeCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = PrimarySoft) {
                Icon(icon, contentDescription = null, tint = PrimaryLight, modifier = Modifier.padding(8.dp).size(18.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp, minLines = 1, maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Text(formatReminderTime(rule.hour, rule.minute), color = PrimaryLight, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Every day", color = TextMuted, fontSize = 10.sp)
        TextButton(onClick = onTimeClick, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
            Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Change time", fontSize = 11.sp)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (rule.enabled) "ON" else "OFF", color = if (rule.enabled) PrimaryLight else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Switch(checked = rule.enabled, onCheckedChange = onToggle, modifier = Modifier.width(48.dp))
        }
    }
}

private data class ReminderScheduleItem(
    val title: String,
    val message: String,
    val at: LocalDateTime,
    val enabled: Boolean,
    val customReminder: CustomReminder? = null,
    val systemType: String? = null,
    val repeatLabel: String = "Daily"
)

private fun nextScheduledDateTime(
    now: LocalDateTime,
    hour: Int,
    minute: Int,
    repeat: String,
    scheduledDate: String? = null
): LocalDateTime? {
    val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    if (repeat == CustomReminder.REPEAT_ONCE) {
        val date = scheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return date.atTime(time).takeIf { it.isAfter(now) }
    }
    val selectedStartDate = scheduledDate
        ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        ?: now.toLocalDate()
    val firstDate = if (selectedStartDate.isAfter(now.toLocalDate())) selectedStartDate else now.toLocalDate()
    for (offset in 0L..7L) {
        val date = firstDate.plusDays(offset)
        if (repeat == CustomReminder.REPEAT_WEEKDAYS &&
            (date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY)
        ) continue
        val candidate = date.atTime(time)
        if (candidate.isAfter(now)) return candidate
    }
    return null
}

private fun buildTimelineItems(
    study: ReminderRule,
    dailyLog: ReminderRule,
    customReminders: List<CustomReminder>,
    lastStudyTriggered: Long,
    lastLogTriggered: Long
): List<ReminderScheduleItem> {
    val now = LocalDateTime.now()
    return buildList {
        if (study.enabled || lastStudyTriggered > 0L) {
            nextScheduledDateTime(now, study.hour, study.minute, CustomReminder.REPEAT_DAILY)?.let {
                add(
                    ReminderScheduleItem(
                        title = "Daily study",
                        message = "Start a focused JEE study block.",
                        at = it,
                        enabled = study.enabled,
                        systemType = ReminderSettingsRepository.TYPE_DAILY_STUDY,
                        repeatLabel = CustomReminder.REPEAT_DAILY
                    )
                )
            }
        }
        if (dailyLog.enabled || lastLogTriggered > 0L) {
            nextScheduledDateTime(now, dailyLog.hour, dailyLog.minute, CustomReminder.REPEAT_DAILY)?.let {
                add(
                    ReminderScheduleItem(
                        title = "Daily progress",
                        message = "Record what you studied and close today's loop.",
                        at = it,
                        enabled = dailyLog.enabled,
                        systemType = ReminderSettingsRepository.TYPE_DAILY_LOG,
                        repeatLabel = CustomReminder.REPEAT_DAILY
                    )
                )
            }
        }
        customReminders.forEach { reminder ->
            val scheduled = if (reminder.repeat == CustomReminder.REPEAT_ONCE) {
                reminder.scheduledDate
                    ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
                    ?.atTime(reminder.hour, reminder.minute)
            } else {
                nextScheduledDateTime(
                    now,
                    reminder.hour,
                    reminder.minute,
                    reminder.repeat,
                    reminder.scheduledDate
                )
            }
            scheduled?.let {
                add(
                    ReminderScheduleItem(
                        title = reminder.title,
                        message = reminder.message,
                        at = it,
                        enabled = reminder.enabled,
                        customReminder = reminder,
                        repeatLabel = reminder.repeat
                    )
                )
            }
        }
    }.sortedWith(compareBy<ReminderScheduleItem> { it.at }.thenBy { it.title.lowercase() })
}

private fun formatTimelineDate(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
}

@Composable
private fun ReminderTimeline(
    items: List<ReminderScheduleItem>,
    masterEnabled: Boolean
) {
    if (items.isEmpty()) {
        JeeCard {
            Text("Your timeline is empty", style = MaterialTheme.typography.titleSmall)
            Text("Saved reminders will appear here in date and time order.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val groups = items.groupBy { it.at.toLocalDate() }.toSortedMap()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { (date, dayItems) ->
            Text(
                formatTimelineDate(date),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                dayItems.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            formatReminderTime(item.at.hour, item.at.minute),
                            modifier = Modifier.width(76.dp).padding(top = 12.dp),
                            color = if (item.enabled && masterEnabled) Primary else TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Column(
                            Modifier.width(10.dp).padding(top = 13.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                .background(if (item.enabled && masterEnabled) Primary else TextMuted)
                            )
                            if (index < dayItems.lastIndex) {
                                Spacer(
                                    Modifier.padding(top = 3.dp)
                                        .width(1.dp)
                                        .height(66.dp)
                                        .background(BgDivider)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = BgCard,
                            border = BorderStroke(1.dp, BgCardBorder.copy(alpha = .75f))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        item.title,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (item.message.isNotBlank()) {
                                        Text(
                                            item.message,
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    Text(
                                        when {
                                            !masterEnabled && item.enabled -> "PAUSED"
                                            item.repeatLabel == CustomReminder.REPEAT_ONCE && item.at.isBefore(LocalDateTime.now()) -> "PAST DUE · EDIT TO RESCHEDULE"
                                            !item.enabled -> "OFF"
                                            item.repeatLabel == CustomReminder.REPEAT_ONCE -> "ONCE"
                                            else -> "DAILY"
                                        },
                                        color = if (masterEnabled && item.enabled) Primary else TextMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    val defaultHour = existing?.hour ?: 18
    val defaultMinute = existing?.minute ?: 0
    val defaultTime = LocalTime.of(defaultHour, defaultMinute)
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var message by remember(existing?.id) { mutableStateOf(existing?.message.orEmpty()) }
    var hour by remember(existing?.id) { mutableIntStateOf(defaultHour) }
    var minute by remember(existing?.id) { mutableIntStateOf(defaultMinute) }
    var repeatMode by remember(existing?.id) {
        mutableStateOf(if (existing?.repeat == CustomReminder.REPEAT_ONCE) CustomReminder.REPEAT_ONCE else CustomReminder.REPEAT_DAILY)
    }
    val today = LocalDate.now()
    val suggestedDate = if (defaultTime.isAfter(LocalTime.now())) today else today.plusDays(1)
    var scheduledDate by remember(existing?.id) {
        mutableStateOf(
            existing?.scheduledDate
                ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
                ?.takeIf { date ->
                    !date.isBefore(today) && (date.isAfter(today) || defaultTime.isAfter(LocalTime.now()))
                }
                ?: suggestedDate
        )
    }
    val chosenTime = LocalTime.of(hour, minute)
    val validSchedule = !scheduledDate.isBefore(today) &&
        (scheduledDate.isAfter(today) || chosenTime.isAfter(LocalTime.now()))
    val dateLabel = if (repeatMode == CustomReminder.REPEAT_DAILY) "Start date" else "Date"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.93f).widthIn(max = 420.dp).heightIn(max = 620.dp),
            shape = RoundedCornerShape(26.dp),
            color = BgCard,
            border = BorderStroke(1.dp, BgCardBorder)
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (existing == null) "New reminder" else "Edit reminder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Add it to your study routine.", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close reminder editor", tint = TextMuted)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(CustomReminderRepository.MAX_TITLE_LENGTH) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 66.dp),
                    label = { Text("Title", fontSize = 13.sp) },
                    placeholder = { Text("e.g. Revise vectors", fontSize = 15.sp) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(CustomReminderRepository.MAX_MESSAGE_LENGTH) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp, max = 104.dp),
                    label = { Text("Message (optional)", fontSize = 13.sp) },
                    placeholder = { Text("What should you remember?", fontSize = 15.sp) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Repeat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JeeFilterChip(
                            label = "Daily",
                            selected = repeatMode == CustomReminder.REPEAT_DAILY,
                            onClick = { repeatMode = CustomReminder.REPEAT_DAILY }
                        )
                        JeeFilterChip(
                            label = "Once",
                            selected = repeatMode == CustomReminder.REPEAT_ONCE,
                            onClick = { repeatMode = CustomReminder.REPEAT_ONCE }
                        )
                    }
                    Text(
                        if (repeatMode == CustomReminder.REPEAT_DAILY) {
                            "Repeats every day from the selected start date."
                        } else {
                            "Runs once on the selected date and time."
                        },
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(dateLabel, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        scheduledDate = LocalDate.of(year, month + 1, day)
                                    },
                                    scheduledDate.year,
                                    scheduledDate.monthValue - 1,
                                    scheduledDate.dayOfMonth
                                ).apply {
                                    datePicker.minDate = LocalDate.now()
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                }.show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Icon(Icons.Filled.Today, null, modifier = Modifier.size(17.dp), tint = PrimaryLight)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                scheduledDate.format(DateTimeFormatter.ofPattern("d MMM yy", Locale.getDefault())),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = { showTimePicker(context, hour, minute) { h, m -> hour = h; minute = m } },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(17.dp), tint = PrimaryLight)
                            Spacer(Modifier.width(6.dp))
                            Text(formatReminderTime(hour, minute), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                if (!validSchedule) {
                    Text("Choose a future date and time.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                HorizontalDivider(color = BgDivider.copy(alpha = .5f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = title.isNotBlank() && validSchedule,
                        onClick = {
                            onSave(
                                CustomReminder(
                                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    message = message.trim(),
                                    hour = hour,
                                    minute = minute,
                                    repeat = repeatMode,
                                    enabled = existing?.enabled ?: true,
                                    lastTriggered = existing?.lastTriggered ?: 0L,
                                    scheduledDate = scheduledDate.toString()
                                )
                            )
                        },
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLight,
                            contentColor = BgAppBase
                        )
                    ) { Text("Save reminder", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
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
    JeeCard(modifier = Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = "$title reminder" }
                )
            }
        }
        HorizontalDivider(color = BgDivider.copy(alpha = .6f))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Reminder time", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Text(formatReminderTime(rule.hour, rule.minute), color = Primary, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onTimeClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Alarm, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Change time")
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    NotificationManagementScreen(onBack = onBack)
}

private fun showTimePicker(context: Context, hour: Int, minute: Int, onTime: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, selectedHour, selectedMinute -> onTime(selectedHour, selectedMinute) }, hour, minute, DateFormat.is24HourFormat(context)).show()
}

private fun formatReminderTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (val h = hour % 12) { 0 -> 12; else -> h }
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
}

private fun formatReminderDate(date: String?): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
}.getOrDefault("Specific date")

private fun formatInstant(timeMillis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timeMillis))
}.getOrDefault("Recently")
