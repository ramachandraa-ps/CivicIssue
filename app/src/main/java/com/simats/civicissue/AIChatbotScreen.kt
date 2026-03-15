package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.PrimaryBlue
import com.simats.civicissue.ui.theme.BackgroundBlue
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatbotScreen(
    onBack: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(text = "Hello! I'm your AI Civic Assistant. How can I help you today?", isUser = false),
            ChatMessage(text = "You can report issues like potholes, streetlights, or check your complaint status.", isUser = false)
        )
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickActions = listOf(
        "Report Pothole" to Icons.Default.ReportProblem,
        "Garbage Issue" to Icons.Default.DeleteOutline,
        "Streetlight Not Working" to Icons.Default.Lightbulb,
        "Drainage Problem" to Icons.Default.WaterDrop,
        "Check Status" to Icons.Default.TrackChanges
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Civic Assistant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Always here to help", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = BackgroundBlue
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                if (isSending) {
                    item {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            // Bot avatar for typing indicator
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = PrimaryBlue.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                tonalElevation = 1.dp
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                // Quick Actions
                Text(
                    "Suggested actions",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(quickActions) { action ->
                        SuggestionChip(label = action.first, icon = action.second) {
                            inputText = action.first
                        }
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type your civic issue...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = Color.LightGray,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (inputText.isNotBlank() && !isSending) {
                                    val currentText = inputText
                                    messages.add(ChatMessage(text = currentText, isUser = true))
                                    inputText = ""
                                    isSending = true
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                        try {
                                            val api = RetrofitClient.instance
                                            val chatResponse = api.chat(ChatRequest(message = currentText, session_id = sessionId))
                                            sessionId = chatResponse.session_id
                                            messages.add(ChatMessage(text = chatResponse.response, isUser = false))
                                        } catch (e: Exception) {
                                            Log.e("AIChatbot", "Chat failed: ${e.message}")
                                            messages.add(ChatMessage(text = "Sorry, I couldn't process that. Please try again.", isUser = false))
                                        } finally {
                                            isSending = false
                                        }
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            },
                        color = if (isSending) Color.Gray else PrimaryBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSending) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isUser) {
            // User message: right-aligned with subtle shadow
            Surface(
                color = PrimaryBlue,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                ),
                tonalElevation = 1.dp,
                shadowElevation = 3.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        } else {
            // Bot message: with small bot avatar icon on the left
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(end = 48.dp)
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = PrimaryBlue.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    ),
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = formatMarkdownText(message.text, Color.Black),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
        Text(
            text = message.timestamp,
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = if (message.isUser) 4.dp else 40.dp, end = 4.dp)
        )
    }
}

fun formatMarkdownText(text: String, defaultColor: Color): AnnotatedString {
    // First, normalize newlines: replace literal \n with actual newlines
    val normalized = text.replace("\\n", "\n")

    return buildAnnotatedString {
        var i = 0
        while (i < normalized.length) {
            when {
                // Bold: **text**
                i + 1 < normalized.length && normalized[i] == '*' && normalized[i + 1] == '*' -> {
                    val endIndex = normalized.indexOf("**", i + 2)
                    if (endIndex != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                            append(normalized.substring(i + 2, endIndex))
                        }
                        i = endIndex + 2
                    } else {
                        append(normalized[i])
                        i++
                    }
                }
                // Bullet point: * at start of line (after newline or at position 0)
                normalized[i] == '*' && normalized.getOrNull(i + 1) == ' ' &&
                        (i == 0 || normalized[i - 1] == '\n') -> {
                    append("  \u2022 ")
                    i += 2 // skip "* "
                }
                else -> {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(normalized[i])
                    }
                    i++
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
        color = PrimaryBlue.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offsetY.dp)
                    .background(Color.Gray, CircleShape)
            )
        }
    }
}

// getBotResponse removed - now using real API via RetrofitClient.instance.chat()
