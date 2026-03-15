package com.example.qlfs

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.qlfs.ui.AboutScreen
import com.example.qlfs.ui.ShareScreen
import com.example.qlfs.ui.ShareUiState
import com.example.qlfs.ui.ShareViewModel
import com.example.qlfs.ui.SharingActiveScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate {
                        AppContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionGate(content: @Composable () -> Unit) {
        val context = LocalContext.current

        // Build the list of permissions we need
        val permissions = buildList {
            // Hotspot: NEARBY_WIFI_DEVICES on API 33+, ACCESS_FINE_LOCATION on older
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // Notifications on API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        fun isGranted(perm: String) =
            ContextCompat.checkSelfPermission(context, perm) == PermissionChecker.PERMISSION_GRANTED

        var allGranted by remember {
            mutableStateOf(permissions.all { isGranted(it) })
        }
        var deniedPermanently by remember { mutableStateOf(false) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            allGranted = results.values.all { it }
            // If any was denied and we can't ask again, flag permanent denial
            if (!allGranted) {
                deniedPermanently = permissions.any { perm ->
                    !isGranted(perm) && !shouldShowRequestPermissionRationale(perm)
                }
            }
        }

        if (allGranted) {
            content()
            return
        }

        if (deniedPermanently) {
            PermissionDeniedScreen(
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )
            return
        }

        // Show rationale screen before requesting
        PermissionRationaleScreen(
            permissions = permissions,
            onGrant = { launcher.launch(permissions.toTypedArray()) }
        )
    }

    @Composable
    private fun PermissionRationaleScreen(
        permissions: List<String>,
        onGrant: () -> Unit
    ) {
        LaunchedEffect(Unit) {
            // Auto-launch the system dialog on first composition; the rationale
            // card below is shown while the dialog is pending or if the user
            // needs a second nudge.
            onGrant()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Permissions Required",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A73E8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "QLFS needs the following permissions to work:",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(24.dp))

            permissions.forEach { perm ->
                PermissionRow(perm)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            ) {
                Text("Grant Permissions", fontSize = 16.sp)
            }
        }
    }

    @Composable
    private fun PermissionRow(permission: String) {
        val (icon, label, reason) = permissionMeta(permission)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF111827))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(reason, fontSize = 12.sp, color = Color(0xFF6B7280), lineHeight = 16.sp)
                }
            }
        }
    }

    @Composable
    private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Permissions Denied",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Some required permissions were permanently denied. Please grant them in Settings to use QLFS.",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            ) {
                Text("Open App Settings", fontSize = 16.sp)
            }
        }
    }

    @Composable
    private fun AppContent() {
        val state by viewModel.uiState.collectAsState()
        var showAbout by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showAbout) {
                AboutScreen(onClose = { showAbout = false })
            } else {
                when (val currentState = state) {
                    is ShareUiState.Idle, is ShareUiState.FilesSelected -> {
                        ShareScreen(
                            state = currentState,
                            onFilesSelected = { uris: List<android.net.Uri>, context: android.content.Context ->
                                viewModel.onFilesSelectedFromPicker(uris, context)
                            },
                            onStartSharing = { viewModel.onStartSharing() }
                        )
                    }
                    is ShareUiState.ServerStarting -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF1A73E8))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Starting hotspot…", color = Color(0xFF6B7280))
                            }
                        }
                    }
                    is ShareUiState.SharingActive -> {
                        SharingActiveScreen(
                            state = currentState,
                            onStopSharing = { viewModel.onStopSharing() }
                        )
                    }
                    is ShareUiState.Stopped -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Server stopped")
                        }
                    }
                    is ShareUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(modifier = Modifier.padding(24.dp)) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Error", color = Color.Red, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(currentState.message, fontSize = 14.sp, lineHeight = 20.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.dismissError() }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = { showAbout = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About",
                        tint = Color(0xFF1A73E8).copy(alpha = 0.55f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    private fun permissionMeta(permission: String): Triple<ImageVector, String, String> = when (permission) {
        Manifest.permission.NEARBY_WIFI_DEVICES -> Triple(
            Icons.Filled.Wifi,
            "Nearby Wi-Fi Devices",
            "Required to start a local Wi-Fi hotspot so others can connect and download your files."
        )
        Manifest.permission.ACCESS_FINE_LOCATION -> Triple(
            Icons.Filled.LocationOn,
            "Location (for Wi-Fi)",
            "Required on Android 12 and below to start a local Wi-Fi hotspot. Your location is never stored or shared."
        )
        Manifest.permission.POST_NOTIFICATIONS -> Triple(
            Icons.Filled.Notifications,
            "Notifications",
            "Shows a persistent notification while sharing is active so the server keeps running in the background."
        )
        else -> Triple(Icons.Outlined.Info, permission, "Required for app functionality.")
    }
}

