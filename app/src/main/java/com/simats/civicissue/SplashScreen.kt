package com.simats.civicissue

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000L) // 2 seconds delay
        onNavigate()
    }

    // Animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "LogoAlpha"
    )
    val textOffsetY by animateIntAsState(
        targetValue = if (visible) 0 else 80,
        animationSpec = tween(durationMillis = 800, delayMillis = 300),
        label = "TextOffsetY"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300),
        label = "TextAlpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White, BackgroundBlue),
                        radius = 900f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo with fade-in
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "CivicIssue Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .alpha(logoAlpha)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // App Name with slide-up + fade-in
                Text(
                    text = "CivicIssue",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(textAlpha)
                        .offset { IntOffset(0, textOffsetY) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle with slide-up + fade-in
                Text(
                    text = "Smart Civic Reporting",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(textAlpha)
                        .offset { IntOffset(0, textOffsetY) }
                )

                // Powered by AI
                Text(
                    text = "Powered by AI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(textAlpha)
                        .offset { IntOffset(0, textOffsetY) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CivicIssueTheme {
        SplashScreen(onNavigate = {})
    }
}
