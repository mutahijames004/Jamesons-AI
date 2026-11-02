package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY id DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: Long): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewEpochDay <= :todayEpochDay ORDER BY nextReviewEpochDay ASC")
    fun getCardsDue(todayEpochDay: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY subject ASC, folder ASC, id ASC")
    fun getAllTopics(): Flow<List<TopicChecklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicChecklistEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicChecklistEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicChecklistEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateEpochDay ASC")
    fun getAllTasks(): Flow<List<AcademicTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AcademicTaskEntity): Long

    @Update
    suspend fun updateTask(task: AcademicTaskEntity)

    @Delete
    suspend fun deleteTask(task: AcademicTaskEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY lastModifiedMillis DESC")
    fun getAllNotes(): Flow<List<AcademicNoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): AcademicNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: AcademicNoteEntity): Long

    @Update
    suspend fun updateNote(note: AcademicNoteEntity)

    @Delete
    suspend fun deleteNote(note: AcademicNoteEntity)
}

@Dao
interface StudyLogDao {
    @Query("SELECT * FROM study_logs ORDER BY timestampMillis DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<StudySessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudySessionLogEntity): Long
}
