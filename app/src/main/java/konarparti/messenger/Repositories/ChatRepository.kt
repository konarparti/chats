package konarparti.messenger.Repositories

import android.util.Log
import konarparti.chats.Db.toChatEntity
import konarparti.messenger.DAL.ChatDAO
import konarparti.messenger.Base.Chat
import konarparti.messenger.Base.Constants.getServerAPI
import konarparti.messenger.Base.Message
import konarparti.messenger.Base.Resource
import konarparti.messenger.R
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ChatRepository(private val database: ChatDAO? = null) : BaseRepository() {

    private val api = getServerAPI()

    suspend fun getMessagesFromChat(chat: String, lastKnownId: Int = 0, limit: Int = 20, rev: Boolean): ChatListState =
        when (val response = apiCall { api.getMessages(chat, lastKnownId, limit, rev) }) {
            is Resource.Error -> ChatListState.Error(R.string.something_goes_wrong.toString())
            is Resource.Loading -> ChatListState.Loading
            is Resource.Success -> {
                ChatListState.Success(Chat(chat, response.data!!))
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

    suspend fun getMessagesFromDataBase(): List<String>? {
        return withContext(Dispatchers.IO) {
            database?.getAllChats()?.map { it.title }
        }
    }

    suspend fun sendMessage(token: String, message: Message): Response<Int> {
        return api.sendMessage(token, message)
    }
}