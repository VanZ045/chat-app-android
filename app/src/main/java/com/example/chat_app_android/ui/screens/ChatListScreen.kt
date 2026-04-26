package com.example.chat_app_android.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chat_app_android.data.models.ChatSummaryModel
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.ui.viewmodels.ChatListViewModel
import kotlin.math.absoluteValue

private fun buildImageUrl(relativePath: String?): String? {
    if (relativePath.isNullOrBlank()) return null
    return "http://10.0.2.2:8080$relativePath"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<ChatSummaryModel?>(null) }

    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(false)
    val error by viewModel.error.collectAsStateWithLifecycle(null)
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle(false)
    val navigateToChat by viewModel.navigateToChat.collectAsStateWithLifecycle(null)
    val typingChats by viewModel.typingChats.collectAsStateWithLifecycle()

    val currentUserId = viewModel.getCurrentUserId()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        android.util.Log.d("FCM_DEBUG", "POST_NOTIFICATIONS granted = $granted")
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(navigateToChat) {
        navigateToChat?.let { chat ->
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

    val filteredChats = chats.filter {
        it.otherUsername.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search people...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Chats",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(
                        onClick = {
                            navController.navigate("profile")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        chatToDelete?.let { chat ->
            AlertDialog(
                onDismissRequest = { chatToDelete = null },
                title = { Text("Delete chat") },
                text = { Text("Delete your conversation with ${chat.otherUsername}?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteChat(chat.chatId)
                        chatToDelete = null
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error ?: "",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadUsers() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (isSearching) {
                        if (filteredUsers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No users found", color = Color.Gray)
                                }
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No chats yet — tap search to find someone!",
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
                                    },
                                    onLongClick = {
                                        chatToDelete = chat
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
fun ChatItem(
    chat: ChatSummaryModel,
    formattedTime: String,
    currentUserId: Long,
    isTyping: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[chat.otherUserId.toInt().absoluteValue % avatarColors.size]
    val fullProfileImageUrl = buildImageUrl(chat.otherUserProfileImageUrl)

    val lastMessageText = when {
        isTyping -> "typing..."
        chat.lastMessage.isEmpty() -> "Tap to start chatting"
        chat.lastMessageSenderId == currentUserId -> when (chat.lastMessage) {
            "[Image]" -> "You sent a photo"
            else -> "You: ${chat.lastMessage}"
        }
        else -> when (chat.lastMessage) {
            "[Image]" -> "sent a photo"
            else -> chat.lastMessage
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fullProfileImageUrl != null) {
            AsyncImage(
                model = fullProfileImageUrl,
                contentDescription = "Profile image",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.otherUsername.first().uppercaseChar().toString(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
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
                color = if (isTyping) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 13.sp,
                fontStyle = if (isTyping) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (formattedTime.isNotEmpty()) {
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
fun UserItem(user: UserModel, onClick: () -> Unit) {
    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[user.id.toInt().absoluteValue % avatarColors.size]
    val fullProfileImageUrl = buildImageUrl(user.profileImageUrl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fullProfileImageUrl != null) {
            AsyncImage(
                model = fullProfileImageUrl,
                contentDescription = "Profile image",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.first().uppercaseChar().toString(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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