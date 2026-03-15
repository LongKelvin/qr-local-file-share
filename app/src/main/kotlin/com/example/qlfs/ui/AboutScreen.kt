package com.example.qlfs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qlfs.R

@Composable
fun AboutScreen(onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close About",
                    tint = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App icon
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "QuickQRShare Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "QuickQRShare",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A73E8)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Open Source · Free to use",
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ABOUT",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "QuickQRShare lets you instantly share any file from your Android device to any device on the same local network — no extra apps, no cables, and no cloud needed. Just select your files, scan the generated QR code with any browser, and download.",
                    fontSize = 15.sp,
                    color = Color(0xFF374151),
                    lineHeight = 23.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Author card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "AUTHOR",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Long Kelvin",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Long Kelvin",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "github.com/LongKelvin",
                            fontSize = 13.sp,
                            color = Color(0xFF1A73E8),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/LongKelvin/")
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GitHub repo card — full tappable
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri("https://github.com/LongKelvin/qr-local-file-share") },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "View Source on GitHub",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LongKelvin / qr-local-file-share",
                        fontSize = 12.sp,
                        color = Color(0xFFBFDAFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
