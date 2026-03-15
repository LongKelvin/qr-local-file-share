package com.example.qlfs.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qlfs.util.FileSizeFormatter
import kotlinx.coroutines.delay

@Composable
fun SharingActiveScreen(
    state: ShareUiState.SharingActive,
    onStopSharing: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedUrl by remember { mutableStateOf(false) }
    var copiedPassword by remember { mutableStateOf(false) }

    LaunchedEffect(copiedUrl) { if (copiedUrl) { delay(2000); copiedUrl = false } }
    LaunchedEffect(copiedPassword) { if (copiedPassword) { delay(2000); copiedPassword = false } }

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
            text = "Expires in ${String.format("%02d:%02d", minutes, seconds)}  ·  ${state.downloadCount} download(s)",
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Step 1: Join hotspot ──────────────────────────────────────────────
        StepCard(stepNumber = "1", stepLabel = "Join Hotspot", stepColor = Color(0xFF1A73E8)) {
            Image(
                bitmap = state.wifiQrBitmap.asImageBitmap(),
                contentDescription = "Wi-Fi QR code for ${state.ssid}",
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SSID row
            InfoRow(
                icon = Icons.Filled.Wifi,
                iconTint = Color(0xFF1A73E8),
                label = "Network",
                value = state.ssid
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Password row with copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Password",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.width(64.dp)
                )
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.password,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.password))
                        copiedPassword = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy password",
                        tint = if (copiedPassword) Color(0xFF059669) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (copiedPassword) {
                Text("Password copied!", fontSize = 11.sp, color = Color(0xFF059669))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Scan with camera app — or open Wi-Fi settings and enter the password above.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Step 2: Open download page ────────────────────────────────────────
        StepCard(stepNumber = "2", stepLabel = "Open Download Page", stepColor = Color(0xFF059669)) {
            Image(
                bitmap = state.downloadQrBitmap.asImageBitmap(),
                contentDescription = "Download QR code",
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // URL row with copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "URL",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.width(64.dp)
                )
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.downloadUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF374151),
                        maxLines = 2
                    )
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(state.downloadUrl))
                        copiedUrl = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy URL",
                        tint = if (copiedUrl) Color(0xFF059669) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (copiedUrl) {
                Text("URL copied!", fontSize = 11.sp, color = Color(0xFF059669))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Once connected to the hotspot, scan this QR or type the URL in any browser.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
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
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Stop Sharing", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFF6B7280), modifier = Modifier.width(64.dp))
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF111827))
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
