package konarparti.messenger.DAL

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insertMessages(messages: List<Message>)

    @Query("SELECT * FROM Message WHERE chatName = :chatName")
    fun getMessages(chatName: String): Flow<List<Message>>
}
