package com.example.qlfs.ui


import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.qlfs.model.SharedFile
import com.example.qlfs.util.FileSizeFormatter

@Composable
fun ShareScreen(
    state: ShareUiState,
    onFilesSelected: (List<Uri>, Context) -> Unit,
    onStartSharing: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        onFilesSelected(uris, context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Quick QR Share",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A73E8)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Share files instantly on your network",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (state is ShareUiState.FilesSelected) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val totalSize = state.files.sumOf { it.size }
                    Text(text = "${state.files.size} file(s) selected", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = FileSizeFormatter.format(totalSize), color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.files) { file ->
                            FilePreviewThumbnail(file)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onStartSharing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Sharing", fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { launcher.launch(arrayOf("*/*")) },
            colors = if (state is ShareUiState.FilesSelected) {
                ButtonDefaults.outlinedButtonColors()
            } else {
                ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (state is ShareUiState.FilesSelected) "Select Different Files" else "Select Files", fontSize = 18.sp)
        }
    }
}

@Composable
private fun FilePreviewThumbnail(file: SharedFile) {
    if (file.mimeType.startsWith("image/")) {
        AsyncImage(
            model = file.uri,
            contentDescription = file.name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        val (icon, bgColor, iconColor) = when {
            file.mimeType.startsWith("audio/") ->
                Triple(Icons.Filled.MusicNote, Color(0xFFEDE9FE), Color(0xFF7C3AED))
            file.mimeType.startsWith("video/") ->
                Triple(Icons.Filled.Movie, Color(0xFFFEE2E2), Color(0xFFDC2626))
            file.mimeType == "application/pdf" || file.mimeType.startsWith("text/") ->
                Triple(Icons.Filled.Description, Color(0xFFFEF3C7), Color(0xFFD97706))
            file.mimeType.startsWith("application/zip") ||
            file.mimeType.startsWith("application/x-") ->
                Triple(Icons.Filled.Archive, Color(0xFFD1FAE5), Color(0xFF059669))
            else ->
                Triple(Icons.Filled.InsertDriveFile, Color(0xFFF3F4F6), Color(0xFF6B7280))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
