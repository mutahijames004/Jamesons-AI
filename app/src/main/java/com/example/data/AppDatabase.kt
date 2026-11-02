package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        TopicChecklistEntity::class,
        AcademicTaskEntity::class,
        AcademicNoteEntity::class,
        StudySessionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun topicDao(): TopicDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "synapse_academic.db"
                ).build()
                INSTANCE = instance

                // Ensure initial starter academic decks, flashcards & topics are populated on fresh device installs
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (instance.deckDao().getDeckCount() == 0) {
                            seedDatabase(instance)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                instance
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val today = LocalDate.now().toEpochDay()

            // Seed Deck 1: Computer Science - Algorithms & Complexity
            val csDeckId = db.deckDao().insertDeck(
                DeckEntity(
                    title = "Data Structures & Algorithms",
                    subject = "Computer Science",
                    colorHex = "#00E5FF",
                    examDateEpochDay = today + 14,
                    description = "Midterm revision on trees, graph theory, dynamic programming, and asymptotic bounds."
                )
            )

            db.flashcardDao().insertCards(
                listOf(
                    FlashcardEntity(
                        deckId = csDeckId,
                        question = "What is the time complexity of QuickSelect in the average vs worst case?",
                        answer = "Average Case: O(N)\nWorst Case: O(N²) when poor pivots are repeatedly chosen.",
                        repetitionCount = 1,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        nextReviewEpochDay = today,
                        tags = "Algorithms, Sorting"
                    ),
                    FlashcardEntity(
                        deckId = csDeckId,
                        question = "Explain the difference between Dijkstra's algorithm and the Bellman-Ford algorithm.",
                        answer = "Dijkstra computes single-source shortest paths on graphs with non-negative edge weights in O((V + E) log V).\nBellman-Ford handles negative edge weights and detects negative cycles in O(V * E).",
                        repetitionCount = 2,
                        intervalDays = 3,
                        easeFactor = 2.4f,
                        nextReviewEpochDay = today,
                        tags = "Graphs"
                    ),
                    FlashcardEntity(
                        deckId = csDeckId,
                        question = "What are the two key properties required for a problem to be solved using Dynamic Programming?",
                        answer = "1. Optimal Substructure: Optimal solution contains optimal solutions to subproblems.\n2. Overlapping Subproblems: The same subproblems are solved multiple times.",
                        repetitionCount = 3,
                        intervalDays = 6,
                        easeFactor = 2.6f,
                        nextReviewEpochDay = today + 1,
                        tags = "Dynamic Programming"
                    )
                )
            )

            // Seed Deck 2: Physics - Electromagnetism & Thermodynamics
            val physDeckId = db.deckDao().insertDeck(
                DeckEntity(
                    title = "Electromagnetism & Optics",
                    subject = "Physics",
                    colorHex = "#7C4DFF",
                    examDateEpochDay = today + 21,
                    description = "Maxwell's equations, wave propagation, and electromagnetic induction."
                )
            )

            db.flashcardDao().insertCards(
                listOf(
                    FlashcardEntity(
                        deckId = physDeckId,
                        question = "State Faraday's Law of Electromagnetic Induction.",
                        answer = "The induced electromotive force (EMF) in any closed circuit is equal to the negative rate of change of magnetic flux through the circuit: ε = -dΦB/dt.",
                        repetitionCount = 1,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        nextReviewEpochDay = today,
                        tags = "Physics, Maxwell"
                    ),
                    FlashcardEntity(
                        deckId = physDeckId,
                        question = "What does the Poynting vector represent and what are its SI units?",
                        answer = "The Poynting vector S = (1/μ₀) * (E × B) represents the directional energy flux density (rate of energy transfer per unit area) of an electromagnetic field. Units: Watts/m².",
                        repetitionCount = 0,
                        intervalDays = 1,
                        easeFactor = 2.5f,
                        nextReviewEpochDay = today,
                        tags = "Waves"
                    )
                )
            )

            // Seed Topics & Checklists
            db.topicDao().insertTopic(
                TopicChecklistEntity(
                    subject = "Computer Science",
                    topicName = "Asymptotic Notation & Recurrences",
                    masteryScore = 85,
                    lastRevisedEpochDay = today - 3,
                    nextTestEpochDay = today + 2,
                    folder = "Unit 1: Foundations",
                    isCompleted = true
                )
            )
            db.topicDao().insertTopic(
                TopicChecklistEntity(
                    subject = "Computer Science",
                    topicName = "Red-Black Trees & AVL Trees",
                    masteryScore = 60,
                    lastRevisedEpochDay = today - 1,
                    nextTestEpochDay = today,
                    folder = "Unit 2: Trees",
                    isCompleted = false
                )
            )
            db.topicDao().insertTopic(
                TopicChecklistEntity(
                    subject = "Physics",
                    topicName = "Gauss's Law for Magnetism & Monopoles",
                    masteryScore = 70,
                    lastRevisedEpochDay = today - 4,
                    nextTestEpochDay = today + 3,
                    folder = "Electrodynamics",
                    isCompleted = false
                )
            )

            // Seed Academic Tasks
            db.taskDao().insertTask(
                AcademicTaskEntity(
                    title = "Solve Problem Set 4: Dynamic Programming",
                    description = "Focus on knapsack problem variations and matrix chain multiplication proofs.",
                    subject = "Computer Science",
                    priority = "High",
                    dueDateEpochDay = today + 2,
                    isCompleted = false
                )
            )
            db.taskDao().insertTask(
                AcademicTaskEntity(
                    title = "Review Lab Report: Hall Effect Measurement",
                    description = "Verify uncertainty propagation calculations and plot magnetic field vs Hall voltage.",
                    subject = "Physics",
                    priority = "Medium",
                    dueDateEpochDay = today + 4,
                    isCompleted = false
                )
            )

            // Seed Notes Vault
            db.noteDao().insertNote(
                AcademicNoteEntity(
                    title = "Master Theorem Cheat Sheet",
                    content = "# Master Theorem for Divide-and-Conquer Recurrences\nT(n) = a*T(n/b) + f(n)\n\nCase 1: If f(n) = O(n^(log_b(a) - ε)), then T(n) = Θ(n^(log_b(a))).\nCase 2: If f(n) = Θ(n^(log_b(a)) * log^k(n)), then T(n) = Θ(n^(log_b(a)) * log^(k+1)(n)).\nCase 3: If f(n) = Ω(n^(log_b(a) + ε)) and regularity condition holds, then T(n) = Θ(f(n)).",
                    subject = "Computer Science",
                    folder = "Algorithms",
                    tags = "CheatSheet, Recurrences, Complexity",
                    isEncrypted = false
                )
            )

            // Seed Study Logs for chart history
            db.studyLogDao().insertLog(
                StudySessionLogEntity(
                    timestampMillis = System.currentTimeMillis() - 86400000L * 3,
                    cardsReviewed = 18,
                    correctCount = 16,
                    durationMinutes = 25,
                    retentionRate = 0.88f
                )
            )
            db.studyLogDao().insertLog(
                StudySessionLogEntity(
                    timestampMillis = System.currentTimeMillis() - 86400000L * 2,
                    cardsReviewed = 24,
                    correctCount = 21,
                    durationMinutes = 35,
                    retentionRate = 0.87f
                )
            )
            db.studyLogDao().insertLog(
                StudySessionLogEntity(
                    timestampMillis = System.currentTimeMillis() - 86400000L * 1,
                    cardsReviewed = 30,
                    correctCount = 28,
                    durationMinutes = 40,
                    retentionRate = 0.93f
                )
            )
        }
    }
}
