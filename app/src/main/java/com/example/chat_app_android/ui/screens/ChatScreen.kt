package com.example.chat_app_android.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chat_app_android.data.models.MessageModel
import com.example.chat_app_android.data.network.RetrofitClient
import com.example.chat_app_android.ui.viewmodels.ChatViewModel
import java.io.File
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: Long,
    otherUsername: String,
    otherUserProfileImageUrl: String? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle()
    val isOtherTyping by viewModel.isOtherTyping.collectAsStateWithLifecycle()
    val failedMessageContent by viewModel.failedMessageContent.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    var messageToEdit by remember { mutableStateOf<MessageModel?>(null) }
    val listState = rememberLazyListState()
    val currentUserId = viewModel.getCurrentUserId()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(chatId) {
        viewModel.enterActiveChat(chatId)

        onDispose {
            viewModel.leaveActiveChat()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadImage(chatId, uri)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.uploadImage(chatId, cameraImageUri!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[otherUsername.hashCode().absoluteValue % avatarColors.size]

    val lastOwnMessageId = messages.lastOrNull { it.senderId == currentUserId }?.id

    LaunchedEffect(failedMessageContent) {
        failedMessageContent?.let { content ->
            val result = snackbarHostState.showSnackbar(
                message = "Съобщението не беше изпратено",
                actionLabel = "Опитай отново",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryMessage(chatId, content)
            } else {
                viewModel.clearFailedMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val fullOtherImageUrl = remember(otherUserProfileImageUrl) {
                        if (!otherUserProfileImageUrl.isNullOrBlank() && otherUserProfileImageUrl != "null")
                            RetrofitClient.BASE_URL.trimEnd('/') + otherUserProfileImageUrl
                        else null
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (fullOtherImageUrl != null) {
                            AsyncImage(
                                model = fullOtherImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(avatarColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = otherUsername.first().uppercaseChar().toString(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Камера",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        if (it.isNotEmpty()) viewModel.onUserTyping(chatId)
                    },
                    placeholder = { Text("Съобщение...") },
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
                        if (messageText.isNotBlank()) {
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
                        contentDescription = "Изпрати",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
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
                            text = "Все още няма съобщения.\nКажи здрасти",
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
                                showStatusLabel = message.id == lastOwnMessageId && message.senderId == currentUserId,
                                onEditRequest = { selectedMessage ->
                                    messageToEdit = selectedMessage
                                },
                                onDeleteRequest = { selectedMessage ->
                                    viewModel.deleteMessage(chatId, selectedMessage.id)
                                },
                                onImageClick = { imageUrl ->
                                    selectedImageUrl = imageUrl
                                }
                            )
                        }
                    }
                }
            }

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
                        text = "$otherUsername пише...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            messageToEdit?.let { msg ->
                var editText by remember(msg.id) { mutableStateOf(msg.content ?: "") }
                AlertDialog(
                    onDismissRequest = { messageToEdit = null },
                    title = { Text("Редактиране на съобщение") },
                    text = {
                        TextField(
                            value = editText,
                            onValueChange = { editText = it },
                            singleLine = false
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (editText.isNotBlank()) {
                                viewModel.editMessage(chatId, msg.id, editText)
                            }
                            messageToEdit = null
                        }) {
                            Text("Запази")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { messageToEdit = null }) {
                            Text("Отказ")
                        }
                    }
                )
            }

            selectedImageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .combinedClickable(
                            onClick = { selectedImageUrl = null },
                            onLongClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Снимка на цял екран",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "images")
    if (!imageDir.exists()) {
        imageDir.mkdirs()
    }

    val imageFile = File(imageDir, "camera_${System.currentTimeMillis()}.jpg")

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

@Composable
fun MessageBubble(
    message: MessageModel,
    isOwnMessage: Boolean,
    showStatusLabel: Boolean,
    onEditRequest: (MessageModel) -> Unit,
    onDeleteRequest: (MessageModel) -> Unit,
    onImageClick: (String) -> Unit
) {
    val formattedTime = remember(message.createdAt) {
        try {
            val dt = java.time.LocalDateTime.parse(message.createdAt)
            dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            ""
        }
    }

    val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
    val fullImageUrl = remember(message.imageUrl) {
        message.imageUrl?.let { baseUrl + it }
    }

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isOwnMessage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isOwnMessage) 18.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 18.dp
                    )
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (isOwnMessage) showMenu = true }
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column {
                when (message.type) {
                    "TEXT" -> {
                        Text(
                            text = message.content ?: "",
                            color = if (isOwnMessage) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }

                    "IMAGE" -> {
                        if (fullImageUrl != null) {
                            AsyncImage(
                                model = fullImageUrl,
                                contentDescription = "Изпратена снимка",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onImageClick(fullImageUrl) },
                                        onLongClick = { if (isOwnMessage) showMenu = true }
                                    )
                            )
                        } else {
                            Text(
                                text = "Снимката не е налична",
                                color = if (isOwnMessage) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                if (formattedTime.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = if (isOwnMessage) Color.White.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        if (isOwnMessage && showStatusLabel && message.status == "SEEN") {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Видяно",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (message.type == "TEXT") {
                DropdownMenuItem(
                    text = { Text("Редактирай") },
                    onClick = {
                        showMenu = false
                        onEditRequest(message)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Изтрий") },
                onClick = {
                    showMenu = false
                    onDeleteRequest(message)
                }
            )
        }
    }
}