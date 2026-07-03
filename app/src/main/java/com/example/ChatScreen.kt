package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// Data Models
data class ChatMessage(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val isMedia: Boolean = false,
    val isSafe: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val vibeLevel: Float = 0f, // 0.0 to 1.0
    val isClimax: Boolean = false
)

// ViewModel
class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        // Vibe decay simulator
        viewModelScope.launch {
            while(true) {
                delay(1000)
                val currentVibe = _state.value.vibeLevel
                if (currentVibe > 0 && !_state.value.isClimax) {
                    _state.value = _state.value.copy(vibeLevel = (currentVibe - 0.05f).coerceAtLeast(0f))
                }
            }
        }
        
        // WebSocket simulation
        viewModelScope.launch {
            delay(1500)
            _state.value = _state.value.copy(isTyping = true)
            delay(2000)
            _state.value = _state.value.copy(isTyping = false)
            receiveMessage("Hey there! ✌️")
            
            delay(6000)
            _state.value = _state.value.copy(isTyping = true)
            delay(1500)
            _state.value = _state.value.copy(isTyping = false)
            receiveMediaMessage()
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            isMine = true
        )
        val currentMessages = _state.value.messages.toMutableList()
        currentMessages.add(0, newMessage)
        
        val vibeBoost = text.length * 0.02f
        val newVibe = (_state.value.vibeLevel + vibeBoost).coerceAtMost(1f)
        val isClimax = newVibe >= 1f
        
        _state.value = _state.value.copy(
            messages = currentMessages,
            vibeLevel = newVibe,
            isClimax = _state.value.isClimax || isClimax
        )
    }

    private fun receiveMessage(text: String) {
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            isMine = false
        )
        val currentMessages = _state.value.messages.toMutableList()
        currentMessages.add(0, newMessage)
        val vibeBoost = text.length * 0.02f
        val newVibe = (_state.value.vibeLevel + vibeBoost).coerceAtMost(1f)
        val isClimax = newVibe >= 1f
        _state.value = _state.value.copy(
            messages = currentMessages, 
            vibeLevel = newVibe,
            isClimax = _state.value.isClimax || isClimax
        )
    }

    private fun receiveMediaMessage() {
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = "Sent an image",
            isMine = false,
            isMedia = true,
            isSafe = false
        )
        val currentMessages = _state.value.messages.toMutableList()
        currentMessages.add(0, newMessage)
        val newVibe = (_state.value.vibeLevel + 0.1f).coerceAtMost(1f)
        val isClimax = newVibe >= 1f
        _state.value = _state.value.copy(
            messages = currentMessages, 
            vibeLevel = newVibe,
            isClimax = _state.value.isClimax || isClimax
        )
    }

    fun markMediaSafe(messageId: String) {
        val currentMessages = _state.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            currentMessages[index] = currentMessages[index].copy(isSafe = true)
            _state.value = _state.value.copy(messages = currentMessages)
        }
    }

    fun purgeMedia(messageId: String) {
        val currentMessages = _state.value.messages.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            currentMessages[index] = currentMessages[index].copy(isSafe = false, text = "[PURGED]")
            _state.value = _state.value.copy(messages = currentMessages)
        }
    }
}

// UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.isClimax) {
        if (state.isClimax) {
            for (i in 0..10) {
                shakeOffset.animateTo(15f, tween(40))
                shakeOffset.animateTo(-15f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeOffset.value
            }
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text("Anonymous User", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                    // Vibe Meter
                    VibeMeter(vibeLevel = state.vibeLevel)
                }
            },
            bottomBar = {
                ChatInputBar(onSendMessage = { viewModel.sendMessage(it) })
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    if (state.isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                    
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onUnlock = { viewModel.markMediaSafe(message.id) },
                            onPurge = { viewModel.purgeMedia(message.id) }
                        )
                    }
                }

                if (state.isClimax) {
                    ConfettiOverlay()
                    
                    Button(
                        onClick = { /* Handle Reveal */ },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF00FF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Send Instagram Reveal Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val fall by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )
    
    val particles = remember {
        List(60) {
            Triple(
                Math.random().toFloat(),
                Math.random().toFloat(),
                listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color.Yellow, Color(0xFF00FF00)).random()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { (xOffset, yOffset, color) ->
            val x = xOffset * size.width
            val y = (yOffset * size.height + fall) % size.height
            drawCircle(color, radius = 8f, center = Offset(x, y))
        }
    }
}

@Composable
fun VibeMeter(vibeLevel: Float) {
    val animatedVibe by animateFloatAsState(
        targetValue = vibeLevel,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "VibeMeterAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color(0xFF1E1E1E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedVibe)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00FFFF), Color(0xFFFF00FF))
                    )
                )
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0f at 0
                        (-4f) at 300 + (i * 150)
                        0f at 600 + (i * 150)
                        0f at 1200
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offset.dp)
                    .background(Color(0xFF938F99), CircleShape)
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onUnlock: () -> Unit, onPurge: () -> Unit) {
    val isMine = message.isMine
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (message.isMedia) {
            MediaMessageBubble(message, onUnlock, onPurge)
        } else {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isMine) Color(0xFF4A4458) else Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun MediaMessageBubble(message: ChatMessage, onUnlock: () -> Unit, onPurge: () -> Unit) {
    var isHolding by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(220.dp, 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        onUnlock()
                        tryAwaitRelease()
                        isHolding = false
                        onPurge()
                    }
                )
            }
    ) {
        if (isHolding && message.isSafe) {
            // Unlocked image placeholder
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                Text("Image Content Viewable", color = Color.White, fontSize = 12.sp)
            }
        } else {
            // Blurred state with shield overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (isHolding) 0.dp else 20.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "AI Safe Shield",
                    tint = Color(0xFF9D00FF),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AI Safe Shield",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (message.text == "[PURGED]") "Media Purged" else "Hold to Reveal",
                    color = Color(0xFF938F99),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.ime), // To handle keyboard perfectly
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (text.isEmpty()) {
                Text("Message...", color = Color(0xFF938F99), fontSize = 15.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color(0xFFD000FF)),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (text.isNotBlank()) listOf(Color(0xFF9D00FF), Color(0xFFD000FF)) 
                                 else listOf(Color(0xFF333333), Color(0xFF333333))
                    ),
                    shape = CircleShape
                )
                .clickable(enabled = text.isNotBlank()) {
                    onSendMessage(text)
                    text = ""
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Color.White else Color(0xFF666666),
                modifier = Modifier.size(20.dp).offset(x = 2.dp)
            )
        }
    }
}
