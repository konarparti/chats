package konarparti.messenger.DAL

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val chatName: String,
    val from: String,
    val to: String,
    val text: String?,
    val imageLink: String?,
    val time: String
)

fun String.toMessageEntity(message: MessageEntity): MessageEntity = MessageEntity(
    message.id,
    message.chatName,
    message.from,
    message.to,
    message.text,
    message.imageLink,
    message.time)