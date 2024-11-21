package konarparti.messenger.States

import konarparti.messenger.Base.Chat

sealed class ChatListState : BaseState() {

    data class Error(val message: String?) : ChatListState()

    object Loading : ChatListState()

    data class Success(
        val chatInfo: Chat
    ) : ChatListState()
}