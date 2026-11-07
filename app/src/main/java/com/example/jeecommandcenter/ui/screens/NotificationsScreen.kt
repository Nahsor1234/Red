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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jeecommandcenter.data.CustomReminder
import com.example.jeecommandcenter.data.CustomReminderRepository
import com.example.jeecommandcenter.data.ReminderRule
import com.example.jeecommandcenter.data.ReminderScheduler
import com.example.jeecommandcenter.data.ReminderNotificationPublisher
import com.example.jeecommandcenter.data.ReminderSettingsRepository
import com.example.jeecommandcenter.ui.components.JeeCard
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
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

    Scaffold(
        containerColor = BgApp,
        topBar = { JeeTopBar(title = "Notifications", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "YOUR STUDY ROUTINE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Text("Stay on track", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Plan a reminder for the right moment.", color = TextSecondary, fontSize = 12.sp)
            }

            JeeCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("All reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (masterEnabled) "Your scheduled reminders are enabled."
                            else "All scheduled reminders are paused.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { enabled ->
                            settings.setMasterEnabled(enabled)
                            ReminderScheduler(context).scheduleAll()
                            refresh++
                        },
                        modifier = Modifier.width(48.dp)
                    )
                }
                HorizontalDivider(color = BgDivider.copy(alpha = .45f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        val posted = ReminderNotificationPublisher.showTest(context)
                        testNotificationStatus = if (posted) {
                            "Test notification requested. Check your notification shade."
                        } else {
                            "Test was blocked. Check notification permission and the Sigma JEE reminders channel."
                        }
                    }) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send test notification")
                    }
                }
                testNotificationStatus?.let {
                    Text(it, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            JeeCard(featured = true) {
                Text(
                    if (!masterEnabled) "REMINDERS PAUSED" else "UP NEXT",
                    color = PrimaryLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(7.dp))
                when {
                    !masterEnabled -> {
                        Text("Nothing will notify you yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Turn All reminders back on to resume your schedule.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    nextUpcoming == null -> {
                        Text("Nothing scheduled", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Add a reminder to give your next study block a place on the timeline.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    else -> {
                        Text(nextUpcoming.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (nextUpcoming.message.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(nextUpcoming.message, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Alarm, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                formatReminderTime(nextUpcoming.at.hour, nextUpcoming.at.minute),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLight
                            )
                            Spacer(Modifier.width(9.dp))
                            Text("·", color = TextMuted)
                            Spacer(Modifier.width(9.dp))
                            Text(formatTimelineDate(nextUpcoming.at.toLocalDate()), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        timelineItems.size.toString() + if (timelineItems.size == 1) " reminder" else " reminders",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                FilledIconButton(
                    onClick = { editingCustomReminder = null; showCustomDialog = true },
                    modifier = Modifier.size(42.dp),
                    shape = JeeShapes.medium,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryLight,
                        contentColor = BgAppBase
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add reminder", modifier = Modifier.size(23.dp))
                }
            }

            ReminderTimeline(
                items = timelineItems,
                masterEnabled = masterEnabled,
                onCustomToggle = ::toggleCustomReminder,
                onCustomEdit = ::editReminder,
                onCustomDelete = { confirmDeleteReminder = it },
                onSystemToggle = ::toggleSystemReminder,
                onSystemTimeClick = ::changeSystemReminderTime
            )

            if (!notificationsAllowed || !appNotificationsEnabled || !reminderChannelEnabled) {
                JeeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, null, tint = PrimaryLight, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Notification delivery is blocked", fontWeight = FontWeight.SemiBold)
                            Text("Enable app notifications and the Sigma JEE reminders channel in Android settings.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = JeeShapes.medium
                    ) {
                        Text(if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) "Allow notifications" else "Open system notification settings")
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsAllowed) {
                JeeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Alarm, null, tint = PrimaryLight, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Precise alarm access is off", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Android may delay reminders. Enable Alarms & reminders access for the most reliable timing.",
                                color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = JeeShapes.medium
                    ) { Text("Enable precise timing") }
                }
            }
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
private fun CustomReminderActions(
    reminder: CustomReminder,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember(reminder.id) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Reminder options", tint = TextSecondary, modifier = Modifier.size(19.dp))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menuExpanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(if (reminder.enabled) "Turn off" else "Turn on") },
                leadingIcon = { Icon(Icons.Rounded.Notifications, null) },
                onClick = { menuExpanded = false; onToggleEnabled(!reminder.enabled) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Danger) },
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
            Icon(Icons.Rounded.MoreVert, contentDescription = "$title options", tint = TextSecondary, modifier = Modifier.size(19.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Change time") },
                leadingIcon = { Icon(Icons.Rounded.Alarm, null) },
                onClick = { expanded = false; onChangeTime() }
            )
            DropdownMenuItem(
                text = { Text(if (enabled) "Turn off" else "Turn on") },
                leadingIcon = { Icon(Icons.Rounded.Notifications, null) },
                onClick = { expanded = false; onToggle(!enabled) }
            )
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
    masterEnabled: Boolean,
    onCustomToggle: (CustomReminder, Boolean) -> Unit,
    onCustomEdit: (CustomReminder) -> Unit,
    onCustomDelete: (CustomReminder) -> Unit,
    onSystemToggle: (String, Boolean) -> Unit,
    onSystemTimeClick: (String) -> Unit
) {
    if (items.isEmpty()) {
        JeeCard {
            Text("Your timeline is empty", fontWeight = FontWeight.SemiBold)
            Text("Use + to add a reminder. It will appear here on its scheduled date.", color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
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
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            formatReminderTime(item.at.hour, item.at.minute),
                            modifier = Modifier.width(62.dp).padding(top = 12.dp),
                            color = if (item.enabled && masterEnabled) PrimaryLight else TextMuted,
                            fontSize = 11.sp,
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
                                    .background(if (item.enabled && masterEnabled) PrimaryLight else TextMuted)
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
                            modifier = Modifier.weight(1f).clickable {
                                item.customReminder?.let {
                                    if (it.repeat == CustomReminder.REPEAT_ONCE && item.at.isBefore(LocalDateTime.now())) {
                                        onCustomEdit(it)
                                    } else {
                                        onCustomToggle(it, !it.enabled)
                                    }
                                }
                                item.systemType?.let { onSystemToggle(it, !item.enabled) }
                            },
                            shape = JeeShapes.medium,
                            color = BgCard,
                            border = BorderStroke(1.dp, BgCardBorder)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        item.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 19.sp
                                    )
                                    if (item.message.isNotBlank()) {
                                        Text(
                                            item.message,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 3
                                        )
                                    }
                                    Text(
                                        when {
                                            !masterEnabled && item.enabled -> "PAUSED"
                                            item.repeatLabel == CustomReminder.REPEAT_ONCE && item.at.isBefore(LocalDateTime.now()) -> "PAST DUE · EDIT TO RESCHEDULE"
                                            !item.enabled -> "OFF · TAP TO TURN ON"
                                            item.repeatLabel == CustomReminder.REPEAT_ONCE -> "ONCE · TAP TO TURN OFF"
                                            else -> "DAILY · TAP TO TURN OFF"
                                        },
                                        color = if (masterEnabled && item.enabled) PrimaryLight else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = .35.sp
                                    )
                                }
                                when {
                                    item.customReminder != null -> {
                                        val reminder = item.customReminder
                                        CustomReminderActions(
                                            reminder = reminder,
                                            onToggleEnabled = { onCustomToggle(reminder, it) },
                                            onEdit = { onCustomEdit(reminder) },
                                            onDelete = { onCustomDelete(reminder) }
                                        )
                                    }
                                    item.systemType != null -> {
                                        SystemReminderActions(
                                            title = item.title,
                                            enabled = item.enabled,
                                            onToggle = { onSystemToggle(item.systemType, it) },
                                            onChangeTime = { onSystemTimeClick(item.systemType) }
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
            shape = JeeShapes.large,
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
                        Icon(Icons.Rounded.Close, contentDescription = "Close reminder editor", tint = TextMuted)
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
                    shape = JeeShapes.medium
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
                    shape = JeeShapes.medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Repeat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = repeatMode == CustomReminder.REPEAT_DAILY,
                            onClick = { repeatMode = CustomReminder.REPEAT_DAILY },
                            label = { Text("Daily") },
                            leadingIcon = if (repeatMode == CustomReminder.REPEAT_DAILY) {
                                { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = repeatMode == CustomReminder.REPEAT_ONCE,
                            onClick = { repeatMode = CustomReminder.REPEAT_ONCE },
                            label = { Text("Once") },
                            leadingIcon = if (repeatMode == CustomReminder.REPEAT_ONCE) {
                                { Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                            } else null
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
                            shape = JeeShapes.medium
                        ) {
                            Icon(Icons.Rounded.Today, null, modifier = Modifier.size(17.dp), tint = PrimaryLight)
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
                            shape = JeeShapes.medium
                        ) {
                            Icon(Icons.Rounded.Alarm, null, modifier = Modifier.size(17.dp), tint = PrimaryLight)
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
                        shape = JeeShapes.medium,
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
