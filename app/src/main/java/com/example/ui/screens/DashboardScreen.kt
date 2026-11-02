package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicTaskEntity
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
fun DashboardScreen(
    viewModel: SynapseViewModel,
    onNavigateToRevision: (deckId: Long?) -> Unit,
    onNavigateToAiTutor: () -> Unit,
    onOpenStudyRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dueCards by viewModel.dueCards.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val qotd by viewModel.questionOfTheDay.collectAsState()
    val selectedQotd by viewModel.selectedQotdAnswer.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    ResponsiveContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JAMESON",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = ElectricCyan
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SuccessMint, CircleShape)
                            )
                        }
                        Text(
                            text = "Academic Mastery & Spaced Revision",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Study streak badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔥 5 Day Streak",
                                color = WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.triggerStudyNotification() },
                            modifier = Modifier.testTag("notification_bell_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Test Notification",
                                tint = ElectricCyan
                            )
                        }
                    }
                }
            }

            // Spaced Repetition Due Review Banner
            item {
                SynapseCard(
                    borderColor = ElectricCyan.copy(alpha = 0.4f),
                    backgroundColor = AmoledSurfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ElectricCyan.copy(alpha = 0.08f),
                                        NeonViolet.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(
                                    text = "SUPERMEMO SM-2 ENGINE",
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "${allCards.size} Total Cards",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${dueCards.size} Cards Due for Revision Today",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (dueCards.isNotEmpty())
                                    "Daily active recall keeps your memory retention above 85%."
                                else
                                    "All caught up for today! Review ahead or add new notes.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToRevision(null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("start_quick_review_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = AmoledSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Start Review",
                                        color = AmoledSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = onOpenStudyRoom,
                                    modifier = Modifier.testTag("open_study_room_button"),
                                    border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = NeonViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Study Room", color = NeonViolet)
                                }
                            }
                        }
                    }
                }
            }

            // Exam Countdowns
            if (decks.any { it.examDateEpochDay != null }) {
                item {
                    SectionHeader(
                        title = "Target Exam Countdowns",
                        subtitle = "Days remaining until scheduled academic evaluations"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        decks.filter { it.examDateEpochDay != null }.take(2).forEach { deck ->
                            val daysLeft = (deck.examDateEpochDay!! - viewModel.todayEpochDay).coerceAtLeast(0)
                            SynapseCard(
                                modifier = Modifier.weight(1f),
                                borderColor = AmoledSurfaceBorder
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    StatusBadge(text = deck.subject, color = NeonViolet)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$daysLeft Days",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (daysLeft <= 7) DangerCoral else ElectricCyan
                                        )
                                    )
                                    Text(
                                        text = deck.title,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Challenge Question of the Day
            item {
                SectionHeader(
                    title = "Challenge Question of the Day",
                    subtitle = "Test your academic problem solving"
                )

                if (qotd != null) {
                    val currentQ = qotd!!
                    SynapseCard(
                        borderColor = NeonViolet.copy(alpha = 0.3f),
                        backgroundColor = AmoledSurface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusBadge(text = currentQ.topic, color = NeonViolet)
                                Text(
                                    text = "Daily Challenge",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentQ.question,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            currentQ.options.forEachIndexed { index, option ->
                                val isSelected = selectedQotd == index
                                val isRevealed = selectedQotd != null
                                val isCorrect = index == currentQ.correctIndex

                                val optionBorder = when {
                                    !isRevealed && isSelected -> ElectricCyan
                                    isRevealed && isCorrect -> SuccessMint
                                    isRevealed && isSelected && !isCorrect -> DangerCoral
                                    else -> AmoledSurfaceBorder
                                }

                                val optionBg = when {
                                    isRevealed && isCorrect -> SuccessMint.copy(alpha = 0.12f)
                                    isRevealed && isSelected && !isCorrect -> DangerCoral.copy(alpha = 0.12f)
                                    else -> AmoledSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(optionBg)
                                        .border(BorderStroke(1.dp, optionBorder), RoundedCornerShape(10.dp))
                                        .clickable(enabled = selectedQotd == null) {
                                            viewModel.selectQotdAnswer(index)
                                        }
                                        .padding(12.dp)
                                        .testTag("qotd_option_$index")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${('A' + index)}. ",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRevealed && isCorrect) SuccessMint else ElectricCyan
                                        )
                                        Text(
                                            text = option,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = selectedQotd != null) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    val wasCorrect = selectedQotd == currentQ.correctIndex
                                    Text(
                                        text = if (wasCorrect) "✓ Correct Answer!" else "✗ Incorrect",
                                        fontWeight = FontWeight.Bold,
                                        color = if (wasCorrect) SuccessMint else DangerCoral
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQ.explanation,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Academic Tasks Section
            item {
                SectionHeader(
                    title = "Academic Revision Tasks",
                    subtitle = "Prioritized study goals & assignments",
                    action = {
                        IconButton(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.testTag("add_task_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = ElectricCyan
                            )
                        }
                    }
                )
            }

            if (tasks.isEmpty()) {
                item {
                    SynapseCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No pending tasks. Tap '+' to schedule study milestones.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = { viewModel.toggleTask(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var taskSubject by remember { mutableStateOf("Computer Science") }
        var priority by remember { mutableStateOf("High") }
        var dueDays by remember { mutableStateOf("2") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = AmoledSurface,
            title = {
                Text("New Academic Task", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                    )
                    OutlinedTextField(
                        value = taskSubject,
                        onValueChange = { taskSubject = it },
                        label = { Text("Subject") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDays,
                        onValueChange = { dueDays = it },
                        label = { Text("Days until due") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            val days = dueDays.toIntOrNull() ?: 2
                            viewModel.addTask(taskTitle, taskSubject, priority, days)
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_add_task_button")
                ) {
                    Text("Add Task", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun TaskItemRow(
    task: AcademicTaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    SynapseCard(
        modifier = Modifier.testTag("task_row_${task.id}"),
        borderColor = if (task.isCompleted) AmoledSurfaceBorder.copy(alpha = 0.5f) else AmoledSurfaceBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Toggle Task",
                tint = if (task.isCompleted) SuccessMint else ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.subject,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = task.priority,
                        color = when (task.priority) {
                            "High" -> DangerCoral
                            "Medium" -> WarningAmber
                            else -> ElectricCyan
                        }
                    )
                }
            }
        }
    }
}
