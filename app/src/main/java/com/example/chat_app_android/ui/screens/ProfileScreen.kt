package com.example.chat_app_android.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chat_app_android.data.local.SessionManager
import com.example.chat_app_android.data.models.ChangePasswordRequest
import com.example.chat_app_android.data.models.UserModel
import com.example.chat_app_android.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.math.absoluteValue

private fun uriToFile(context: Context, uri: Uri): File {
    val tempFile = File.createTempFile("profile_", ".jpg", context.cacheDir)

    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Не може да се отвори избраният файл.")

    inputStream.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }

    return tempFile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<UserModel?>(null) }
    var isUploadingProfileImage by remember { mutableStateOf(false) }

    val email = currentUser?.email ?: sessionManager.fetchEmail().orEmpty()
    val username = currentUser?.username ?: ""
    val firstLetter = when {
        username.isNotEmpty() -> username.first().uppercaseChar().toString()
        email.isNotEmpty() -> email.first().uppercaseChar().toString()
        else -> "?"
    }

    val avatarColors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800)
    )
    val avatarColor = avatarColors[email.hashCode().absoluteValue % avatarColors.size]

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = sessionManager.fetchAuthToken() ?: return@LaunchedEffect
        try {
            val response = RetrofitClient.apiService.getMe("Bearer $token")
            if (response.isSuccessful) {
                currentUser = response.body()
            }
        } catch (_: Exception) {
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val token = sessionManager.fetchAuthToken() ?: return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
            try {
                isUploadingProfileImage = true

                val file = uriToFile(context, uri)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    requestFile
                )

                val uploadResponse = RetrofitClient.apiService.uploadProfileImage(
                    token = "Bearer $token",
                    file = multipartBody
                )

                if (uploadResponse.isSuccessful) {
                    val meResponse = RetrofitClient.apiService.getMe("Bearer $token")
                    if (meResponse.isSuccessful) {
                        currentUser = meResponse.body()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isUploadingProfileImage = false
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Изход") },
            text = { Text("Сигурен ли си, че искаш да излезеш?") },
            confirmButton = {
                TextButton(onClick = {
                    sessionManager.clearSession()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Изход", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отказ")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Изтриване на акаунт") },
            text = { Text("Това ще изтрие завинаги твоя акаунт и всички твои чатове. Действието не може да бъде отменено.") },
            confirmButton = {
                TextButton(onClick = {
                    val token = sessionManager.fetchAuthToken() ?: return@TextButton
                    isDeleting = true
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.deleteAccount("Bearer $token")
                            if (response.code() == 200) {
                                sessionManager.clearSession()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            isDeleting = false
                        }
                    }
                    showDeleteAccountDialog = false
                }) {
                    Text("Изтрий", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Отказ")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmNewPassword = ""
                changePasswordError = null
            },
            title = { Text("Смяна на парола") },
            text = {
                Column {
                    changePasswordError?.let {
                        Text(
                            it,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Текуща парола") },
                        visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(
                                    if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Нова парола") },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Потвърди новата парола") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        isError = confirmNewPassword.isNotEmpty() && confirmNewPassword != newPassword,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (currentPassword.isBlank() || newPassword.isBlank()) {
                        changePasswordError = "Моля, попълнете всички полета"
                        return@TextButton
                    }
                    if (newPassword != confirmNewPassword) {
                        changePasswordError = "Паролите не съвпадат"
                        return@TextButton
                    }
                    if (newPassword.length < 6) {
                        changePasswordError = "Паролата трябва да е поне 6 символа"
                        return@TextButton
                    }

                    val token = sessionManager.fetchAuthToken() ?: return@TextButton

                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.changePassword(
                                "Bearer $token",
                                ChangePasswordRequest(currentPassword, newPassword)
                            )
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Паролата беше сменена успешно!", Toast.LENGTH_SHORT).show()
                                showChangePasswordDialog = false
                                currentPassword = ""
                                newPassword = ""
                                confirmNewPassword = ""
                                changePasswordError = null
                            } else {
                                changePasswordError = "Текущата парола е грешна"
                            }
                        } catch (_: Exception) {
                            changePasswordError = "Грешка в мрежата"
                        }
                    }
                }) {
                    Text("Запази")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    changePasswordError = null
                }) {
                    Text("Отказ")
                }
            }
        )
    }

    val baseUrl = "http://10.0.2.2:8080"
    val fullProfileImageUrl = currentUser?.profileImageUrl?.let { baseUrl + it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профил") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            if (fullProfileImageUrl != null) {
                AsyncImage(
                    model = fullProfileImageUrl,
                    contentDescription = "Профилна снимка",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploadingProfileImage
            ) {
                Text(if (isUploadingProfileImage) "Качване..." else "Смени профилната снимка")
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
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Смени паролата", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Изход", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isDeleting
            ) {
                Text("Изтрий акаунта", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}