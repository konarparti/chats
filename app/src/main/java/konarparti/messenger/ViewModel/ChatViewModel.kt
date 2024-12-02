package konarparti.messenger.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import konarparti.chats.Db.ChatsDatabase
import konarparti.messenger.R
import konarparti.messenger.Repositories.ChatRepository
import konarparti.messenger.States.ChatsState
import konarparti.messenger.Web.SharedPreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val context: Context) : ViewModel() {

    private val repository = ChatRepository(ChatsDatabase.getDatabase(context).chatsDAO(), ChatsDatabase.getDatabase(context).messagesDAO())

    private val _chatListState = MutableStateFlow<ChatsState>(ChatsState.Loading)
    val chatListState: StateFlow<ChatsState> = _chatListState

    // Метод для загрузки чатов с сервера или из базы данных
    fun loadChats() {
        viewModelScope.launch {
            try {
                when(val chatState = repository.getChats()){
                    is ChatsState.Error -> {
                        val chats = repository.getChatsFromDataBase()
                        _chatListState.value = ChatsState.Success(chats!!)
                    }
                    is ChatsState.Success ->  _chatListState.value = chatState
                    ChatsState.Loading -> TODO()
                }

            } catch (e: Exception){
                   _chatListState.value = ChatsState.Error(context.getString(R.string.no_data_available))
            }
        }
    }
    fun resetState() {
        _chatListState.value = ChatsState.Loading
    }

    fun createChat(chatName: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val username = SharedPreferencesHelper.getLogin(context) ?: "unknown_user"
            val token = SharedPreferencesHelper.getToken(context) ?: "token"
            val message = context.getString(R.string.created_chat_event, username, chatName)

            val result = repository.createChat(chatName, username, message, token)
            if (result) {
                onSuccess("$chatName@channel")
                loadChats()
            } else {
                onError(context.getString(R.string.error_while_creating_chat))
            }
        }
    }
}

