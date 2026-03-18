package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfficerScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }

    var departments by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }
    var selectedDepartment by remember { mutableStateOf<String?>(null) }
    var departmentExpanded by remember { mutableStateOf(false) }
    var isDepartmentsLoading by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch departments on launch
    LaunchedEffect(Unit) {
        try {
            departments = RetrofitClient.instance.getDepartments()
        } catch (_: Exception) {
        } finally {
            isDepartmentsLoading = false
        }
    }

    // Show snackbar for success/error
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            successMessage = null
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Officer", fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    text = "New Officer",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Enter the officer's details below",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Form Card
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
                        // Full Name
                        Text(text = "Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("John Doe", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Email
                        Text(text = "Email *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("officer@example.com", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )
                        if (email.isNotBlank() && !email.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                            Text("Please enter a valid email address", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password
                        Text(text = "Password *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Min 8 chars, A-z, 0-9, @#\$...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
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
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )

                        // Password strength indicators
                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val hasMinLength = password.length >= 8
                            val hasUppercase = password.any { it.isUpperCase() }
                            val hasLowercase = password.any { it.isLowerCase() }
                            val hasDigit = password.any { it.isDigit() }
                            val hasSpecial = password.any { !it.isLetterOrDigit() }
                            val passedCount = listOf(hasMinLength, hasUppercase, hasLowercase, hasDigit, hasSpecial).count { it }

                            val strengthColor = when {
                                passedCount <= 2 -> Color(0xFFEF4444)
                                passedCount <= 3 -> Color(0xFFF59E0B)
                                passedCount <= 4 -> Color(0xFF3B82F6)
                                else -> Color(0xFF10B981)
                            }
                            val strengthLabel = when {
                                passedCount <= 2 -> "Weak"
                                passedCount <= 3 -> "Fair"
                                passedCount <= 4 -> "Good"
                                else -> "Strong"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { passedCount / 5f },
                                    modifier = Modifier.weight(1f).height(4.dp),
                                    color = strengthColor,
                                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strengthLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = strengthColor)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                PasswordRule("At least 8 characters", hasMinLength)
                                PasswordRule("One uppercase letter (A-Z)", hasUppercase)
                                PasswordRule("One lowercase letter (a-z)", hasLowercase)
                                PasswordRule("One number (0-9)", hasDigit)
                                PasswordRule("One special character (@#\$%...)", hasSpecial)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Phone Number
                        Text(text = "Phone Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.filter { it.isDigit() }
                                if (digitsOnly.length <= 10) {
                                    phoneNumber = digitsOnly
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("9876543210", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = PrimaryBlue
                            )
                        )
                        if (phoneNumber.isNotBlank() && phoneNumber.length != 10) {
                            Text("Phone number must be exactly 10 digits", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Department
                        Text(text = "Department *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = departmentExpanded,
                            onExpandedChange = { departmentExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedDepartment ?: "",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        if (isDepartmentsLoading) "Loading departments..." else "Select a department",
                                        color = Color.Gray
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, tint = Color.Gray) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = PrimaryBlue
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = departmentExpanded,
                                onDismissRequest = { departmentExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (departments.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No departments available", color = Color.Gray) },
                                        onClick = { departmentExpanded = false }
                                    )
                                } else {
                                    departments.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { Text(dept.name, color = Color.Black) },
                                            onClick = {
                                                selectedDepartment = dept.name
                                                departmentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Designation
                        Text(text = "Designation", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Senior Inspector", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = Color.Gray) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
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
                        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all required fields"
                            return@Button
                        }
                        if (!email.trim().matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                            errorMessage = "Please enter a valid email address"
                            return@Button
                        }
                        if (phoneNumber.isNotBlank() && phoneNumber.length != 10) {
                            errorMessage = "Phone number must be exactly 10 digits"
                            return@Button
                        }
                        if (password.length < 8) {
                            errorMessage = "Password must be at least 8 characters"
                            return@Button
                        }
                        if (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() } || !password.any { !it.isLetterOrDigit() }) {
                            errorMessage = "Password must contain uppercase, lowercase, number, and special character"
                            return@Button
                        }
                        if (selectedDepartment == null) {
                            errorMessage = "Please select a department"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val api = RetrofitClient.instance
                                val request = OfficerCreateRequest(
                                    full_name = fullName.trim(),
                                    email = email.trim(),
                                    password = password,
                                    phone_number = phoneNumber.ifBlank { null },
                                    department = selectedDepartment,
                                    designation = designation.ifBlank { null }
                                )
                                api.createOfficer(request)
                                successMessage = "Officer created successfully"
                                onSuccess()
                            } catch (e: retrofit2.HttpException) {
                                errorMessage = when (e.code()) {
                                    409 -> "An account with this email already exists"
                                    422 -> "Invalid input. Please check the details."
                                    403 -> "You do not have permission to create officers"
                                    else -> "Failed to create officer: ${e.message()}"
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
                    enabled = !isLoading && fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && selectedDepartment != null
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Officer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
