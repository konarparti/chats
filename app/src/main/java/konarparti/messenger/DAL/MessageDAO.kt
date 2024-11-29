package konarparti.messenger.DAL

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatName = :chatName ORDER BY id DESC")
    fun getMessages(chatName: String): List<MessageEntity>
}
