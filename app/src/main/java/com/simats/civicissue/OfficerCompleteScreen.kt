package com.simats.civicissue

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.ByteArrayOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerCompleteScreen(
    complaintId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            capturedBitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Task", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FE)
    ) { paddingValues ->
        if (isSubmitting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(Modifier.height(16.dp))
                    Text("Submitting completion...", fontWeight = FontWeight.Medium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Your completion will be reviewed by an admin before being marked as resolved.",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Completion Notes
                Column {
                    Text("Completion Notes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Describe what was done *",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        placeholder = { Text("Describe the work completed, actions taken, and any observations...") },
                        label = { Text("Completion notes") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Proof Image Section
                Column {
                    Text("Capture Proof Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Upload a photo showing the completed work",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    val dashedStroke = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                    val dashedColor = PrimaryBlue.copy(alpha = 0.4f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (capturedBitmap != null) Color.White else Color(0xFFF8FAFF))
                            .then(
                                if (capturedBitmap == null) {
                                    Modifier.drawBehind {
                                        drawRoundRect(
                                            color = dashedColor,
                                            style = dashedStroke,
                                            cornerRadius = CornerRadius(12.dp.toPx())
                                        )
                                    }
                                } else {
                                    Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                }
                            )
                            .clickable { cameraLauncher.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                IconButton(
                                    onClick = { capturedBitmap = null },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { cameraLauncher.launch() }
                                ) {
                                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Camera", color = PrimaryBlue, fontSize = 12.sp)
                                }

                                VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = Color.LightGray)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Gallery", color = PrimaryBlue, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Submit Button
                val submitEnabled = notes.isNotBlank() && !isSubmitting
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val api = RetrofitClient.instance
                                val notesPart = notes.toRequestBody("text/plain".toMediaType())
                                var imagePart: MultipartBody.Part? = null
                                if (capturedBitmap != null) {
                                    val stream = ByteArrayOutputStream()
                                    capturedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                    val requestBody = stream.toByteArray().toRequestBody("image/jpeg".toMediaType())
                                    imagePart = MultipartBody.Part.createFormData("image", "proof.jpg", requestBody)
                                }
                                api.completeComplaint(complaintId, imagePart, notesPart)
                                Toast.makeText(context, "Completion submitted for review!", Toast.LENGTH_LONG).show()
                                onSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    enabled = submitEnabled
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (submitEnabled) Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                                ) else Brush.horizontalGradient(
                                    colors = listOf(Color.Gray, Color.Gray)
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Submit for Review", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
