package konarparti.chats.Db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import konarparti.messenger.DAL.ChatDAO
import konarparti.messenger.DAL.MessageDao
import konarparti.messenger.DAL.MessageEntity

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 2)
abstract class ChatsDatabase : RoomDatabase() {
    abstract fun chatsDAO(): ChatDAO
    abstract fun messagesDAO(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatsDatabase? = null

        fun getDatabase(context: Context): ChatsDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = buildDatabase(context)
                }
            }
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): ChatsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChatsDatabase::class.java,
                "konarpartiChats",
            ).build()
        }
    }
}