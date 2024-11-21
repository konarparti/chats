package konarparti.chats.Db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val title: String
)

fun String.toChatEntity(): ChatEntity = ChatEntity(this)