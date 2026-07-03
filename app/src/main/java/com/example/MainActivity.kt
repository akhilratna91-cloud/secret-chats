package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class Screen { PREFERENCES, CHAT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.PREFERENCES) }
            
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = { 
                        if (currentScreen == Screen.PREFERENCES) {
                            BottomNavBar(onNavigateToChat = { currentScreen = Screen.CHAT })
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.PREFERENCES -> PreferencesScreen()
                            Screen.CHAT -> ChatScreen(onBack = { currentScreen = Screen.PREFERENCES })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchmakerScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Matchmaker",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .background(PremiumBadgeBg, CircleShape)
                        .border(1.dp, PremiumBadgeBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PREMIUM",
                        color = PremiumBadgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Search Status Visualizer
            Box(
                modifier = Modifier.size(192.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer circles
                Box(modifier = Modifier.fillMaxSize().border(1.dp, Primary.copy(alpha = 0.2f), CircleShape))
                Box(modifier = Modifier.size(160.dp).border(1.dp, Primary.copy(alpha = 0.4f), CircleShape))
                Box(modifier = Modifier.size(112.dp).border(1.dp, Primary.copy(alpha = 0.6f), CircleShape))
                
                // Central Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Primary, CircleShape)
                        .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = Primary, spotColor = Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Searching",
                        tint = OnPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Floating Nodes
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-16).dp, y = 16.dp)
                        .size(12.dp)
                        .background(Primary, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-32).dp)
                        .size(8.dp)
                        .background(NodeSecondary, CircleShape)
                )
            }

            // Transaction Info
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Searching for connection...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = Primary
                )
                Text(
                    text = "Matching with active users in your region",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SESSION PARAMETERS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ID: FX-8293-T",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "YOUR IDENTITY", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Male", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "SEEKING", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Female", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "✦", fontSize = 10.sp, color = Primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(ActiveIndicator, CircleShape))
                            Text(text = "Firestore Transaction Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        var timer by remember { mutableStateOf(14) }
                        LaunchedEffect(Unit) {
                            while(true) {
                                delay(1000)
                                timer++
                            }
                        }
                        Text(
                            text = String.format("00:%02d", timer),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Button
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = OnPrimaryContainer
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "Stop Search",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(onNavigateToChat: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("⚡", "Match", isActive = true, onClick = { })
        NavItem("💬", "Chats", isActive = false, onClick = onNavigateToChat)
        NavItem("⚙️", "Settings", isActive = false, onClick = { })
    }
}

@Composable
fun NavItem(icon: String, label: String, isActive: Boolean, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .alpha(if (isActive) 1f else 0.6f)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF4A4458), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
        } else {
            Text(text = icon, fontSize = 20.sp)
        }
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true)
@Composable
fun MatchmakerScreenPreview() {
    MyApplicationTheme {
        MatchmakerScreen()
    }
}
