package konarparti.messenger.ViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import konarparti.chats.Db.ChatsDatabase
import konarparti.messenger.Base.Chat
import konarparti.messenger.R
import konarparti.messenger.Repositories.ChatRepository
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessagesViewModel(private val chatId: String, private val context: Context): ViewModel() {

    private val repository = ChatRepository(ChatsDatabase.getDatabase(context).chatsDAO())

    private val _messagesState = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val messagesState: StateFlow<ChatListState> = _messagesState

    fun fetchMessages(callBack: () -> Unit) {
        viewModelScope.launch {
            try {
                when(val messagesState = repository.getMessagesFromChat(chatId)){
                    is ChatListState.Error -> {
                        _messagesState.value = ChatListState.Error(context.getString(R.string.no_data_available))
                    }
                    is ChatListState.Success -> _messagesState.value = messagesState
                    ChatListState.Loading -> TODO()
                }

                //val lastKnownId = st.chatInfo.messages.lastOrNull()?.id

//                val resultList = when (result) {
//                    is ChatListState.Success -> st.chatInfo.messages + result.chatInfo.messages
//                    else -> st.chatInfo.messages
//                }
//
//                setState(ChatListState.Success(Chat(st.chatInfo.title, resultList)))
            } catch (e: Throwable) {
                Log.e("MessagesViewModel", "Error fetching messages", e)
            }
            callBack.invoke()
        }
    }

    fun onScrolled(lastPosition: Int, numberOfElements: Int, callBack: () -> Unit) {
        if (lastPosition >= numberOfElements - MESSAGE_PIVOT) {
            viewModelScope.launch {
                fetchMessages(callBack)
            }
        }
    }

    private companion object {
        const val MESSAGE_PIVOT = 10
    }
}