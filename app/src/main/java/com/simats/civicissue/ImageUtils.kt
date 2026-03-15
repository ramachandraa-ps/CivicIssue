package com.simats.civicissue

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

fun Bitmap.toMultipartPart(name: String = "image"): MultipartBody.Part {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 85, stream)
    val requestBody = stream.toByteArray().toRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData(name, "image.jpg", requestBody)
}

fun Uri.toMultipartPart(context: Context, name: String = "image"): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(this)
        ?: throw IllegalArgumentException("Cannot open URI: $this")
    val bytes = inputStream.readBytes()
    inputStream.close()
    val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData(name, "image.jpg", requestBody)
}
