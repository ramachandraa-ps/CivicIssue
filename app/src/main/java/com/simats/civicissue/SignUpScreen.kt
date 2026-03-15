package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
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
        Triple("+91", "\uD83C\uDDEE\uD83C\uDDF3", "India"), Triple("+1", "\uD83C\uDDFA\uD83C\uDDF8", "USA/Canada"), Triple("+44", "\uD83C\uDDEC\uD83C\uDDE7", "UK"), Triple("+61", "\uD83C\uDDE6\uD83C\uDDFA", "Australia"),
        Triple("+81", "\uD83C\uDDEF\uD83C\uDDF5", "Japan"), Triple("+86", "\uD83C\uDDE8\uD83C\uDDF3", "China"), Triple("+49", "\uD83C\uDDE9\uD83C\uDDEA", "Germany"), Triple("+33", "\uD83C\uDDEB\uD83C\uDDF7", "France"),
        Triple("+39", "\uD83C\uDDEE\uD83C\uDDF9", "Italy"), Triple("+7", "\uD83C\uDDF7\uD83C\uDDFA", "Russia"), Triple("+55", "\uD83C\uDDE7\uD83C\uDDF7", "Brazil"), Triple("+27", "\uD83C\uDDFF\uD83C\uDDE6", "South Africa"),
        Triple("+971", "\uD83C\uDDE6\uD83C\uDDEA", "UAE"), Triple("+65", "\uD83C\uDDF8\uD83C\uDDEC", "Singapore"), Triple("+60", "\uD83C\uDDF2\uD83C\uDDFE", "Malaysia"), Triple("+64", "\uD83C\uDDF3\uD83C\uDDFF", "New Zealand"),
        Triple("+94", "\uD83C\uDDF1\uD83C\uDDF0", "Sri Lanka"), Triple("+880", "\uD83C\uDDE7\uD83C\uDDE9", "Bangladesh"), Triple("+92", "\uD83C\uDDF5\uD83C\uDDF0", "Pakistan"), Triple("+977", "\uD83C\uDDF3\uD83C\uDDF5", "Nepal"),
        Triple("+34", "\uD83C\uDDEA\uD83C\uDDF8", "Spain"), Triple("+52", "\uD83C\uDDF2\uD83C\uDDFD", "Mexico"), Triple("+31", "\uD83C\uDDF3\uD83C\uDDF1", "Netherlands"), Triple("+46", "\uD83C\uDDF8\uD83C\uDDEA", "Sweden"),
        Triple("+41", "\uD83C\uDDE8\uD83C\uDDED", "Switzerland"), Triple("+43", "\uD83C\uDDE6\uD83C\uDDF9", "Austria"), Triple("+32", "\uD83C\uDDE7\uD83C\uDDEA", "Belgium"), Triple("+45", "\uD83C\uDDE9\uD83C\uDDF0", "Denmark"),
        Triple("+358", "\uD83C\uDDEB\uD83C\uDDEE", "Finland"), Triple("+47", "\uD83C\uDDF3\uD83C\uDDF4", "Norway"), Triple("+351", "\uD83C\uDDF5\uD83C\uDDF9", "Portugal"), Triple("+30", "\uD83C\uDDEC\uD83C\uDDF7", "Greece"),
        Triple("+353", "\uD83C\uDDEE\uD83C\uDDEA", "Ireland"), Triple("+354", "\uD83C\uDDEE\uD83C\uDDF8", "Iceland"), Triple("+352", "\uD83C\uDDF1\uD83C\uDDFA", "Luxembourg"), Triple("+356", "\uD83C\uDDF2\uD83C\uDDF9", "Malta"),
        Triple("+357", "\uD83C\uDDE8\uD83C\uDDFE", "Cyprus"), Triple("+350", "\uD83C\uDDEC\uD83C\uDDEE", "Gibraltar"), Triple("+359", "\uD83C\uDDE7\uD83C\uDDEC", "Bulgaria"), Triple("+380", "\uD83C\uDDFA\uD83C\uDDE6", "Ukraine"),
        Triple("+381", "\uD83C\uDDF7\uD83C\uDDF8", "Serbia"), Triple("+385", "\uD83C\uDDED\uD83C\uDDF7", "Croatia"), Triple("+386", "\uD83C\uDDF8\uD83C\uDDEE", "Slovenia"), Triple("+20", "\uD83C\uDDEA\uD83C\uDDEC", "Egypt"),
        Triple("+90", "\uD83C\uDDF9\uD83C\uDDF7", "Turkey"), Triple("+98", "\uD83C\uDDEE\uD83C\uDDF7", "Iran"), Triple("+966", "\uD83C\uDDF8\uD83C\uDDE6", "Saudi Arabia"), Triple("+62", "\uD83C\uDDEE\uD83C\uDDE9", "Indonesia"),
        Triple("+82", "\uD83C\uDDF0\uD83C\uDDF7", "South Korea"), Triple("+84", "\uD83C\uDDFB\uD83C\uDDF3", "Vietnam"), Triple("+66", "\uD83C\uDDF9\uD83C\uDDED", "Thailand"), Triple("+63", "\uD83C\uDDF5\uD83C\uDDED", "Philippines"),
        Triple("+54", "\uD83C\uDDE6\uD83C\uDDF7", "Argentina"), Triple("+57", "\uD83C\uDDE8\uD83C\uDDF4", "Colombia"), Triple("+56", "\uD83C\uDDE8\uD83C\uDDF1", "Chile"), Triple("+51", "\uD83C\uDDF5\uD83C\uDDEA", "Peru"),
        Triple("+58", "\uD83C\uDDFB\uD83C\uDDEA", "Venezuela"), Triple("+53", "\uD83C\uDDE8\uD83C\uDDFA", "Cuba"), Triple("+212", "\uD83C\uDDF2\uD83C\uDDE6", "Morocco"), Triple("+254", "\uD83C\uDDF0\uD83C\uDDEA", "Kenya"),
        Triple("+234", "\uD83C\uDDF3\uD83C\uDDEC", "Nigeria"), Triple("+233", "\uD83C\uDDEC\uD83C\uDDED", "Ghana"), Triple("+256", "\uD83C\uDDFA\uD83C\uDDEC", "Uganda"), Triple("+255", "\uD83C\uDDF9\uD83C\uDDFF", "Tanzania"),
        Triple("+260", "\uD83C\uDDFF\uD83C\uDDF2", "Zambia"), Triple("+263", "\uD83C\uDDFF\uD83C\uDDFC", "Zimbabwe")
    ).sortedBy { it.third }

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
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
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
                                modifier = Modifier.weight(0.40f)
                            ) {
                                val selectedFlag = countryCodes.find { it.first == selectedCountryCode }?.second ?: ""
                                OutlinedTextField(
                                    value = "$selectedFlag $selectedCountryCode",
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
                                    countryCodes.forEach { (code, flag, country) ->
                                        DropdownMenuItem(
                                            text = { Text("$flag $country ($code)", color = Color.Black) },
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
                                modifier = Modifier.weight(0.60f),
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
