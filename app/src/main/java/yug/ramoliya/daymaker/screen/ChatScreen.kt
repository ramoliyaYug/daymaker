package yug.ramoliya.daymaker.screen

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import yug.ramoliya.daymaker.constants.Constants
import yug.ramoliya.daymaker.viewmodel.ChatViewModel
import java.io.File

@Composable
fun ChatScreen(
    vm: ChatViewModel = viewModel()
) {

    val messages by vm.messages.collectAsState()
    val inputText by vm.inputText.collectAsState()
    val isTyping by vm.isTyping.collectAsState()
    val otherUserStatus by vm.otherUserStatus.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showMediaOptions by remember { mutableStateOf(false) }

    // Auto scroll when new message comes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { f ->
                vm.sendMediaMessage(f, Constants.TYPE_IMAGE)
            }
        }
        showMediaOptions = false
    }

    // Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { f ->
                vm.sendMediaMessage(f, Constants.TYPE_VIDEO)
            }
        }
        showMediaOptions = false
    }

    // Audio Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it)
            file?.let { f ->
                vm.sendMediaMessage(f, Constants.TYPE_AUDIO)
            }
        }
        showMediaOptions = false
    }

    // Error Dialog
    if (error != null) {
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Error") },
            text = { Text(error ?: "") },
            confirmButton = {
                Button(onClick = { vm.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Media Options Dialog
    if (showMediaOptions) {
        AlertDialog(
            onDismissRequest = { showMediaOptions = false },
            title = { Text("Select Media Type") },
            text = { Text("Choose what you want to send") },
            confirmButton = {
                Button(onClick = { showMediaOptions = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showMediaOptions = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Image")
                    }
                    Button(
                        onClick = {
                            videoPickerLauncher.launch("video/*")
                            showMediaOptions = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Video")
                    }
                    Button(
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                            showMediaOptions = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Audio")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Private Chat",
                    style = MaterialTheme.typography.titleMedium
                )

                OnlineStatus(status = otherUserStatus)
            }
        },
        bottomBar = {
            Column {
                TypingIndicator(isTyping = isTyping)

                ChatInputBar(
                    text = inputText,
                    onTextChange = vm::onTextChange,
                    onSendClick = vm::sendTextMessage,
                    onAttachClick = {
                        showMediaOptions = true
                    }
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (loading) {
                CenterLoader()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.messageId }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

// Helper function to convert Uri to File with proper extension
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        // Get MIME type and determine extension
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: run {
                // Fallback: try to get from URI path
                val path = uri.path
                if (path != null && path.contains(".")) {
                    path.substring(path.lastIndexOf(".") + 1)
                } else {
                    "bin"
                }
            }

        val fileName = "file_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, fileName)

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        file
    } catch (e: Exception) {
        null
    }
}
