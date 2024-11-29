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
import konarparti.messenger.Web.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesViewModel(private val chatId: String, private val context: Context) : ViewModel() {

    private val repository = ChatRepository(ChatsDatabase.getDatabase(context).chatsDAO(), ChatsDatabase.getDatabase(context).messagesDAO())
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
                _messagesState.value = ChatListState.Error(context.getString(R.string.failed_to_load_messages))
            }
        }
    }

    fun getFirstVisibleMessageId(): Int {
        return allMessages.lastOrNull()?.id ?: 0
    }

    private suspend fun loadMessages(lastKnownId: Int, limit: Int = 20) : List<Message> {
        try {
            val state = repository.getMessagesFromChat(chatId, lastKnownId = lastKnownId, limit = limit, rev = true)
            if (state is ChatListState.Success) {
                val newMessages = state.chatInfo.messages
                if (newMessages.isNotEmpty()) {
                    allMessages.addAll(allMessages.size, newMessages)
                    _messagesState.value = ChatListState.Success(Chat(chatId, allMessages))
                    return newMessages
                }
            } else if (state is ChatListState.Error) {
                if(allMessages.none { m -> m.id == lastKnownId}){
                    val mes = repository.getMessagesFromDatabase(chatId)
                    allMessages.addAll(allMessages.size, mes?: emptyList())
                    _messagesState.value = ChatListState.Success(Chat(chatId, allMessages))
                    return mes?: emptyList()
                }

            }
        } catch (e: Exception) {
            _messagesState.value = ChatListState.Error(e.message ?: context.getString(R.string.unknown_error))
        }
        return emptyList()
    }

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
            val messageId = response.body()
            val newMessage = message.copy(id = messageId ?: 0)
            allMessages.add(0, newMessage)

            // Обновляем состояние
            _messagesState.value = ChatListState.Success(Chat(chatId, allMessages))
        } else {
            _messagesState.value = ChatListState.Error("Не удалось отправить сообщение")
        }
    }


    fun loadMoreMessages(lastKnownId: Int, callBack: (List<Message>) -> Unit) {
        viewModelScope.launch {
            val list = loadMessages(lastKnownId ?: 0, limit = 20)
            callBack(list)

        }

    }

}


