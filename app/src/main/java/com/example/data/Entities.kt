package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val colorHex: String = "#00E5FF",
    val examDateEpochDay: Long? = null,
    val description: String = ""
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val question: String,
    val answer: String,
    val repetitionCount: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewEpochDay: Long,
    val isEncrypted: Boolean = false,
    val tags: String = ""
)

@Entity(tableName = "topics")
data class TopicChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val topicName: String,
    val masteryScore: Int = 0, // 0 - 100
    val lastRevisedEpochDay: Long = 0,
    val nextTestEpochDay: Long = 0,
    val folder: String = "Syllabus",
    val isCompleted: Boolean = false
)

@Entity(tableName = "tasks")
data class AcademicTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val priority: String = "High", // High, Medium, Low
    val dueDateEpochDay: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class AcademicNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val subject: String,
    val folder: String = "Lectures",
    val tags: String = "",
    val isEncrypted: Boolean = false,
    val lastModifiedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_logs")
data class StudySessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val cardsReviewed: Int,
    val correctCount: Int,
    val durationMinutes: Int = 15,
    val retentionRate: Float = 0.85f
)
