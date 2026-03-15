package com.example.qlfs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qlfs.util.FileSizeFormatter

@Composable
fun SharingActiveScreen(
    state: ShareUiState.SharingActive,
    onStopSharing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(text = "Scan to Download", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        Image(
            bitmap = state.qrBitmap.asImageBitmap(),
            contentDescription = "QR code for downloading files. URL: ${state.url}",
            modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SelectionContainer {
            Text(
                text = state.url,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                items(state.files) { file ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            text = file.name,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(text = FileSizeFormatter.format(file.size), color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        
        Text(text = "Expires in $timeString", color = Color.Gray, fontWeight = FontWeight.Medium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = "Downloaded ${state.downloadCount} time(s)", color = Color.Gray)

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onStopSharing,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Stop Sharing")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
