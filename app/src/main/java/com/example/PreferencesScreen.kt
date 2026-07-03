package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// State Management
enum class MatchFilter(val label: String) {
    EVERYONE("Everyone 🌐"),
    BOYS("Boys 👦"),
    GIRLS("Girls 👧 \uD83D\uDC51") // Crown emoji
}

data class PreferencesState(
    val isPremium: Boolean = false,
    val selectedFilter: MatchFilter = MatchFilter.EVERYONE,
    val showPaywall: Boolean = false,
    val isSearching: Boolean = false
)

class PreferencesViewModel : ViewModel() {
    private val _state = MutableStateFlow(PreferencesState())
    val state: StateFlow<PreferencesState> = _state.asStateFlow()

    fun onFilterSelected(filter: MatchFilter) {
        if (filter == MatchFilter.GIRLS && !_state.value.isPremium) {
            _state.value = _state.value.copy(showPaywall = true)
        } else {
            _state.value = _state.value.copy(selectedFilter = filter)
        }
    }

    fun dismissPaywall() {
        _state.value = _state.value.copy(showPaywall = false)
    }

    fun upgradeToPremium() {
        _state.value = _state.value.copy(
            isPremium = true,
            showPaywall = false,
            selectedFilter = MatchFilter.GIRLS
        )
    }

    fun toggleSearch() {
        _state.value = _state.value.copy(isSearching = !_state.value.isSearching)
    }
}

// Cyberpunk Colors
val AmoledBlack = Color(0xFF000000)
val GlassmorphismBg = Color(0x33FFFFFF)
val GlassmorphismBorder = Color(0x1AFFFFFF)
val NeonPurpleStart = Color(0xFF9D00FF)
val NeonPurpleEnd = Color(0xFFD000FF)
val PaywallBgStart = Color(0xFF12072B)
val PaywallBgEnd = Color(0xFF000000)
val NeonGold = Color(0xFFFFD700)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFA0A0A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(viewModel: PreferencesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = AmoledBlack,
        contentColor = TextWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "DISCOVERY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(80.dp))
                
                // Main Interactive Centerpiece
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSearching) {
                        AmbientSonarRadar()
                    } else {
                        // Inactive radar core
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color(0xFF111111), CircleShape)
                                .border(1.dp, Color(0xFF333333), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Idle",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Selectors or Action Controls
                AnimatedVisibility(
                    visible = !state.isSearching,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "TARGET PREFERENCE",
                            fontSize = 10.sp,
                            color = TextGray,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        FilterSegment(
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = viewModel::onFilterSelected
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.toggleSearch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222222),
                                contentColor = TextWhite
                            ),
                            border = BorderStroke(1.dp, Color(0xFF444444))
                        ) {
                            Text("INITIATE SONAR SWEEP", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = state.isSearching,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = { viewModel.toggleSearch() },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x33FF0044),
                            contentColor = Color(0xFFFF5555)
                        ),
                        border = BorderStroke(1.dp, Color(0x88FF0044))
                    ) {
                        Text("ABORT SWEEP", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }

            if (state.showPaywall) {
                ModalBottomSheet(
                    onDismissRequest = viewModel::dismissPaywall,
                    containerColor = Color.Transparent,
                    dragHandle = null
                ) {
                    PaywallContent(onUpgrade = viewModel::upgradeToPremium)
                }
            }
        }
    }
}

@Composable
fun AmbientSonarRadar() {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    
    // Wave 1
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "w1s"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "w1a"
    )

    // Wave 2
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing, delayMillis = 1250),
            repeatMode = RepeatMode.Restart
        ),
        label = "w2s"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing, delayMillis = 1250),
            repeatMode = RepeatMode.Restart
        ),
        label = "w2a"
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sonar Waves
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = wave1Scale
                    scaleY = wave1Scale
                    alpha = wave1Alpha
                }
                .border(2.dp, NeonPurpleStart, CircleShape)
                .background(NeonPurpleStart.copy(alpha = 0.2f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = wave2Scale
                    scaleY = wave2Scale
                    alpha = wave2Alpha
                }
                .border(2.dp, NeonPurpleStart, CircleShape)
                .background(NeonPurpleStart.copy(alpha = 0.2f), CircleShape)
        )
        
        // Active Core
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurpleStart.copy(alpha = 0.5f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Brush.linearGradient(listOf(NeonPurpleStart, NeonPurpleEnd)), CircleShape)
                    .border(2.dp, TextWhite.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Searching",
                    tint = TextWhite,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun FilterSegment(
    selectedFilter: MatchFilter,
    onFilterSelected: (MatchFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassmorphismBg, RoundedCornerShape(24.dp))
            .border(1.dp, GlassmorphismBorder, RoundedCornerShape(24.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MatchFilter.values().forEach { filter ->
            val isSelected = filter == selectedFilter
            val isGirls = filter == MatchFilter.GIRLS

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onFilterSelected(filter) }
                    .then(
                        if (isSelected) {
                            Modifier.background(Color(0xFF333333))
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (isGirls) {
                            Modifier.border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = NeonGold,
                                shape = RoundedCornerShape(18.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected || isGirls) TextWhite else TextGray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun PaywallContent(onUpgrade: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(PaywallBgStart, PaywallBgEnd)))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                .padding(bottom = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "REFINE YOUR VIBES",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextWhite,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Unlock Gender Filter for ₹500/month",
            fontSize = 14.sp,
            color = NeonGold,
            fontWeight = FontWeight.Bold,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = NeonGold.copy(alpha = 0.5f),
                    blurRadius = 12f
                )
            )
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            FeatureRow("Infinite Girls/Boys Match")
            FeatureRow("Priority Queue Processing")
            FeatureRow("Verified Avatar Badge")
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { onUpgrade() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(NeonPurpleStart, NeonPurpleEnd)))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAY VIA UPI / NET BANKING",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = NeonGold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold
        )
    }
}
