package com.example.jeecommandcenter.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LabelOutline
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jeecommandcenter.data.RevisionNote
import com.example.jeecommandcenter.data.RevisionNotesRepository
import com.example.jeecommandcenter.ui.components.JeeTopBar
import com.example.jeecommandcenter.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val noteTags = listOf("No tag", "Revision", "Physics", "Chemistry", "Mathematics", "Ideas")

@Composable
fun RevisionScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val notesRepository = remember(context) { RevisionNotesRepository(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val notes = remember(refresh) { notesRepository.getNotes() }
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<RevisionNote?>(null) }
    var deleteCandidate by remember { mutableStateOf<RevisionNote?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var saveStatus by remember { mutableStateOf("Draft") }

    var draftId by remember { mutableStateOf("") }
    var draftCreatedAt by remember { mutableLongStateOf(0L) }
    var draftTitle by remember { mutableStateOf("") }
    var draftTag by remember { mutableStateOf("No tag") }
    var draftContent by remember { mutableStateOf("") }
    var tagPickerOpen by remember { mutableStateOf(false) }

    fun openEditor(note: RevisionNote?) {
        editingNote = note
        draftId = note?.id ?: UUID.randomUUID().toString()
        draftCreatedAt = note?.createdAt ?: System.currentTimeMillis()
        draftTitle = note?.title.orEmpty()
        draftTag = note?.subject?.takeIf { it in noteTags && it != "General" } ?: "No tag"
        draftContent = note?.content.orEmpty()
        tagPickerOpen = false
        saveStatus = if (note == null) "Draft" else "Saved"
        showEditor = true
    }

    fun persistDraft() {
        if (draftTitle.isBlank() && draftContent.isBlank() && editingNote == null) {
            saveStatus = "Draft"
            return
        }
        val now = System.currentTimeMillis()
        val saved = notesRepository.saveNote(
            RevisionNote(
                id = draftId,
                title = draftTitle.trim().ifBlank { "Untitled note" },
                subject = if (draftTag == "No tag") "General" else draftTag,
                // Preserve existing chapter metadata for older notes without forcing a chapter field into the new editor.
                chapter = editingNote?.chapter.orEmpty(),
                content = draftContent,
                createdAt = draftCreatedAt.takeIf { it > 0L } ?: now,
                updatedAt = now
            )
        )
        draftId = saved.id
        draftCreatedAt = saved.createdAt
        editingNote = saved
        saveStatus = "Saved"
        refresh++
    }

    LaunchedEffect(showEditor, draftId, draftTitle, draftTag, draftContent) {
        if (showEditor && (draftTitle.isNotBlank() || draftContent.isNotBlank())) {
            saveStatus = "Saving…"
            delay(650)
            persistDraft()
        }
    }

    Scaffold(
        containerColor = BgApp,
        topBar = {
            JeeTopBar(
                title = "Notes",
                subtitle = "Focused writing",
                onBack = if (showEditor) ({ persistDraft(); showEditor = false; tagPickerOpen = false }) else onBack,
                trailing = {
                    IconButton(onClick = {
                        if (showEditor) {
                            persistDraft()
                            showEditor = false
                            tagPickerOpen = false
                        } else {
                            openEditor(null)
                        }
                    }) {
                        Icon(
                            imageVector = if (showEditor) Icons.Filled.GridView else Icons.Filled.Add,
                            contentDescription = if (showEditor) "All notes" else "Create note",
                            tint = PrimaryLight
                        )
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = showEditor,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn()).togetherWith(
                        slideOutHorizontally(targetOffsetX = { -it / 5 }) + fadeOut()
                    )
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 5 }) + fadeIn()).togetherWith(
                        slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut()
                    )
                }.using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "notesHorizontalSlide"
        ) { editorVisible ->
            if (editorVisible) {
                NoteEditor(
                    title = draftTitle,
                    onTitleChange = { draftTitle = it; saveStatus = "Draft" },
                    content = draftContent,
                    onContentChange = { draftContent = it; saveStatus = "Draft" },
                    tag = draftTag,
                    onTagClick = { tagPickerOpen = !tagPickerOpen },
                    tagPickerOpen = tagPickerOpen,
                    onTagSelected = { draftTag = it; tagPickerOpen = false; saveStatus = "Draft" },
                    saveStatus = saveStatus,
                    onBack = { persistDraft(); showEditor = false; tagPickerOpen = false },
                    onSave = { persistDraft(); tagPickerOpen = false }
                )
            } else {
                NotesLibrary(
                    notes = notes,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onOpenNote = ::openEditor,
                    onNewNote = { openEditor(null) },
                    onDeleteNote = { deleteCandidate = it }
                )
            }
        }
    }

    deleteCandidate?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete note?") },
            text = { Text("“${note.title}” will be removed from your notes.") },
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

@Composable
private fun NotesLibrary(
    notes: List<RevisionNote>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenNote: (RevisionNote) -> Unit,
    onNewNote: () -> Unit,
    onDeleteNote: (RevisionNote) -> Unit
) {
    val filteredNotes = remember(notes, searchQuery) {
        notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.subject.contains(searchQuery, ignoreCase = true) ||
                it.chapter.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search your notes…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BgCardBorder,
                focusedBorderColor = PrimaryLight
            )
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("RECENT NOTES", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
            Spacer(Modifier.weight(1f))
            Text("${filteredNotes.size}", color = TextMuted, fontSize = 11.sp)
        }

        if (filteredNotes.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(BgCardAlt)
                    .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.NoteAdd, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text(if (notes.isEmpty()) "Your notes start here" else "No matching notes", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (notes.isEmpty()) "Capture formulas, ideas and concepts in your own words." else "Try another title, keyword or tag.",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onNewNote) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("New note") }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onOpenNote(note) }, onDelete = { onDeleteNote(note) })
                }
            }
        }

        Button(
            onClick = onNewNote,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("New note", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun NoteCard(note: RevisionNote, onClick: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(JeeSurfaceTokens.borderWidth, BgCardBorder.copy(alpha = JeeSurfaceTokens.cardBorderAlpha), JeeShapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                note.title.ifBlank { "Untitled note" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${note.title}", tint = TextMuted, modifier = Modifier.size(17.dp))
            }
        }
        Text(
            note.content.ifBlank { "Tap to start writing…" },
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        val tag = note.subject.takeIf { it.isNotBlank() && it != "General" }
        if (tag != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.LabelOutline, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(13.dp))
                Text(tag, color = PrimaryLight, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(note.updatedAt)), color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun NoteEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    tag: String,
    onTagClick: () -> Unit,
    tagPickerOpen: Boolean,
    onTagSelected: (String) -> Unit,
    saveStatus: String,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("All notes", color = PrimaryLight) }
            }
            Text(saveStatus, color = TextMuted, fontSize = 11.sp)
        }

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Note title") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = BgCardAlt,
                focusedContainerColor = BgCardAlt,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = PrimaryLight
            )
        )

        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            placeholder = { Text("Start writing…") },
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BgCardBorder,
                focusedBorderColor = PrimaryLight
            )
        )

        if (tagPickerOpen) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCardAlt).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Optional tag", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    noteTags.forEach { value ->
                        FilterChip(
                            selected = tag == value,
                            onClick = { onTagSelected(value) },
                            label = { Text(value, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onTagClick, shape = RoundedCornerShape(28.dp)) {
                Icon(Icons.Filled.LabelOutline, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (tag == "No tag") "Add tag" else "# $tag")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSave, shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save note")
            }
        }
    }
}
