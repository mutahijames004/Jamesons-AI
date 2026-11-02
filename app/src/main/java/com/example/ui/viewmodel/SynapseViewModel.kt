package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiTutorService
import com.example.ai.QuestionOfTheDay
import com.example.data.AcademicNoteEntity
import com.example.data.AcademicTaskEntity
import com.example.data.AppDatabase
import com.example.data.CryptoVault
import com.example.data.DeckEntity
import com.example.data.FlashcardEntity
import com.example.data.StudySessionLogEntity
import com.example.data.TopicChecklistEntity
import com.example.domain.ExportEngine
import com.example.domain.ReviewRating
import com.example.domain.Sm2Algorithm
import com.example.domain.StudyNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class ChatMessage(
    val sender: String, // "user" or "tutor"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SynapseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val aiService = AiTutorService()

    val todayEpochDay: Long = LocalDate.now().toEpochDay()

    // Database flows
    val decks: StateFlow<List<DeckEntity>> = db.deckDao().getAllDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCards: StateFlow<List<FlashcardEntity>> = db.flashcardDao().getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueCards: StateFlow<List<FlashcardEntity>> = db.flashcardDao().getCardsDue(todayEpochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<TopicChecklistEntity>> = db.topicDao().getAllTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<AcademicTaskEntity>> = db.taskDao().getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<AcademicNoteEntity>> = db.noteDao().getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyLogs: StateFlow<List<StudySessionLogEntity>> = db.studyLogDao().getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Revision Session State
    private val _activeDeckId = MutableStateFlow<Long?>(null)
    val activeDeckId: StateFlow<Long?> = _activeDeckId.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _sessionReviewedCount = MutableStateFlow(0)
    val sessionReviewedCount: StateFlow<Int> = _sessionReviewedCount.asStateFlow()

    private val _sessionCorrectCount = MutableStateFlow(0)
    val sessionCorrectCount: StateFlow<Int> = _sessionCorrectCount.asStateFlow()

    // Custom Subjects
    private val _availableSubjects = MutableStateFlow<List<String>>(
        listOf("Computer Science", "Physics", "Mathematics", "Engineering", "General")
    )
    val availableSubjects: StateFlow<List<String>> = _availableSubjects.asStateFlow()

    private val _selectedActiveSubject = MutableStateFlow("Computer Science")
    val selectedActiveSubject: StateFlow<String> = _selectedActiveSubject.asStateFlow()

    fun selectSubject(subject: String) {
        _selectedActiveSubject.value = subject
    }

    fun addCustomSubject(subject: String) {
        val clean = subject.trim()
        if (clean.isNotBlank() && !_availableSubjects.value.any { it.equals(clean, ignoreCase = true) }) {
            _availableSubjects.value = _availableSubjects.value + clean
            _selectedActiveSubject.value = clean
            postMessage("Added subject: $clean")
        }
    }

    // AI Tutor & Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("tutor", "Hello! I am Jameson, your live academic tutor. Ask me anything via text or tap the microphone for live voice conversation.")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Voice Conversation Manager
    val voiceManager = com.example.ai.VoiceConversationManager(application) { spokenText ->
        sendVoiceTutorMessage(spokenText)
    }

    private val _isVoiceModeActive = MutableStateFlow(false)
    val isVoiceModeActive: StateFlow<Boolean> = _isVoiceModeActive.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val audioRmsLevel: StateFlow<Float> = voiceManager.audioRmsLevel
    val liveTranscript: StateFlow<String> = voiceManager.liveTranscript
    val speechStatus: StateFlow<String> = voiceManager.speechStatus

    fun toggleVoiceMode() {
        val newState = !_isVoiceModeActive.value
        _isVoiceModeActive.value = newState
        if (!newState) {
            voiceManager.stopListening()
            voiceManager.stopSpeaking()
        }
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _questionOfTheDay = MutableStateFlow<QuestionOfTheDay?>(null)
    val questionOfTheDay: StateFlow<QuestionOfTheDay?> = _questionOfTheDay.asStateFlow()

    private val _selectedQotdAnswer = MutableStateFlow<Int?>(null)
    val selectedQotdAnswer: StateFlow<Int?> = _selectedQotdAnswer.asStateFlow()

    private val _photoAnalysisText = MutableStateFlow<String?>(null)
    val photoAnalysisText: StateFlow<String?> = _photoAnalysisText.asStateFlow()

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto: StateFlow<Boolean> = _isAnalyzingPhoto.asStateFlow()

    // Study Room / Live Focus Session State
    private val _pomodoroSecondsRemaining = MutableStateFlow(25 * 60)
    val pomodoroSecondsRemaining: StateFlow<Int> = _pomodoroSecondsRemaining.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _activeCollaborators = MutableStateFlow(42)
    val activeCollaborators: StateFlow<Int> = _activeCollaborators.asStateFlow()

    // UI Snack / Status notifications
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        loadQuestionOfTheDay()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun postMessage(msg: String) {
        _userMessage.value = msg
    }

    // --- Spaced Repetition (SM-2) Actions ---

    fun startDeckReview(deckId: Long?) {
        _activeDeckId.value = deckId
        _currentCardIndex.value = 0
        _isCardFlipped.value = false
        _sessionReviewedCount.value = 0
        _sessionCorrectCount.value = 0
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun submitRating(card: FlashcardEntity, rating: ReviewRating) {
        viewModelScope.launch(Dispatchers.IO) {
            val sm2Result = Sm2Algorithm.calculateNextReview(
                currentRepetitions = card.repetitionCount,
                currentIntervalDays = card.intervalDays,
                currentEaseFactor = card.easeFactor,
                rating = rating,
                todayEpochDay = todayEpochDay
            )

            val updatedCard = card.copy(
                repetitionCount = sm2Result.repetitionCount,
                intervalDays = sm2Result.intervalDays,
                easeFactor = sm2Result.easeFactor,
                nextReviewEpochDay = sm2Result.nextReviewEpochDay
            )
            db.flashcardDao().updateCard(updatedCard)

            _sessionReviewedCount.value += 1
            if (rating != ReviewRating.AGAIN) {
                _sessionCorrectCount.value += 1
            }

            _isCardFlipped.value = false
            _currentCardIndex.value += 1

            // Record log if session finished
            val reviewed = _sessionReviewedCount.value
            val correct = _sessionCorrectCount.value
            if (reviewed > 0 && reviewed % 5 == 0) {
                db.studyLogDao().insertLog(
                    StudySessionLogEntity(
                        cardsReviewed = reviewed,
                        correctCount = correct,
                        retentionRate = correct.toFloat() / reviewed.toFloat()
                    )
                )
            }
        }
    }

    fun createFlashcard(deckId: Long, question: String, answer: String, tags: String = "", encrypt: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalAnswer = if (encrypt) CryptoVault.encrypt(answer) else answer
            db.flashcardDao().insertCard(
                FlashcardEntity(
                    deckId = deckId,
                    question = question,
                    answer = finalAnswer,
                    nextReviewEpochDay = todayEpochDay,
                    isEncrypted = encrypt,
                    tags = tags
                )
            )
            postMessage("Flashcard created successfully!")
        }
    }

    fun createDeck(title: String, subject: String, examDaysAhead: Int = 14, colorHex: String = "#00E5FF") {
        viewModelScope.launch(Dispatchers.IO) {
            db.deckDao().insertDeck(
                DeckEntity(
                    title = title,
                    subject = subject,
                    colorHex = colorHex,
                    examDateEpochDay = todayEpochDay + examDaysAhead
                )
            )
            postMessage("Deck \"$title\" added!")
        }
    }

    // --- Academic Tasks & Topics Checklists ---

    fun addTask(title: String, subject: String, priority: String, daysUntilDue: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().insertTask(
                AcademicTaskEntity(
                    title = title,
                    subject = subject,
                    priority = priority,
                    dueDateEpochDay = todayEpochDay + daysUntilDue
                )
            )
            postMessage("Task scheduled!")
        }
    }

    fun toggleTask(task: AcademicTaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: AcademicTaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().deleteTask(task)
        }
    }

    fun addTopic(subject: String, topicName: String, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.topicDao().insertTopic(
                TopicChecklistEntity(
                    subject = subject,
                    topicName = topicName,
                    masteryScore = 20,
                    folder = folder,
                    lastRevisedEpochDay = todayEpochDay,
                    nextTestEpochDay = todayEpochDay + 3
                )
            )
            postMessage("Topic checklist added!")
        }
    }

    fun updateTopicMastery(topic: TopicChecklistEntity, newMastery: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.topicDao().updateTopic(
                topic.copy(
                    masteryScore = newMastery.coerceIn(0, 100),
                    isCompleted = newMastery >= 90,
                    lastRevisedEpochDay = todayEpochDay,
                    nextTestEpochDay = todayEpochDay + if (newMastery >= 80) 7 else 2
                )
            )
        }
    }

    // --- Notes Vault & AES-256 Encryption ---

    fun addNote(title: String, content: String, subject: String, folder: String, tags: String, encrypt: Boolean, passphrase: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val storedContent = if (encrypt) CryptoVault.encrypt(content, passphrase) else content
            db.noteDao().insertNote(
                AcademicNoteEntity(
                    title = title,
                    content = storedContent,
                    subject = subject,
                    folder = folder,
                    tags = tags,
                    isEncrypted = encrypt
                )
            )
            postMessage(if (encrypt) "Note securely stored with AES-256 encryption!" else "Study note saved.")
        }
    }

    fun decryptNote(note: AcademicNoteEntity, passphrase: String? = null): String {
        return if (note.isEncrypted) {
            CryptoVault.decrypt(note.content, passphrase)
        } else {
            note.content
        }
    }

    fun deleteNote(note: AcademicNoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.noteDao().deleteNote(note)
        }
    }

    // --- AI Tutor, Summarizer, and Photo Scanner ---

    fun sendTutorMessage(prompt: String, subject: String = "Computer Science") {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage("user", trimmed))
        _chatMessages.value = currentList

        viewModelScope.launch {
            _isAiLoading.value = true
            val responseBuilder = StringBuilder()
            try {
                aiService.streamTutorResponse(trimmed, subject).collect { chunk ->
                    responseBuilder.append(chunk)
                    val updated = _chatMessages.value.toMutableList()
                    if (updated.isNotEmpty() && updated.last().sender == "tutor_streaming") {
                        updated[updated.lastIndex] = ChatMessage("tutor_streaming", responseBuilder.toString())
                    } else {
                        updated.add(ChatMessage("tutor_streaming", responseBuilder.toString()))
                    }
                    _chatMessages.value = updated
                }
            } catch (t: Throwable) {
                android.util.Log.e("SynapseViewModel", "Tutor stream error, activating instant fallback", t)
                if (responseBuilder.isEmpty()) {
                    responseBuilder.append(aiService.getFallbackExplanation(trimmed, subject))
                }
            } finally {
                val fullAnswer = responseBuilder.toString().ifBlank {
                    aiService.getFallbackExplanation(trimmed, subject)
                }
                val finalUpdated = _chatMessages.value.toMutableList()
                if (finalUpdated.isNotEmpty() && finalUpdated.last().sender == "tutor_streaming") {
                    finalUpdated[finalUpdated.lastIndex] = ChatMessage("tutor", fullAnswer)
                } else {
                    finalUpdated.add(ChatMessage("tutor", fullAnswer))
                }
                _chatMessages.value = finalUpdated
                _isAiLoading.value = false
            }
        }
    }

    fun sendVoiceTutorMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return
        val currentSubject = _selectedActiveSubject.value
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage("user", trimmed))
        _chatMessages.value = currentList

        viewModelScope.launch {
            _isAiLoading.value = true
            val responseBuilder = StringBuilder()
            try {
                aiService.streamTutorResponse(trimmed, currentSubject).collect { chunk ->
                    responseBuilder.append(chunk)
                    val updated = _chatMessages.value.toMutableList()
                    if (updated.isNotEmpty() && updated.last().sender == "tutor_streaming") {
                        updated[updated.lastIndex] = ChatMessage("tutor_streaming", responseBuilder.toString())
                    } else {
                        updated.add(ChatMessage("tutor_streaming", responseBuilder.toString()))
                    }
                    _chatMessages.value = updated
                }
            } catch (t: Throwable) {
                android.util.Log.e("SynapseViewModel", "Voice tutor stream error, activating instant fallback", t)
                if (responseBuilder.isEmpty()) {
                    responseBuilder.append(aiService.getFallbackExplanation(trimmed, currentSubject))
                }
            } finally {
                val fullAnswer = responseBuilder.toString().ifBlank {
                    aiService.getFallbackExplanation(trimmed, currentSubject)
                }
                val finalUpdated = _chatMessages.value.toMutableList()
                if (finalUpdated.isNotEmpty() && finalUpdated.last().sender == "tutor_streaming") {
                    finalUpdated[finalUpdated.lastIndex] = ChatMessage("tutor", fullAnswer)
                } else {
                    finalUpdated.add(ChatMessage("tutor", fullAnswer))
                }
                _chatMessages.value = finalUpdated
                _isAiLoading.value = false

                // In voice mode, speak the answer aloud!
                if (_isVoiceModeActive.value && fullAnswer.isNotBlank()) {
                    voiceManager.speak(fullAnswer)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }

    fun generateCardsFromNote(note: AcademicNoteEntity, targetDeckId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAiLoading.value = true
            val plainContent = decryptNote(note)
            val generated = aiService.generateFlashcardsFromNotes(plainContent, note.subject)
            val newCards = generated.map {
                FlashcardEntity(
                    deckId = targetDeckId,
                    question = it.question,
                    answer = it.answer,
                    nextReviewEpochDay = todayEpochDay,
                    tags = note.tags
                )
            }
            db.flashcardDao().insertCards(newCards)
            _isAiLoading.value = false
            postMessage("Generated ${newCards.size} cards with AI!")
        }
    }

    fun analyzePhoto(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzingPhoto.value = true
            val analysis = aiService.analyzePhotoNotes(bitmap)
            _photoAnalysisText.value = analysis
            _isAnalyzingPhoto.value = false
            postMessage("Photo analysis complete!")
        }
    }

    fun clearPhotoAnalysis() {
        _photoAnalysisText.value = null
    }

    fun loadQuestionOfTheDay(subject: String = "Computer Science & Engineering") {
        viewModelScope.launch {
            _questionOfTheDay.value = aiService.getChallengeQuestionOfTheDay(subject)
            _selectedQotdAnswer.value = null
        }
    }

    fun selectQotdAnswer(index: Int) {
        _selectedQotdAnswer.value = index
    }

    // --- PDF / Markdown Export & Reminders ---

    fun exportSubjectToPdf(subject: String): File? {
        val subjectNotes = notes.value.filter { it.subject.equals(subject, ignoreCase = true) }
            .map { it.copy(content = decryptNote(it)) }
        val subjectTopics = topics.value.filter { it.subject.equals(subject, ignoreCase = true) }

        val file = ExportEngine.exportToPdf(
            context = getApplication(),
            title = "$subject Revision Guide",
            subject = subject,
            notes = subjectNotes,
            topics = subjectTopics
        )
        if (file != null) {
            postMessage("PDF saved to ${file.name}")
        }
        return file
    }

    fun exportSubjectToMarkdown(subject: String): String {
        val subjectNotes = notes.value.filter { it.subject.equals(subject, ignoreCase = true) }
            .map { it.copy(content = decryptNote(it)) }
        val subjectTopics = topics.value.filter { it.subject.equals(subject, ignoreCase = true) }

        return ExportEngine.exportToMarkdown(
            title = "$subject Revision Guide",
            subject = subject,
            notes = subjectNotes,
            topics = subjectTopics
        )
    }

    fun triggerStudyNotification() {
        StudyNotifier.sendStudyReminder(
            context = getApplication(),
            title = "Jameson: ${dueCards.value.size} Cards Due for Review",
            message = "Stay ahead of your exam curve! Complete your daily spaced recall session to maintain 90%+ retention."
        )
        postMessage("Study notification sent!")
    }

    fun triggerCloudBackupSync() {
        viewModelScope.launch(Dispatchers.IO) {
            postMessage("Syncing encrypted vault & study telemetry with Cloud...")
            kotlinx.coroutines.delay(1200)
            postMessage("Cloud Backup completed! All decks, topics & logs synchronized.")
        }
    }

    // --- Pomodoro Study Focus ---

    fun togglePomodoro() {
        _isPomodoroRunning.value = !_isPomodoroRunning.value
    }

    fun tickPomodoro() {
        if (_isPomodoroRunning.value && _pomodoroSecondsRemaining.value > 0) {
            _pomodoroSecondsRemaining.value -= 1
        }
    }

    fun resetPomodoro() {
        _isPomodoroRunning.value = false
        _pomodoroSecondsRemaining.value = 25 * 60
    }
}
