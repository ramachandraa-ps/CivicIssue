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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyAccountScreen(onBack: () -> Unit, onVerify: () -> Unit) {
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val userEmail = TokenManager.pendingEmail ?: TokenManager.getUser()?.email ?: ""

    // Timer state
    var timeLeft by remember { mutableIntStateOf(60) }
    
    // Start countdown timer
    LaunchedEffect(key1 = timeLeft) {
        if (timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft -= 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Account", fontWeight = FontWeight.Bold) },
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
                    text = "Verify Account",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (userEmail.isNotEmpty()) "Enter 6 digit OTP sent to $userEmail" else "Enter 6 digit OTP sent to your email",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    otpValues.forEachIndexed { index, value ->
                        AccountOtpDigitBox(
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

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val resendText = if (timeLeft > 0) {
                        "Resend in 00:${timeLeft.toString().padStart(2, '0')}"
                    } else {
                        "Resend OTP"
                    }
                    
                    Text(
                        text = buildAnnotatedString {
                            if (timeLeft > 0) {
                                append("Didn't receive code? ")
                                withStyle(style = SpanStyle(color = Color.Gray, fontWeight = FontWeight.Normal)) {
                                    append(resendText)
                                }
                            } else {
                                append("Didn't receive code? ")
                                withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                    append(resendText)
                                }
                            }
                        },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable(enabled = timeLeft == 0) {
                            timeLeft = 60
                            scope.launch {
                                try {
                                    RetrofitClient.instance.resendOtp(
                                        ForgotPasswordRequest(email = userEmail)
                                    )
                                } catch (_: Exception) {
                                    errorMessage = "Failed to resend OTP"
                                }
                            }
                        }
                    )
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
                                RetrofitClient.instance.verifyEmail(
                                    VerifyEmailRequest(email = userEmail, otp_code = otpCode)
                                )
                                onVerify()
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
                        Text("Verify", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AccountOtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 45.dp, height = 56.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (value.isEmpty()) {
                Text(
                    text = "-",
                    fontSize = 20.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
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
fun VerifyAccountScreenPreview() {
    CivicIssueTheme {
        VerifyAccountScreen(onBack = {}, onVerify = {})
    }
}
