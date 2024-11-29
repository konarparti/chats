package konarparti.messenger

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import konarparti.messenger.Base.Data
import konarparti.messenger.Base.Message
import konarparti.messenger.States.ChatListState
import konarparti.messenger.States.ChatsState
import konarparti.messenger.ViewModel.ChatViewModel
import konarparti.messenger.ViewModel.LoginUiState
import konarparti.messenger.ViewModel.LoginViewModel
import konarparti.messenger.ViewModel.MessagesViewModel
import konarparti.messenger.Web.SharedPreferencesHelper
import konarparti.messenger.Web.TokenManager
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val tokenManager = remember { TokenManager(this) }
            val loginViewModel = remember { LoginViewModel(tokenManager, this) }
            val chatViewModel = remember { ChatViewModel(this) }
            var isLoggedIn by rememberSaveable { mutableStateOf(SharedPreferencesHelper.getToken(this) != null) }
            var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
            val context = this

            MaterialTheme {
                if (!isLoggedIn) {
                    LoginScreen(viewModel = loginViewModel) {
                        isLoggedIn = true
                        chatViewModel.loadChats()
                    }
                } else {
                    LaunchedEffect(Unit) {
                        chatViewModel.loadChats()
                    }
                    val chatListState = chatViewModel.chatListState.collectAsState()

                    BackHandler(enabled = true) {
                        when {
                            selectedChatId != null -> {
                                selectedChatId = null
                            }
                            else -> {
                                loginViewModel.logout()
                                chatViewModel.resetState()
                                SharedPreferencesHelper.clearToken(context)
                                isLoggedIn = false
                            }
                        }
                    }

                    ResponsiveChatScreen(
                        chatListState = chatListState.value,
                        selectedChatId = selectedChatId,
                        onChatSelected = { chatId ->
                            selectedChatId = chatId
                        },
                        context = context,
                        onBack = {
                            selectedChatId = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ResponsiveChatScreen(
    chatListState: ChatsState,
    selectedChatId: String?,
    onChatSelected: (String) -> Unit,
    context: Context,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(Modifier.fillMaxSize()) {
            ChatListScreen(
                chatListState = chatListState,
                modifier = Modifier.width(300.dp),
                onChatClick = onChatSelected
            )
            if (selectedChatId != null) {
                ChatDetailScreen(chatId = selectedChatId, modifier = Modifier.weight(1f), context, onBack = onBack)
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.choose_chat), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    } else {
        if (selectedChatId == null) {
            ChatListScreen(
                chatListState = chatListState,
                modifier = Modifier.fillMaxSize(),
                onChatClick = onChatSelected
            )
        } else {
            ChatDetailScreen(
                chatId = selectedChatId,
                modifier = Modifier.fillMaxSize(),
                context,
                onBack = onBack
            )
        }
    }
}

@Composable
fun ChatListScreen(
    chatListState: ChatsState,
    modifier: Modifier = Modifier,
    onChatClick: (String) -> Unit
) {
    when (chatListState) {
        is ChatsState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ChatsState.Success -> {
            LazyColumn(modifier) {
                items(chatListState.chats.size) { index ->
                    val chat = chatListState.chats[index]
                    Text(
                        text = chat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(chat) }
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        is ChatsState.Error -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error, chatListState.message?: ""), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    chatId: String,
    modifier: Modifier = Modifier,
    context: Context,
    onBack: (() -> Unit)? = null
) {
    val messagesViewModel = remember(chatId) { MessagesViewModel(chatId, context) }
    val messagesState by messagesViewModel.messagesState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var imageUrlToShow by remember { mutableStateOf<String?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var messages: List<Message>  by remember {  mutableStateOf (listOf()) }

    LaunchedEffect(chatId) {
        messagesViewModel.getFirstVisibleMessageId()
    }

    val reachedBottom: Boolean by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 5
        }
    }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom && !isLoading) {
            isLoading = true
            val lastKnownId = messagesViewModel.getFirstVisibleMessageId()
            messagesViewModel.loadMoreMessages(lastKnownId) {
                isLoading = false
                messages = messages + it
            }
        }
    }


    BackHandler(enabled = true) {
        if (imageUrlToShow != null) {
            imageUrlToShow = null
        } else {
            onBack?.invoke()
        }
    }

    if (imageUrlToShow != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isImageLoading) {
                CircularProgressIndicator()
            }
            GlideImage(
                url = imageUrlToShow!!,
                modifier = Modifier.fillMaxSize(),
                mode = "img",
            )
        }
    } else {
        Column(modifier) {
            Text(
                text = "Чат: $chatId",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Divider()

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            when (messagesState) {
                is ChatListState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatListState.Success -> {
                    messages = (messagesState as ChatListState.Success).chatInfo.messages
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(messages.size) { index ->
                            val message = messages[index]
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
                                                imageUrlToShow = message.data.Image.link
                                            },
                                        context.getString(R.string.thumb_mode)
                                    )
                                }
                            }
                        }
                    }
                    ChatInputField(onMessageSend = { text ->
                        coroutineScope.launch {
                            val from = SharedPreferencesHelper.getLogin(context)

                            if(from == null){
                                Toast.makeText(context,
                                    context.getString(R.string.error_with_login), Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            messagesViewModel.sendMessage(text, from)

                            val newMessage = Message(
                                id = 0,
                                from = from,
                                to = chatId,
                                data = Data(Text = konarparti.messenger.Base.Text(text = text)),
                                time = System.currentTimeMillis().toString()
                            )
                            messages = messages + listOf(newMessage)
                        }
                    })
                }
                is ChatListState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.error_loading_messages),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputField(onMessageSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.type_your_message)) },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onMessageSend(text)
                    text = ""
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(stringResource(R.string.send))
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
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.login(name, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login))
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