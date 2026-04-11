package com.example.chat_app_android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.ui.viewmodels.ChatViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: Long,
    otherUsername: String,
    viewModel: ChatViewModel = viewModel()
){
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle()
    val isOtherTyping by viewModel.isOtherTyping.collectAsStateWithLifecycle()
    val failedMessageContent by viewModel.failedMessageContent.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentUserId = viewModel.getCurrentUserId()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionExpired) {
        if(sessionExpired){
            navController.navigate("login"){
                popUpTo(0) {inclusive = true}
            }
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    LaunchedEffect(messages.size) {
        if(messages.isNotEmpty()){
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[otherUsername.hashCode().absoluteValue % avatarColors.size]

    val lastOwnMessageId = messages.lastOrNull{it.senderId == currentUserId}?.id

    LaunchedEffect(failedMessageContent) {
        failedMessageContent?.let {content ->
            val result = snackbarHostState.showSnackbar(
                message = "Message failed to send",
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if(result == SnackbarResult.ActionPerformed){
                viewModel.retryMessage(chatId, content)
            }else{
                viewModel.clearFailedMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = {SnackbarHost(snackbarHostState)},
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ){
                            Text(
                                text = otherUsername.first().uppercaseChar().toString(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = otherUsername,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {navController.popBackStack()}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = {messageText = it
                                    if(it.isNotEmpty()) viewModel.onUserTyping(chatId)},
                    placeholder = {Text("Message...")},
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if(messageText.isNotBlank()){
                            viewModel.sendMessage(chatId, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet.\nSay hello",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = if (isOtherTyping) 36.dp else 12.dp
                        )
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                showStatusLabel = message.id == lastOwnMessageId && message.senderId == currentUserId
                            )
                        }
                    }
                }
            }

            // Typing indicator anchored to bottom of the Box
            AnimatedVisibility(
                visible = isOtherTyping,
                modifier = Modifier.align(Alignment.BottomStart),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$otherUsername is typing...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageModel, isOwnMessage: Boolean, showStatusLabel: Boolean){
    val formattedTime = remember(message.createdAt){
        try {
            val dt = java.time.LocalDateTime.parse(message.createdAt)
            dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        }catch (e: Exception){""}
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if(isOwnMessage) Alignment.End else Alignment.Start
    ){
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if(isOwnMessage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if(isOwnMessage) 18.dp else 4.dp,
                        bottomEnd = if(isOwnMessage) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ){
            Column {
                Text(
                    text = message.content,
                    color = if (isOwnMessage) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                if(formattedTime.isNotEmpty()){
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = if(isOwnMessage) Color.White.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        if(isOwnMessage && showStatusLabel && message.status == "SEEN"){
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Seen",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}