package konarparti.messenger

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bumptech.glide.Glide
import konarparti.messenger.DAL.Message
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import konarparti.messenger.ViewModel.ChatViewModel
import konarparti.messenger.ViewModel.LoginUiState
import konarparti.messenger.ViewModel.LoginViewModel
import konarparti.messenger.ViewModel.MessagesViewModel
import konarparti.messenger.Web.RetrofitInstance
import konarparti.messenger.Web.SharedPreferencesHelper
import konarparti.messenger.Web.TokenManager
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = this
        super.onCreate(savedInstanceState)
        val tokenManager = TokenManager(this)
        RetrofitInstance.initialize(this)

        setContent {
            val navController = rememberNavController()
            val viewModel = LoginViewModel(tokenManager, context)
            val chatViewModel = ChatViewModel(context)

            val startDestination = if (SharedPreferencesHelper.getToken(context) != null) {
                "chatList"
            } else {
                "login"
            }

            BackHandler(enabled = true) {
                if (navController.currentBackStackEntry?.destination?.route == "chatList") {
                    viewModel.logout()
                    chatViewModel.resetState();
                    navController.navigate("login") {}
                } else {
                    navController.popBackStack()
                }
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
                    LaunchedEffect(Unit) {
                        chatViewModel.loadChats()
                    }
                    ChatListScreen(viewModel = chatViewModel, onChatClick = { chatId ->
                        navController.navigate("messages/$chatId/0")
                    })
                }
                composable(
                    route = "messages/{chatId}/{lastKnownId}",
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("lastKnownId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                    val lastKnownId = backStackEntry.arguments?.getInt("lastKnownId") ?: 0
                    val messagesViewModel = MessagesViewModel(chatId, context)

                    MessagesScreen(
                        viewModel = messagesViewModel,
                        onImageClick = { imageUrl ->
                            navController.navigate("imageView/${Uri.encode(imageUrl)}")
                        }
                    )
                }

                composable(
                    route = "imageView/{imageUrl}",
                    arguments = listOf(navArgument("imageUrl") { type = NavType.StringType })
                ) { backStackEntry ->
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: return@composable
                    ImageViewScreen(imageUrl = imageUrl, onBackPressed = { navController.popBackStack() })
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
fun ChatListScreen(viewModel: ChatViewModel, onChatClick: (id: String) -> Unit) {
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
                    Row {
                        Text(
                            text = chat,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .weight(9f)
                                .clickable {
                                    onChatClick(chat)
                                }
                                .padding(8.dp)

                        )
                    }

                }
            }
        }
        is ChatsState.Error -> {
            // Ошибка при получении чатов
            Text("Error: ${state.message}")
        }
    }

}

@Composable
fun MessagesScreen(viewModel: MessagesViewModel, onImageClick: (String) -> Unit) {
    val messagesState by viewModel.messagesState.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Отслеживаем прокрутку и загружаем старые сообщения
    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val totalItemsCount = listState.layoutInfo.totalItemsCount

    LaunchedEffect(messagesState) {
        // Прокручиваем список до последнего сообщения после загрузки
        if (messagesState is ChatListState.Success) {
            listState.scrollToItem((messagesState as ChatListState.Success).chatInfo.messages.size - 1)
        }
    }

    LaunchedEffect(firstVisibleItemIndex) {
        // Подгружаем более старые сообщения, если пользователь прокручивает в верхнюю часть списка
        if (firstVisibleItemIndex <= 1 && totalItemsCount > 0) {
            val firstVisibleMessage = viewModel.getFirstVisibleMessageId()
            viewModel.loadMoreMessages(firstVisibleMessage) {}

        }
    }

    when (messagesState) {
        is ChatListState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is ChatListState.Success -> {
            val chatInfo = (messagesState as ChatListState.Success).chatInfo
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    state = listState
                ) {
                    items(chatInfo.messages.size) { index ->
                        val message = chatInfo.messages[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${message.from}:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (message.data.Text != null) {
                                Text(
                                    text = message.data.Text.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else if (message.data.Image != null) {
                                GlideImage(
                                    url = message.data.Image.link,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clickable {
                                            onImageClick(message.data.Image.link)
                                        },
                                    "thumb"
                                )
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(onClick = {
                        if (messageText.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.sendMessage(messageText, from = "konarparti")
                                messageText = "" // Очистка поля после отправки
                            }
                        }
                    }) {
                        Text("Send")
                    }
                }
            }
        }
        is ChatListState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${(messagesState as ChatListState.Error).message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}






@Composable
fun ImageViewScreen(imageUrl: String, onBackPressed: () -> Unit) {
    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlideImage(
            url = imageUrl,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            "img"
        )
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

//@Composable
//fun ChatScreen(messages: List<Message>, onMessageSend: (String) -> Unit) {
//    Column {
//        LazyColumn(modifier = Modifier.weight(1f)) {
//            items(messages.size) { index ->
//                val message = messages[index]
//                when {
//                    message.text != null -> Text(message.text)
//                    message.imageLink != null -> GlideImage(message.imageLink, )
//                }
//            }
//        }
//        TextField(
//            value = "",
//            onValueChange = { /* handle input */ },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Button(onClick = { onMessageSend("Your Message") }) {
//            Text("Send")
//        }
//    }
//}

@Composable
fun GlideImage(url: String, modifier: Modifier = Modifier, mode: String) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                if(mode == context.getString(R.string.thumb_mode))
                    Glide.with(context).load(context.getString(R.string.thumb, url))
                        .into(this)
                else if ( mode == context.getString(R.string.img_mode))
                    Glide.with(context).load(context.getString(R.string.img, url))
                        .into(this)
            }
        },
        modifier = modifier
    )
}



