package com.example.jeecommandcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.RevisionNote
import com.example.jeecommandcenter.data.RevisionNotesRepository
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RevisionScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val notesRepository = remember(context) { RevisionNotesRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val notes = remember(refresh) { notesRepository.getNotes() }
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<RevisionNote?>(null) }
    var deleteCandidate by remember { mutableStateOf<RevisionNote?>(null) }

    var draftTitle by remember { mutableStateOf("") }
    var draftSubject by remember { mutableStateOf("General") }
    var draftChapter by remember { mutableStateOf("") }
    var draftContent by remember { mutableStateOf("") }

    fun openEditor(note: RevisionNote?) {
        editingNote = note
        draftTitle = note?.title.orEmpty()
        draftSubject = note?.subject?.takeIf { it in listOf("General", "Physics", "Chemistry", "Mathematics") } ?: "General"
        draftChapter = note?.chapter.orEmpty()
        draftContent = note?.content.orEmpty()
        showEditor = true
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Notes & Revision",
                subtitle = if (notes.isEmpty()) "Your personal JEE revision library" else "${notes.size} saved notes",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { openEditor(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create revision note", tint = PrimaryLight)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Your revision library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Save formulas, tricky concepts, derivations, exceptions and reminders in one place.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            if (notes.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(BgCardAlt)
                            .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(Icons.Filled.NoteAdd, null, tint = PrimaryLight, modifier = Modifier.size(28.dp))
                        Text("Create your first revision note", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "For example, create “Revise Electrostatics” and keep important formulas or doubts inside it.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Button(onClick = { openEditor(null) }) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add a note")
                        }
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                            .clickable { openEditor(note) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(11.dp), color = PrimarySoft) {
                                Icon(Icons.Filled.Description, null, tint = PrimaryLight, modifier = Modifier.padding(10.dp).size(20.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                val metadata = listOf(note.subject, note.chapter).filter { it.isNotBlank() }.joinToString(" · ")
                                if (metadata.isNotBlank()) Text(metadata, color = PrimaryLight, fontSize = 11.sp)
                                if (note.content.isNotBlank()) {
                                    Text(note.content, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3)
                                } else {
                                    Text("No note content yet · Tap to add", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { deleteCandidate = note }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete note", tint = TextMuted)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Updated " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(note.updatedAt)), color = TextMuted, fontSize = 10.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { openEditor(note) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingNote == null) "New revision note" else "Edit revision note") },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. Revise Electrostatics") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Subject", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("General", "Physics", "Chemistry", "Mathematics").forEach { value ->
                            FilterChip(
                                selected = draftSubject == value,
                                onClick = { draftSubject = value },
                                label = { Text(value, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draftChapter,
                        onValueChange = { draftChapter = it },
                        label = { Text("Chapter (optional)") },
                        placeholder = { Text("e.g. Electrostatic Potential") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        label = { Text("Notes") },
                        placeholder = { Text("Write formulas, concepts, derivations, exceptions or reminders…") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = draftTitle.isNotBlank(),
                    onClick = {
                        val now = System.currentTimeMillis()
                        notesRepository.saveNote(
                            RevisionNote(
                                id = editingNote?.id.orEmpty(),
                                title = draftTitle,
                                subject = draftSubject,
                                chapter = draftChapter,
                                content = draftContent,
                                createdAt = editingNote?.createdAt ?: now,
                                updatedAt = now
                            )
                        )
                        showEditor = false
                        editingNote = null
                        refresh++
                    }
                ) { Text("Save note") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } }
        )
    }

    deleteCandidate?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete note?") },
            text = { Text("“${note.title}” will be removed from your revision library.") },
            confirmButton = {
                TextButton(onClick = {
                    notesRepository.deleteNote(note.id)
                    deleteCandidate = null
                    refresh++
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } }
        )
    }
}
