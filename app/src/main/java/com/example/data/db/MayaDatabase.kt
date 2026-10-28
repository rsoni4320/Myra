package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryItem
import com.example.data.model.Note
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface MayaDao {
    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    // Chat Sessions
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()

    // Notes
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // User Memory (User-controlled memory store)
    @Query("SELECT * FROM user_memory ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItem): Long

    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM user_memory")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM user_memory WHERE fact LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryItem>>
}

@Database(
    entities = [ChatMessage::class, ChatSession::class, Reminder::class, Note::class, MemoryItem::class],
    version = 2,
    exportSchema = false
)
abstract class MayaDatabase : RoomDatabase() {
    abstract fun mayaDao(): MayaDao
}
