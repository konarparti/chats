package konarparti.messenger.DAL

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // Метод для вставки списка сообщений
    @Insert
    suspend fun insertMessages(messages: List<Message>)

    // Метод для получения всех сообщений для чата
    @Query("SELECT * FROM Message WHERE chatName = :chatName")
    fun getMessages(chatName: String): Flow<List<Message>>  // Используем Flow для асинхронных запросов
}
