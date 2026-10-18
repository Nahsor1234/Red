package com.example.jeecommandcenter.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.MoreVert
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
import java.time.LocalDate
import java.time.LocalTime
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
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            Text("YOUR REMINDERS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
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

            Text("CUSTOM REMINDERS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            if (customReminders.isEmpty()) {
                Text("No custom reminders created yet.", color = TextMuted, fontSize = 11.sp)
            } else {
                customReminders.forEach { reminder ->
                    CustomReminderSummaryCard(reminder)
                }
            }

            Text("RECENT ACTIVITY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
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
            DailyReminderTimeline(study, dailyLog, masterEnabled, customReminders)
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
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryLight, contentColor = BgApp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add custom reminder", tint = BgApp, modifier = Modifier.size(25.dp))
                }
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
                        onToggleEnabled = { enabled ->
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
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember(reminder.id) { mutableStateOf(false) }
    JeeCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (reminder.message.isNotBlank()) {
                    Text(
                        reminder.message,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 3
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.Alarm, null, tint = PrimaryLight, modifier = Modifier.size(15.dp))
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
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Reminder options", tint = TextSecondary)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (reminder.enabled) "Pause reminder" else "Enable reminder") },
                        leadingIcon = { Icon(Icons.Filled.Alarm, null) },
                        onClick = {
                            menuExpanded = false
                            onToggleEnabled(!reminder.enabled)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
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
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var message by remember(existing?.id) { mutableStateOf(existing?.message.orEmpty()) }
    var hour by remember(existing?.id) { mutableIntStateOf(existing?.hour ?: 18) }
    var minute by remember(existing?.id) { mutableIntStateOf(existing?.minute ?: 0) }
    var repeat by remember(existing?.id) {
        mutableStateOf(if (existing?.repeat == CustomReminder.REPEAT_ONCE) CustomReminder.REPEAT_ONCE else CustomReminder.REPEAT_DAILY)
    }
    var scheduledDate by remember(existing?.id) {
        mutableStateOf(
            existing?.scheduledDate?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
                ?: LocalDate.now().plusDays(1)
        )
    }
    val validSchedule = repeat != CustomReminder.REPEAT_ONCE ||
        scheduledDate.isAfter(LocalDate.now()) ||
        (scheduledDate == LocalDate.now() && LocalTime.of(hour, minute).isAfter(LocalTime.now()))
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
                Text("Schedule", color = TextSecondary, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(CustomReminder.REPEAT_DAILY, CustomReminder.REPEAT_ONCE).forEach { option ->
                        FilterChip(
                            selected = repeat == option,
                            onClick = { repeat = option },
                            label = { Text(if (option == CustomReminder.REPEAT_ONCE) "Specific date" else "Daily", fontSize = 11.sp) }
                        )
                    }
                }
                if (repeat == CustomReminder.REPEAT_ONCE) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Reminder date", color = TextMuted, fontSize = 10.sp)
                            Text(
                                scheduledDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                                color = PrimaryLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day -> scheduledDate = LocalDate.of(year, month + 1, day) },
                                    scheduledDate.year,
                                    scheduledDate.monthValue - 1,
                                    scheduledDate.dayOfMonth
                                ).apply {
                                    datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                }.show()
                            }
                        ) { Text("Choose date") }
                    }
                    if (!validSchedule) {
                        Text("Choose a future date or a time that has not passed today.", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    } else {
                        Text("This reminder runs once on the selected date.", color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                } else {
                    Text("Repeats every day at this time.", color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && validSchedule,
                onClick = {
                    val reminder = CustomReminder(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title.trim(),
                        message = message.trim(),
                        hour = hour,
                        minute = minute,
                        repeat = repeat,
                        enabled = existing?.enabled ?: true,
                        lastTriggered = existing?.lastTriggered ?: 0L,
                        scheduledDate = if (repeat == CustomReminder.REPEAT_ONCE) scheduledDate.toString() else null
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

private fun formatReminderDate(date: String?): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
}.getOrDefault("Specific date")

private fun formatInstant(timeMillis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timeMillis))
}.getOrDefault("Recently")
