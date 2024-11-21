package konarparti.messenger.States

sealed class ChatsState : BaseState() {
    data class Error(val message: String?, val fromDb: Boolean = false) : ChatsState()

    object Loading : ChatsState()

    data class Success(
        val chats: List<String>
    ) : ChatsState()
}