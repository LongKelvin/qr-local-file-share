package com.example.qlfs

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                        CircularProgressIndicator()
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
                                        Card(modifier = Modifier.padding(16.dp)) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Error", color = Color.Red, style = MaterialTheme.typography.titleLarge)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(currentState.message)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(onClick = { viewModel.dismissError() }) {
                                                    Text("Dismiss")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Minimal floating info button — bottom-end, clear of system UI
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
            }
        }
    }
}
