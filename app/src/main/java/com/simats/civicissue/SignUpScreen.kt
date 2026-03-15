package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onBack: () -> Unit, onVerifyAccount: () -> Unit, onLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedCountryCode by remember { mutableStateOf("+91") }
    var countryCodeExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val countryCodes = listOf(
        "+91" to "India", "+1" to "USA/Canada", "+44" to "UK", "+61" to "Australia", 
        "+81" to "Japan", "+86" to "China", "+49" to "Germany", "+33" to "France", 
        "+39" to "Italy", "+7" to "Russia", "+55" to "Brazil", "+27" to "South Africa",
        "+971" to "UAE", "+65" to "Singapore", "+60" to "Malaysia", "+64" to "New Zealand",
        "+94" to "Sri Lanka", "+880" to "Bangladesh", "+92" to "Pakistan", "+977" to "Nepal",
        "+34" to "Spain", "+52" to "Mexico", "+31" to "Netherlands", "+46" to "Sweden",
        "+41" to "Switzerland", "+43" to "Austria", "+32" to "Belgium", "+45" to "Denmark",
        "+358" to "Finland", "+47" to "Norway", "+351" to "Portugal", "+30" to "Greece",
        "+353" to "Ireland", "+354" to "Iceland", "+352" to "Luxembourg", "+356" to "Malta",
        "+357" to "Cyprus", "+350" to "Gibraltar", "+359" to "Bulgaria", "+380" to "Ukraine",
        "+381" to "Serbia", "+385" to "Croatia", "+386" to "Slovenia", "+20" to "Egypt",
        "+90" to "Turkey", "+98" to "Iran", "+966" to "Saudi Arabia", "+62" to "Indonesia",
        "+82" to "South Korea", "+84" to "Vietnam", "+66" to "Thailand", "+63" to "Philippines",
        "+54" to "Argentina", "+57" to "Colombia", "+56" to "Chile", "+51" to "Peru",
        "+58" to "Venezuela", "+53" to "Cuba", "+212" to "Morocco", "+254" to "Kenya",
        "+234" to "Nigeria", "+233" to "Ghana", "+256" to "Uganda", "+255" to "Tanzania",
        "+260" to "Zambia", "+263" to "Zimbabwe"
    ).sortedBy { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Join CivicIssue",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Enter your details to create an account",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Up Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        SignUpTextField(label = "Full Name", value = fullName, onValueChange = { fullName = it }, placeholder = "John Doe")
                        Spacer(modifier = Modifier.height(20.dp))
                        SignUpTextField(label = "Email", value = email, onValueChange = { email = it }, placeholder = "example@email.com")
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(text = "Phone Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = countryCodeExpanded,
                                onExpandedChange = { countryCodeExpanded = it },
                                modifier = Modifier.weight(0.35f)
                            ) {
                                OutlinedTextField(
                                    value = selectedCountryCode,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedBorderColor = PrimaryBlue
                                    ),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryCodeExpanded) }
                                )
                                ExposedDropdownMenu(
                                    expanded = countryCodeExpanded,
                                    onDismissRequest = { countryCodeExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    countryCodes.forEach { (code, country) ->
                                        DropdownMenuItem(
                                            text = { Text("$country ($code)", color = Color.Black) },
                                            onClick = {
                                                selectedCountryCode = code
                                                countryCodeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                modifier = Modifier.weight(0.65f),
                                placeholder = { Text("00000 00000", color = Color.Gray) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = PrimaryBlue
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(text = "Password", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("••••••••", color = Color.Gray) },
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Default.Visibility
                                else Icons.Default.VisibilityOff

                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(text = "Confirm Password", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("••••••••", color = Color.Gray) },
                            trailingIcon = {
                                val image = if (confirmPasswordVisible)
                                    Icons.Default.Visibility
                                else Icons.Default.VisibilityOff

                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Client-side validation
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }
                        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all required fields"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val response = RetrofitClient.instance.signup(
                                    SignupRequest(
                                        full_name = fullName.trim(),
                                        email = email.trim(),
                                        password = password,
                                        phone_number = if (phoneNumber.isNotBlank()) phoneNumber.trim() else null,
                                        country_code = selectedCountryCode,
                                        role = "citizen"
                                    )
                                )
                                TokenManager.saveToken(response.access_token)
                                TokenManager.saveUser(response.user)
                                TokenManager.pendingEmail = email.trim()
                                onVerifyAccount()
                            } catch (e: retrofit2.HttpException) {
                                errorMessage = when (e.code()) {
                                    409 -> "Email already exists"
                                    422 -> "Invalid input. Please check your details."
                                    else -> "Signup failed: ${e.message()}"
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

                // Footer
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Already have an account? ")
                            withStyle(style = SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Log In")
                            }
                        },
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onLogin() }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SignUpTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = PrimaryBlue
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    CivicIssueTheme {
        SignUpScreen(onBack = {}, onVerifyAccount = {}, onLogin = {})
    }
}
