package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicNoteEntity
import com.example.domain.ExportEngine
import com.example.ui.components.ResponsiveContainer
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.SynapseCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceBorder
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SynapseViewModel

@Composable
fun VaultNotesScreen(
    viewModel: SynapseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val activeSubject by viewModel.selectedActiveSubject.collectAsState()

    var selectedFolder by remember { mutableStateOf("All") }
    val folders = listOf("All") + notes.map { it.folder }.distinct()

    val filteredNotes = if (selectedFolder == "All") notes else notes.filter { it.folder == selectedFolder }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedNoteForViewing by remember { mutableStateOf<AcademicNoteEntity?>(null) }

    ResponsiveContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Encrypted Vault & Notes",
                    subtitle = "AES-256 GCM encrypted materials, folders & PDF export",
                    action = {
                        IconButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.testTag("add_note_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Note", tint = ElectricCyan)
                        }
                    }
                )
            }

            // Export & Cloud Backup Banner
            item {
                SynapseCard(
                    borderColor = NeonViolet.copy(alpha = 0.4f),
                    backgroundColor = AmoledSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(text = "EXPORT & CLOUD SYNC", color = NeonViolet)
                            IconButton(
                                onClick = { viewModel.triggerCloudBackupSync() },
                                modifier = Modifier.testTag("cloud_backup_sync_button")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Cloud Sync", tint = ElectricCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Export Study Summaries",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate professional on-device PDF revision packages or Markdown files to share.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val pdfFile = viewModel.exportSubjectToPdf(activeSubject)
                                    if (pdfFile != null) {
                                        ExportEngine.shareText(context, "Exported PDF generated at: ${pdfFile.absolutePath}", "Jameson Study Guide PDF")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmoledSurface, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Guide", color = AmoledSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val mdText = viewModel.exportSubjectToMarkdown(activeSubject)
                                    ExportEngine.shareText(context, mdText, "Jameson Study Notes Markdown")
                                },
                                modifier = Modifier.weight(1f).testTag("export_markdown_button"),
                                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Markdown", color = NeonViolet, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Folder Filter Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { f ->
                        FilterChip(
                            selected = selectedFolder == f,
                            onClick = { selectedFolder = f },
                            label = { Text(f, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = AmoledSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (selectedFolder == f) ElectricCyan else AmoledSurfaceBorder)
                        )
                    }
                }
            }

            // Notes List
            if (filteredNotes.isEmpty()) {
                item {
                    SynapseCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No notes in this folder.", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCardItem(
                        note = note,
                        onView = { selectedNoteForViewing = note },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("Computer Science") }
        var folder by remember { mutableStateOf("Lectures") }
        var tags by remember { mutableStateOf("Exam, Core") }
        var encrypt by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            containerColor = AmoledSurface,
            title = { Text("New Academic Study Note", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Note Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content / Derivations / Formulas") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("note_content_input"),
                        minLines = 4
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = AmoledSurfaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = folder,
                            onValueChange = { folder = it },
                            label = { Text("Folder") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = AmoledSurfaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // AES-256 Encryption Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AES-256 Encryption", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                            Text("Encrypt at rest in local vault", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = encrypt,
                            onCheckedChange = { encrypt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("encrypt_note_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            viewModel.addNote(title, content, subject, folder, tags, encrypt)
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_add_note_button")
                ) {
                    Text("Save Note", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // View Note Dialog
    if (selectedNoteForViewing != null) {
        val note = selectedNoteForViewing!!
        val plainContent = viewModel.decryptNote(note)

        AlertDialog(
            onDismissRequest = { selectedNoteForViewing = null },
            containerColor = AmoledSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    if (note.isEncrypted) {
                        StatusBadge(text = "AES-256 SECURED", color = SuccessMint)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Folder: ${note.folder} • Subject: ${note.subject}", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plainContent,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 20.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNoteForViewing = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Done", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: AcademicNoteEntity,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    SynapseCard(
        modifier = Modifier.clickable { onView() }.testTag("note_item_${note.id}"),
        borderColor = if (note.isEncrypted) NeonViolet.copy(alpha = 0.4f) else AmoledSurfaceBorder
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (note.isEncrypted) Icons.Default.Lock else Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (note.isEncrypted) SuccessMint else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${note.subject} / ${note.folder}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (note.isEncrypted) "••••••••••• [AES-256 GCM Encrypted Vault Payload] •••••••••••" else note.content,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 2
            )
        }
    }
}
