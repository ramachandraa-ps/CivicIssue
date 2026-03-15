package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyOTPScreen(role: String = "Citizen", onBack: () -> Unit, onContinue: () -> Unit) {
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val userEmail = TokenManager.pendingEmail ?: ""

    var timeLeft by remember { mutableStateOf(59) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            canResend = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Reset OTP", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF0F4FF),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF0F4FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Verify Reset OTP",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Enter the 6-digit code sent to your device.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(48.dp))

                // OTP Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            otpValues.forEachIndexed { index, value ->
                                OtpDigitBox(
                                    value = value,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 1) {
                                            if (newValue.isNotEmpty()) {
                                                otpValues[index] = newValue
                                                if (index < 5) {
                                                    focusRequesters[index + 1].requestFocus()
                                                } else {
                                                    focusManager.clearFocus()
                                                }
                                            } else {
                                                otpValues[index] = ""
                                            }
                                        }
                                    },
                                    onBackspace = {
                                        if (otpValues[index].isEmpty() && index > 0) {
                                            focusRequesters[index - 1].requestFocus()
                                        }
                                        otpValues[index] = ""
                                    },
                                    modifier = Modifier.focusRequester(focusRequesters[index])
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val timerText = if (timeLeft > 0) {
                            String.format("Resend in 00:%02d", timeLeft)
                        } else {
                            "Resend Code"
                        }

                        Text(
                            text = buildAnnotatedString {
                                append("Didn't receive code? ")
                                withStyle(style = SpanStyle(
                                    color = if (canResend) PrimaryBlue else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )) {
                                    append(timerText)
                                }
                            },
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.clickable(enabled = canResend) {
                                timeLeft = 59
                                canResend = false
                                // Logic to resend OTP
                                println("OTP Resent")
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val otpCode = otpValues.joinToString("")
                        if (otpCode.length < 6) {
                            errorMessage = "Please enter the complete 6-digit OTP"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                RetrofitClient.instance.verifyOtp(
                                    VerifyOtpRequest(email = userEmail, otp_code = otpCode)
                                )
                                TokenManager.pendingOtp = otpCode
                                onContinue()
                            } catch (e: retrofit2.HttpException) {
                                errorMessage = when (e.code()) {
                                    400 -> "Invalid or expired OTP"
                                    else -> "Verification failed: ${e.message()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Something went wrong"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun OtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 45.dp, height = 56.dp),
        shape = RoundedCornerShape(12.dp),
        color = LogoCircleBlue
    ) {
        Box(contentAlignment = Alignment.Center) {
            TextField(
                value = value,
                onValueChange = {
                    if (it.isEmpty()) {
                        onBackspace()
                    } else {
                        onValueChange(it)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 1,
                singleLine = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerifyOTPScreenPreview() {
    CivicIssueTheme {
        VerifyOTPScreen(onBack = {}, onContinue = {})
    }
}
