package konarparti.messenger.DAL

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @PrimaryKey val id: String,
    val chatName: String,
    val from: String,
    val to: String,
    val text: String?,
    val imageLink: String?,
    val time: Long
)