package com.example.chat_app_android.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.ui.viewmodels.ChatListViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = viewModel()
){
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(false)
    val error by viewModel.error.collectAsStateWithLifecycle(null)
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle(false)
    val navigateToChat by viewModel.navigateToChat.collectAsStateWithLifecycle(null)
    val typingChats by viewModel.typingChats.collectAsStateWithLifecycle()

    val currentUserId = viewModel.getCurrentUserId()

    LaunchedEffect(sessionExpired) {
        if(sessionExpired){
            navController.navigate("login"){
                popUpTo(0) {inclusive = true}
            }
        }
    }

    LaunchedEffect(navigateToChat) {
        navigateToChat?.let{chat ->
            navController.navigate("chat/${chat.chatId}/${chat.otherUsername}")
            viewModel.clearNavigation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    val isSearching = searchQuery.isNotEmpty()

    val filteredUsers = users.filter {
        it.username.contains(searchQuery, ignoreCase = true)
    }

    val filteredChats = chats.filter{
        it.otherUsername.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if(isSearchActive){
                        TextField(
                            value = searchQuery,
                            onValueChange = {searchQuery = it},
                            placeholder = {Text("Search people...")},
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }else{
                        Text(
                            text = "Chats",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if(!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if(isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate("profile")
                    }){
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ){
        paddingValues ->
        when{
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ){
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ){
                    Column(horizontalAlignment = Alignment.CenterHorizontally){
                        Text(text = error ?: "", color = Color.Red, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {viewModel.loadUsers()}){
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (isSearching) {
                        if (filteredUsers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text("No users found", color = Color.Gray) }
                            }
                        } else {
                            items(filteredUsers) { user ->
                                UserItem(
                                    user = user,
                                    onClick = { viewModel.openOrCreateChat(user.id) }
                                )
                            }
                        }
                    } else {
                        if (chats.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No chats yet — tap to find someone!",
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            items(filteredChats) { chat ->
                                ChatItem(
                                    chat = chat,
                                    formattedTime = viewModel.formatTime(chat.lastMessageTime),
                                    currentUserId = currentUserId,
                                    isTyping = typingChats.contains(chat.chatId),
                                    onClick = {
                                        navController.navigate("chat/${chat.chatId}/${chat.otherUsername}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: ChatSummaryModel, formattedTime: String, currentUserId: Long, isTyping: Boolean, onClick: () -> Unit){
    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[chat.otherUserId.toInt().absoluteValue % avatarColors.size]

    val lastMessageText = when{
        isTyping -> "typing..."
        chat.lastMessage.isEmpty() -> "Tap to start chatting"
        chat.lastMessageSenderId == currentUserId -> "You: ${chat.lastMessage}"
        else -> chat.lastMessage
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = chat.otherUsername.first().uppercaseChar().toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)){
            Text(
                text = chat.otherUsername,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = lastMessageText,
                color = if(isTyping) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 13.sp,
                fontStyle = if(isTyping) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if(formattedTime.isNotEmpty()){
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formattedTime,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
        color = Color.LightGray.copy(alpha = 0.4f)
    )
}

@Composable
fun UserItem(user: UserModel, onClick: () -> Unit){
    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[user.id.toInt().absoluteValue % avatarColors.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = user.username.first().uppercaseChar().toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 84.dp, end = 16.dp),
        color = Color.Gray.copy(alpha = 0.4f)
    )
}