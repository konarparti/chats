package konarparti.messenger.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import konarparti.chats.Db.ChatsDatabase
import konarparti.messenger.Base.Chat
import konarparti.messenger.Base.Data
import konarparti.messenger.Base.Message
import konarparti.messenger.Base.Text
import konarparti.messenger.R
import konarparti.messenger.Repositories.ChatRepository
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import konarparti.messenger.Web.SharedPreferencesHelper
import konarparti.messenger.Web.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesViewModel(private val chatId: String, private val context: Context) : ViewModel() {

    private val repository = ChatRepository(ChatsDatabase.getDatabase(context).chatsDAO())
    private var lastMessageId: Int? = null // ID последнего загруженного сообщения
    private var allMessages: MutableList<Message> = mutableListOf() // Все сообщения

    private val _messagesState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val messagesState: StateFlow<ChatListState> = _messagesState

    init {
        getLastMessageIdAndLoadMessages()
    }

    private fun getLastMessageIdAndLoadMessages() {
        viewModelScope.launch {
            try {
                val lastKnownId = repository.getLastMessageId(chatId) ?: 0
                loadMessages(lastKnownId + 1)
            } catch (e: Exception) {
                _messagesState.value = ChatListState.Error("Failed to load messages")
            }
        }
    }

    fun getFirstVisibleMessageId(): Int {
        return allMessages.firstOrNull()?.id ?: 0
    }

    // Метод для загрузки сообщений, начиная с последнего ID
    private suspend fun loadMessages(lastKnownId: Int, limit: Int = 20) {
        try {
            val state = repository.getMessagesFromChat(chatId, lastKnownId = lastKnownId, limit = limit, rev = true)
            if (state is ChatListState.Success) {
                val newMessages = state.chatInfo.messages.reversed()
                if (newMessages.isNotEmpty()) {
                    lastMessageId = newMessages.first().id
                    allMessages.addAll(0, newMessages)
                    _messagesState.value = ChatListState.Success(Chat(chatId, allMessages))
                }
            } else if (state is ChatListState.Error) {
                _messagesState.value = ChatListState.Error(state.message ?: "Unknown Error")
            }
        } catch (e: Exception) {
            _messagesState.value = ChatListState.Error(e.message ?: "Unknown Error")
        }
    }

    // Метод для отправки сообщения
    suspend fun sendMessage(text: String, from: String) {
        val message = Message(
            id = 0, // ID игнорируется сервером
            from = from,
            to = chatId,
            data = Data(Text = Text(text = text)),
            time = System.currentTimeMillis().toString()
        )

        val response = withContext(Dispatchers.IO) {
            val token = SharedPreferencesHelper.getToken(context)
            repository.sendMessage(token!!, message)
        }

        if (response.isSuccessful) {
            lastMessageId = response.body()
            allMessages.add(message)
            _messagesState.value = ChatListState.Success(Chat(chatId, allMessages))
        }
    }

    fun loadMoreMessages(lastKnownId: Int, callBack: () -> Unit) {
        viewModelScope.launch {
            loadMessages(lastKnownId ?: 0, limit = 20)
            callBack()
        }
    }
}


