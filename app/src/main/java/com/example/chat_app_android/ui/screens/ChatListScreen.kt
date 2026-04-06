package com.example.chat_app_android.ui.screens


import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.ChatPreview
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

    val chats by viewModel.chats.collectAsStateWithLifecycle(emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(false)
    val error by viewModel.error.collectAsStateWithLifecycle(null)
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle(false)

    LaunchedEffect(sessionExpired) {
        if(sessionExpired){
            navController.navigate("login"){
                popUpTo(0) {inclusive = true}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    val currentEmail = viewModel.getCurrentUserEmail()

    val filteredChats = chats.filter{preview ->
        searchQuery.isEmpty() ||
                preview.user.username.contains(searchQuery, ignoreCase = true)
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

            filteredChats.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ){
                    Text(text = "No users found", color = Color.Gray, fontSize = 16.sp)
                }
            }

            filteredChats.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ){
                    Text(text = "No other users yet", color = Color.Gray, fontSize = 16.sp)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(filteredChats) {preview ->
                        ChatPreviewItem(
                            preview = preview,
                            currentEmail = currentEmail,
                            onClick = {
                                navController.navigate("chat/${preview.user.email}/${preview.user.username}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatPreviewItem(preview: ChatPreview, currentEmail: String, onClick: () -> Unit){
    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0)
    )
    val avatarColor = avatarColors[preview.user.email.hashCode().absoluteValue % avatarColors.size]

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
                text = preview.user.username.first().uppercaseChar().toString(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)){
            Text(
                text = preview.user.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if(preview.isMine) "You: ${preview.lastMessage}"
                else preview.lastMessage,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if(preview.lastMessageTime.isNotEmpty()){
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = preview.lastMessageTime,
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