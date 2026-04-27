package com.example.chat_app_android.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.example.chat_app_android.data.models.ChangeUsernameRequest
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
        ?: throw IllegalArgumentException("Cannot open file.")
    inputStream.use { input -> FileOutputStream(tempFile).use { input.copyTo(it) } }
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
    val username = currentUser?.username ?: sessionManager.fetchUsername().orEmpty()
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

    var showChangeUsernameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var changeUsernameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = sessionManager.fetchAuthToken() ?: return@LaunchedEffect
        try {
            val response = RetrofitClient.apiService.getMe("Bearer $token")
            if (response.isSuccessful) currentUser = response.body()
        } catch (_: Exception) {}
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
                val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val uploadResponse = RetrofitClient.apiService.uploadProfileImage("Bearer $token", multipartBody)
                if (uploadResponse.isSuccessful) {
                    val meResponse = RetrofitClient.apiService.getMe("Bearer $token")
                    if (meResponse.isSuccessful) currentUser = meResponse.body()
                }
            } catch (_: Exception) {
            } finally { isUploadingProfileImage = false }
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Изход") },
            text = { Text("Сигурен ли си, че искаш да излезеш?") },
            confirmButton = {
                TextButton(onClick = {
                    sessionManager.clearSession()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }) { Text("Изход", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отказ") }
            }
        )
    }

    // Delete account dialog
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
                                navController.navigate("login") { popUpTo(0) { inclusive = true } }
                            }
                        } catch (_: Exception) {
                        } finally { isDeleting = false }
                    }
                    showDeleteAccountDialog = false
                }) { Text("Изтрий", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Отказ") }
            }
        )
    }

    // Change password dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                currentPassword = ""; newPassword = ""; confirmNewPassword = ""; changePasswordError = null
            },
            title = { Text("Смяна на парола") },
            text = {
                Column {
                    changePasswordError?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = currentPassword, onValueChange = { currentPassword = it },
                        label = { Text("Текуща парола") },
                        visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it },
                        label = { Text("Нова парола") },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPassword, onValueChange = { confirmNewPassword = it },
                        label = { Text("Потвърди новата парола") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
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
                    if (currentPassword.isBlank() || newPassword.isBlank()) { changePasswordError = "Моля, попълнете всички полета"; return@TextButton }
                    if (newPassword != confirmNewPassword) { changePasswordError = "Паролите не съвпадат"; return@TextButton }
                    if (newPassword.length < 6) { changePasswordError = "Паролата трябва да е поне 6 символа"; return@TextButton }
                    val token = sessionManager.fetchAuthToken() ?: return@TextButton
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.changePassword("Bearer $token", ChangePasswordRequest(currentPassword, newPassword))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Паролата беше сменена успешно!", Toast.LENGTH_SHORT).show()
                                showChangePasswordDialog = false
                                currentPassword = ""; newPassword = ""; confirmNewPassword = ""; changePasswordError = null
                            } else { changePasswordError = "Текущата парола е грешна" }
                        } catch (_: Exception) { changePasswordError = "Грешка в мрежата" }
                    }
                }) { Text("Запази") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                    currentPassword = ""; newPassword = ""; confirmNewPassword = ""; changePasswordError = null
                }) { Text("Отказ") }
            }
        )
    }

    // Change username dialog
    if (showChangeUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showChangeUsernameDialog = false; newUsername = ""; changeUsernameError = null },
            title = { Text("Смяна на потребителско име") },
            text = {
                Column {
                    changeUsernameError?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = newUsername, onValueChange = { newUsername = it },
                        label = { Text("Ново потребителско име") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newUsername.isBlank()) { changeUsernameError = "Моля, въведи потребителско име"; return@TextButton }
                    val token = sessionManager.fetchAuthToken() ?: return@TextButton
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.changeUsername("Bearer $token", ChangeUsernameRequest(newUsername))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Потребителското име беше сменено!", Toast.LENGTH_SHORT).show()
                                val meResponse = RetrofitClient.apiService.getMe("Bearer $token")
                                if (meResponse.isSuccessful) currentUser = meResponse.body()
                                showChangeUsernameDialog = false; newUsername = ""; changeUsernameError = null
                            } else { changeUsernameError = "Потребителското име вече е заето" }
                        } catch (_: Exception) { changeUsernameError = "Грешка в мрежата" }
                    }
                }) { Text("Запази") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeUsernameDialog = false; newUsername = ""; changeUsernameError = null }) { Text("Отказ") }
            }
        )
    }

    val fullProfileImageUrl = currentUser?.profileImageUrl?.let {
        RetrofitClient.BASE_URL.trimEnd('/') + it
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профил") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar section
            Column(
                modifier = Modifier.padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    if (fullProfileImageUrl != null) {
                        AsyncImage(
                            model = fullProfileImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(88.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(88.dp).background(avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(firstLetter, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(username, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            HorizontalDivider()

            // ACCOUNT section
            Text(
                "ACCOUNT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )

            ProfileRow(
                icon = Icons.Default.Person,
                iconBackground = Color(0xFFE3F2FD),
                iconTint = Color(0xFF1565C0),
                title = "Смяна на потребителско име",
                subtitle = username,
                onClick = { newUsername = username; showChangeUsernameDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 70.dp))

            ProfileRow(
                icon = Icons.Default.Lock,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Смяна на парола",
                onClick = { showChangePasswordDialog = true }
            )

            HorizontalDivider()

            // ACTIONS section
            Text(
                "ACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )

            ProfileRow(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconBackground = Color(0xFFFBE9E7),
                iconTint = Color(0xFFBF360C),
                title = "Изход",
                titleColor = Color(0xFFBF360C),
                showArrow = false,
                onClick = { showLogoutDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 70.dp))

            ProfileRow(
                icon = Icons.Default.Delete,
                iconBackground = Color(0xFFFFEBEE),
                iconTint = Color(0xFFC62828),
                title = "Изтрий акаунта",
                titleColor = Color(0xFFC62828),
                showArrow = false,
                onClick = { showDeleteAccountDialog = true }
            )

            HorizontalDivider()
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        if (showArrow) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}