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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeckEntity
import com.example.data.FlashcardEntity
import com.example.data.TopicChecklistEntity
import com.example.domain.ReviewRating
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
fun RevisionScreen(
    viewModel: SynapseViewModel,
    selectedDeckId: Long?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Flashcards (SM-2)", "Topic Checklists", "Exam Quizzes")

    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val dueCards by viewModel.dueCards.collectAsState()
    val topics by viewModel.topics.collectAsState()

    val activeDeckId by viewModel.activeDeckId.collectAsState()
    val currentCardIndex by viewModel.currentCardIndex.collectAsState()
    val isCardFlipped by viewModel.isCardFlipped.collectAsState()
    val reviewedCount by viewModel.sessionReviewedCount.collectAsState()
    val correctCount by viewModel.sessionCorrectCount.collectAsState()

    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showCreateCardDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }

    // Cards currently in the active review session
    val reviewSessionCards = remember(activeDeckId, allCards, dueCards) {
        if (activeDeckId != null) {
            allCards.filter { it.deckId == activeDeckId }
        } else if (dueCards.isNotEmpty()) {
            dueCards
        } else {
            allCards
        }
    }

    val isStudying = activeDeckId != null || (selectedDeckId != null && reviewSessionCards.isNotEmpty())

    ResponsiveContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tab Navigation Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AmoledSurface,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ElectricCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) ElectricCyan else TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("revision_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Flashcards & SM-2 Spaced Repetition Mode
                    if (activeDeckId != null || reviewSessionCards.isNotEmpty() && currentCardIndex < reviewSessionCards.size) {
                        ActiveSm2StudySession(
                            cards = reviewSessionCards,
                            currentIndex = currentCardIndex,
                            isFlipped = isCardFlipped,
                            onFlip = { viewModel.flipCard() },
                            onRate = { card, rating -> viewModel.submitRating(card, rating) },
                            onExit = { viewModel.startDeckReview(null) },
                            reviewedCount = reviewedCount,
                            correctCount = correctCount
                        )
                    } else {
                        // Decks List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(
                                        title = "Flashcard Decks",
                                        subtitle = "Spaced repetition active recall library"
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showCreateDeckDialog = true },
                                        modifier = Modifier.weight(1f).testTag("create_deck_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = AmoledSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New Deck", color = AmoledSurface, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showCreateCardDialog = true },
                                        modifier = Modifier.weight(1f).testTag("create_card_button"),
                                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Card", color = ElectricCyan)
                                    }
                                }
                            }

                            items(decks, key = { it.id }) { deck ->
                                val deckCards = allCards.filter { it.deckId == deck.id }
                                val dueInDeck = deckCards.count { it.nextReviewEpochDay <= viewModel.todayEpochDay }
                                DeckCardItem(
                                    deck = deck,
                                    cardCount = deckCards.size,
                                    dueCount = dueInDeck,
                                    onStartReview = { viewModel.startDeckReview(deck.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Topic Revision Checklist Mode
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Syllabus Mastery Checklists",
                                subtitle = "Track topic mastery and scheduled re-tests",
                                action = {
                                    IconButton(
                                        onClick = { showAddTopicDialog = true },
                                        modifier = Modifier.testTag("add_topic_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Topic", tint = ElectricCyan)
                                    }
                                }
                            )
                        }

                        items(topics, key = { it.id }) { topic ->
                            TopicChecklistItem(
                                topic = topic,
                                onMasteryChanged = { score -> viewModel.updateTopicMastery(topic, score) }
                            )
                        }
                    }
                }

                2 -> {
                    // Exam Countdown Quizzes & Mock Test
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Timed Mock Quizzes",
                                subtitle = "Simulate exam conditions with randomized flashcard sets"
                            )
                        }
                        item {
                            SynapseCard(
                                borderColor = ElectricCyan.copy(alpha = 0.4f),
                                backgroundColor = AmoledSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    StatusBadge(text = "EXAM PREPARATION", color = ElectricCyan)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Comprehensive 20-Question Exam Sprint",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tests high-priority cards across all active syllabi with instant score analytics.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            viewModel.startDeckReview(null)
                                            selectedTab = 0
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("start_mock_quiz_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AmoledSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Launch Mock Exam Sprint", color = AmoledSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Deck Dialog
    if (showCreateDeckDialog) {
        var deckTitle by remember { mutableStateOf("") }
        var deckSubject by remember { mutableStateOf("Computer Science") }
        var examDays by remember { mutableStateOf("14") }

        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false },
            containerColor = AmoledSurface,
            title = { Text("Create Revision Deck", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = deckTitle,
                        onValueChange = { deckTitle = it },
                        label = { Text("Deck Title (e.g. Operating Systems)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("deck_title_input")
                    )
                    OutlinedTextField(
                        value = deckSubject,
                        onValueChange = { deckSubject = it },
                        label = { Text("Subject") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = examDays,
                        onValueChange = { examDays = it },
                        label = { Text("Days until Exam") },
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
                        if (deckTitle.isNotBlank()) {
                            viewModel.addCustomSubject(deckSubject)
                            viewModel.createDeck(deckTitle, deckSubject, examDays.toIntOrNull() ?: 14)
                            showCreateDeckDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_create_deck_button")
                ) {
                    Text("Create Deck", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDeckDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Create Card Dialog
    if (showCreateCardDialog) {
        var question by remember { mutableStateOf("") }
        var answer by remember { mutableStateOf("") }
        var tags by remember { mutableStateOf("") }
        var isEncrypted by remember { mutableStateOf(false) }
        var selectedDeck by remember { mutableStateOf(decks.firstOrNull()?.id ?: 1L) }

        AlertDialog(
            onDismissRequest = { showCreateCardDialog = false },
            containerColor = AmoledSurface,
            title = { Text("New Spaced Recall Card", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Question / Prompt") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_card_question_input")
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Answer / Derivation") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_card_answer_input"),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated)") },
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
                        if (question.isNotBlank() && answer.isNotBlank()) {
                            viewModel.createFlashcard(selectedDeck, question, answer, tags, isEncrypted)
                            showCreateCardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_create_card_button")
                ) {
                    Text("Add Card", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateCardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog) {
        var topicName by remember { mutableStateOf("") }
        var topicSubject by remember { mutableStateOf("Computer Science") }
        var folder by remember { mutableStateOf("Unit 1") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            containerColor = AmoledSurface,
            title = { Text("Add Syllabus Topic", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = topicName,
                        onValueChange = { topicName = it },
                        label = { Text("Topic Name (e.g. Memory Hierarchy)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("topic_name_input")
                    )
                    OutlinedTextField(
                        value = topicSubject,
                        onValueChange = { topicSubject = it },
                        label = { Text("Subject") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = AmoledSurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("Folder / Module") },
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
                        if (topicName.isNotBlank()) {
                            viewModel.addCustomSubject(topicSubject)
                            viewModel.addTopic(topicSubject, topicName, folder)
                            showAddTopicDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_add_topic_button")
                ) {
                    Text("Save Topic", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTopicDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ActiveSm2StudySession(
    cards: List<FlashcardEntity>,
    currentIndex: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onRate: (FlashcardEntity, ReviewRating) -> Unit,
    onExit: () -> Unit,
    reviewedCount: Int,
    correctCount: Int
) {
    if (cards.isEmpty()) {
        SynapseCard(borderColor = AmoledSurfaceBorder) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Flashcards in this Deck",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Add new cards or scan notes with the AI Tutor to populate this revision deck.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("exit_review_session_button")
                ) {
                    Text("Return to Decks", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    if (currentIndex >= cards.size) {
        // Session Completed State
        SynapseCard(borderColor = SuccessMint) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 Revision Session Complete!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SuccessMint
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You revised $reviewedCount cards with ${((correctCount.toFloat() / reviewedCount.coerceAtLeast(1).toFloat()) * 100).toInt()}% retention.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("exit_review_session_button")
                ) {
                    Text("Return to Decks", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val currentCard = cards[currentIndex]
    val progress = (currentIndex + 1).toFloat() / cards.size.toFloat()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit, modifier = Modifier.testTag("exit_session_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = ElectricCyan)
            }
            Text(
                text = "Card ${currentIndex + 1} of ${cards.size}",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            StatusBadge(
                text = "Rep #${currentCard.repetitionCount}",
                color = ElectricCyan
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ElectricCyan,
            trackColor = AmoledSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3D Flip Card
        SynapseCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onFlip() }
                .testTag("flashcard_flip_target"),
            borderColor = if (isFlipped) SuccessMint.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.5f),
            backgroundColor = AmoledSurfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StatusBadge(
                        text = if (!isFlipped) "PROMPT / QUESTION" else "ANSWER / DERIVATION",
                        color = if (!isFlipped) ElectricCyan else SuccessMint
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (!isFlipped) currentCard.question else currentCard.answer,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (!isFlipped) "Tap card to reveal answer" else "Rate your recall ease below",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SuperMemo SM-2 Rating Bar
        AnimatedVisibility(visible = isFlipped) {
            Column {
                Text(
                    text = "Rate Recall Quality (SuperMemo SM-2)",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onRate(currentCard, ReviewRating.AGAIN) },
                        modifier = Modifier.weight(1f).testTag("sm2_again_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerCoral)
                    ) {
                        Text("Again\n(1d)", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { onRate(currentCard, ReviewRating.HARD) },
                        modifier = Modifier.weight(1f).testTag("sm2_hard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                    ) {
                        Text("Hard\n(1.2x)", color = AmoledSurface, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { onRate(currentCard, ReviewRating.GOOD) },
                        modifier = Modifier.weight(1f).testTag("sm2_good_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Good\n(SM-2)", color = AmoledSurface, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { onRate(currentCard, ReviewRating.EASY) },
                        modifier = Modifier.weight(1f).testTag("sm2_easy_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessMint)
                    ) {
                        Text("Easy\n(Bonus)", color = AmoledSurface, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun DeckCardItem(
    deck: DeckEntity,
    cardCount: Int,
    dueCount: Int,
    onStartReview: () -> Unit
) {
    SynapseCard(
        modifier = Modifier.testTag("deck_item_${deck.id}"),
        borderColor = AmoledSurfaceBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(text = deck.subject, color = ElectricCyan)
                if (dueCount > 0) {
                    StatusBadge(text = "$dueCount DUE TODAY", color = DangerCoral)
                } else {
                    StatusBadge(text = "UP TO DATE", color = SuccessMint)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = deck.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            if (deck.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deck.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$cardCount Flashcards",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Button(
                    onClick = onStartReview,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("review_deck_${deck.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AmoledSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Study Deck", color = AmoledSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TopicChecklistItem(
    topic: TopicChecklistEntity,
    onMasteryChanged: (Int) -> Unit
) {
    SynapseCard(
        modifier = Modifier.testTag("topic_row_${topic.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(text = topic.folder, color = NeonViolet)
                Text(
                    text = "${topic.masteryScore}% Mastery",
                    fontWeight = FontWeight.Bold,
                    color = if (topic.masteryScore >= 80) SuccessMint else WarningAmber,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topic.topicName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = topic.masteryScore.toFloat(),
                onValueChange = { onMasteryChanged(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = AmoledSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("mastery_slider_${topic.id}")
            )
        }
    }
}
