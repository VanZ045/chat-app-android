package com.example.chat_app_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    val email = sessionManager.fetchEmail() ?: ""
    val firstLetter = if(email.isNotEmpty()) email.first().uppercaseChar().toString() else "?"

    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800)
    )
    val avatarColor = avatarColors[email.hashCode().absoluteValue % avatarColors.size]

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember {mutableStateOf(false)}
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if(showLogoutDialog){
        AlertDialog(
            onDismissRequest = {showLogoutDialog = false},
            title = { Text("Logout") },
            text = {Text("Are you sure you want to logout?")},
            confirmButton = {
                TextButton(onClick = {
                    sessionManager.clearSession()
                    navController.navigate("login"){
                        popUpTo(0) {inclusive = true}
                    }
                }) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {showLogoutDialog = false}) {
                    Text("Cancel")
                }
            }
        )
    }

    if(showDeleteAccountDialog){
        AlertDialog(
            onDismissRequest = {showDeleteAccountDialog = false},
            title = {Text("Delete account")},
            text = {Text("This will permanently delete your account and all your chats. This cannot be undone.")},
            confirmButton = {
                TextButton(onClick = {
                    val token = sessionManager.fetchAuthToken() ?: return@TextButton
                    isDeleting = true
                    scope.launch {
                        try{
                            val response = RetrofitClient.apiService.deleteAccount("Bearer $token")
                            if(response.code() == 200){
                                sessionManager.clearSession()
                                navController.navigate("login"){
                                    popUpTo(0) {inclusive = true}
                                }
                            }
                        }catch (e: Exception){
                        }finally{
                            isDeleting = false
                        }
                    }
                    showDeleteAccountDialog = false
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = {showDeleteAccountDialog = false}) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {Text("Profile")},
            navigationIcon = {
                IconButton(onClick = {navController.popBackStack()}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = firstLetter,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = email,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(48.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {showLogoutDialog = true},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Logout", color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {showDeleteAccountDialog = true},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isDeleting
            ) {
                Text("Delete Account", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}