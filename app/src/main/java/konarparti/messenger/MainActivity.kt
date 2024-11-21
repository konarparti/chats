package konarparti.messenger

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bumptech.glide.Glide
import konarparti.messenger.DAL.Message
import konarparti.messenger.States.ChatsState
import konarparti.messenger.ViewModel.ChatViewModel
import konarparti.messenger.ViewModel.LoginUiState
import konarparti.messenger.ViewModel.LoginViewModel
import konarparti.messenger.Web.SharedPreferencesHelper
import konarparti.messenger.Web.TokenManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = this
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(this)

        setContent {
            val navController = rememberNavController()
            val viewModel = LoginViewModel(tokenManager, this)

            val startDestination = if (SharedPreferencesHelper.getToken(context) != null) {
                "chatList"
            } else {
                "login"
            }

            NavHost(navController = navController, startDestination) {
                composable("login") {
                    LoginScreen(viewModel, onLoginSuccess = {
                        navController.navigate("chatList") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
                }
                composable("chatList") {
                    val chatViewModel = ChatViewModel(context)
                    LaunchedEffect(Unit) {
                        chatViewModel.loadChats()
                    }
                    ChatListScreen(viewModel = chatViewModel)
                }
                composable("selectedChat") {

                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.login(name, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            is LoginUiState.Loading -> CircularProgressIndicator()
            is LoginUiState.Error -> Text(
                text = (uiState as LoginUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            is LoginUiState.Success -> onLoginSuccess()
            else -> {}
        }
    }
}

@Composable
fun ChatListScreen(viewModel: ChatViewModel) {
    val chatListState = viewModel.chatListState.collectAsState()

    when (val state = chatListState.value) {
        is ChatsState.Loading -> {
            CircularProgressIndicator()
        }
        is ChatsState.Success -> {
            // Отображаем список чатов
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.chats.size) { index ->
                    val chat = state.chats[index]
                    Text(
                        text = chat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Логика выбора чата
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
        is ChatsState.Error -> {
            // Ошибка при получении чатов
            Text("Error: ${state.message}")
        }
    }
}


//@Composable
//fun MessengerApp(viewModel: MessengerViewModel = viewModel()) {
//    val configuration = LocalConfiguration.current
//    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
//
//    val chats by viewModel.chats.collectAsState()
//    val selectedChat by viewModel.selectedChat.collectAsState()
//
//    if (isLandscape) {
//        Row(modifier = Modifier.fillMaxSize()) {
//            ChatList(chats, onChatSelected = { viewModel.selectChat(it) })
//            selectedChat?.let { chat ->
//                ChatScreen(
//                    messages = viewModel.getMessagesForChat(chat),
//                    onMessageSend = { message -> viewModel.sendMessage(chat, message) }
//                )
//            }
//        }
//    } else {
//        if (selectedChat == null) {
//            ChatList(chats, onChatSelected = { viewModel.selectChat(it) })
//        } else {
//            ChatScreen(
//                messages = viewModel.getMessagesForChat(selectedChat!!),
//                onMessageSend = { message -> viewModel.sendMessage(selectedChat!!, message) }
//            )
//        }
//    }
//}

//@Composable
//fun ChatList(chats: List<Chat>, onChatSelected: (Chat) -> Unit) {
//    LazyColumn {
//
//        items(chats.size) { index ->
//            val chat = chats[index]
//            Text(chat.name, modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onChatSelected(chat) }
//                .padding(8.dp))
//        }
//    }
//}

@Composable
fun ChatScreen(messages: List<Message>, onMessageSend: (String) -> Unit) {
    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages.size) { index ->
                val message = messages[index]
                when {
                    message.text != null -> Text(message.text)
                    message.imageLink != null -> GlideImage(message.imageLink)
                }
            }
        }
        TextField(
            value = "",
            onValueChange = { /* handle input */ },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onMessageSend("Your Message") }) {
            Text("Send")
        }
    }
}

@Composable
fun GlideImage(url: String) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                Glide.with(context).load(url).into(this)
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}



