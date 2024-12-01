package konarparti.messenger.Repositories

import konarparti.chats.Db.toChatEntity
import konarparti.messenger.DAL.ChatDAO
import konarparti.messenger.Base.Chat
import konarparti.messenger.Base.Constants.getServerAPI
import konarparti.messenger.Base.Data
import konarparti.messenger.Base.Image
import konarparti.messenger.Base.Message
import konarparti.messenger.Base.Resource
import konarparti.messenger.Base.Text
import konarparti.messenger.DAL.MessageDao
import konarparti.messenger.DAL.MessageEntity
import konarparti.messenger.R
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ChatRepository(private val database: ChatDAO? = null, private val databaseMessage: MessageDao?) : BaseRepository() {

    private val api = getServerAPI()

    suspend fun getMessagesFromChat(chat: String, lastKnownId: Int = 0, limit: Int = 20, rev: Boolean): ChatListState =
        when (val response = apiCall { api.getMessages(chat, lastKnownId, limit, rev) }) {
            is Resource.Error -> ChatListState.Error(R.string.something_goes_wrong.toString())
            is Resource.Loading -> ChatListState.Loading
            is Resource.Success -> {
                withContext(Dispatchers.IO) {
                    response.data?.forEach {
                        databaseMessage?.insertMessages(konarparti.messenger.DAL.MessageEntity(
                            it.id,
                            chat,
                            it.from,
                            it.to,
                            it.data.Text?.text,
                            it.data.Image?.link,
                            it.time))
                    }
                }
                ChatListState.Success(Chat(chat, response.data!!))
            }
        }

    suspend fun getMessagesFromDatabase(chat: String, lastKnownId: Int = 0, limit: Int = 20, rev: Boolean): ChatListState =
        when (val response = apiCall { api.getMessages(chat, lastKnownId, limit, rev) }) {
            is Resource.Error -> ChatListState.Error(R.string.something_goes_wrong.toString())
            is Resource.Loading -> ChatListState.Loading
            is Resource.Success -> {
                withContext(Dispatchers.IO) {
                    response.data?.forEach {
                        databaseMessage?.insertMessages(
                            MessageEntity(
                            it.id,
                            chat,
                            it.from,
                            it.to,
                            it.data.Text?.text,
                            it.data.Image?.link,
                            it.time)
                        )
                    }
                }
                ChatListState.Success(Chat(chat, response.data!!))
            }
        }

    suspend fun getMessagesFromDatabase(chatId: String): List<Message>? {
        return withContext(Dispatchers.IO) {
            databaseMessage?.getMessages(chatId)?.map { Message(
                it.id,
                it.from,
                it.to,
                Data(Image(it.imageLink?: ""), Text(it.text?: "")),
                it.time) }
        }
    }

    suspend fun getChats(): ChatsState =
        when (val response = apiCall { api.getChannels() }) {
            is Resource.Error -> ChatsState.Error(R.string.something_goes_wrong.toString())
            is Resource.Loading -> ChatsState.Loading
            is Resource.Success -> {
                withContext(Dispatchers.IO) {
                    response.data?.forEach {
                        database?.insertChat(it.toChatEntity())
                    }
                }
                ChatsState.Success(response.data?: emptyList())
            }
        }

    suspend fun getLastMessageId(chatId: String): Int {
        return try {
            val messages = api.getMessages(chatId, Int.MAX_VALUE, 1, true)
            messages.firstOrNull()?.id ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getChatsFromDataBase(): List<String>? {
        return withContext(Dispatchers.IO) {
            database?.getAllChats()?.map { it.title }
        }
    }

    suspend fun sendMessage(token: String, message: Message): Response<Int> {
        return api.sendMessage(token, message)
    }

    suspend fun saveSentMessage(message: Message){
        return withContext(Dispatchers.IO) {
            databaseMessage?.insertMessages(
                MessageEntity(
                    message.id,
                    message.to,
                    message.from,
                    message.to,
                    message.data.Text?.text,
                    message.data.Image?.link,
                    message.time)
            )
        }
    }
}