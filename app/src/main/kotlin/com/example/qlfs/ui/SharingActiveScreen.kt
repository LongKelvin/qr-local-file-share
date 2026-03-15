package com.example.qlfs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            .background(Color(0xFFF0F4FF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Share Files",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A73E8)
        )

        Spacer(modifier = Modifier.height(4.dp))

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        Text(
            text = "Session expires in ${String.format("%02d:%02d", minutes, seconds)}  ·  ${state.downloadCount} download(s)",
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Step 1: Connect to hotspot ────────────────────────────────────────
        StepCard(
            stepNumber = "1",
            stepLabel = "Connect to Hotspot",
            stepColor = Color(0xFF1A73E8)
        ) {
            Image(
                bitmap = state.wifiQrBitmap.asImageBitmap(),
                contentDescription = "Wi-Fi QR code for network ${state.wifiSsid}",
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.wifiSsid,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF111827)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scan with your camera app — it will ask to join this phone's Wi-Fi hotspot. Tap Yes.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Step 2: Scan to download ──────────────────────────────────────────
        StepCard(
            stepNumber = "2",
            stepLabel = "Scan to Download",
            stepColor = Color(0xFF059669)
        ) {
            Image(
                bitmap = state.downloadQrBitmap.asImageBitmap(),
                contentDescription = "Download QR code. URL: ${state.url}",
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SelectionContainer {
                    Text(
                        text = state.url,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF374151)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Once connected to the hotspot, scan this QR with any browser QR scanner or camera.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Files list ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FILES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9CA3AF),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                state.files.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = file.name,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = FileSizeFormatter.format(file.size),
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onStopSharing,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Stop Sharing", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    stepLabel: String,
    stepColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(stepColor, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stepLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF111827)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

