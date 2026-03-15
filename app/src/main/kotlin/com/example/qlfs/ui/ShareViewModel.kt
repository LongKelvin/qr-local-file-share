package com.example.qlfs.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qlfs.model.SharedFile
import com.example.qlfs.network.HotspotManager
import com.example.qlfs.network.NetworkManager
import com.example.qlfs.qr.QrGenerator
import com.example.qlfs.service.ShareForegroundService
import com.example.qlfs.share.ShareController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShareUiState {
    object Idle : ShareUiState()
    data class FilesSelected(val files: List<SharedFile>) : ShareUiState()
    object ServerStarting : ShareUiState()
    data class SharingActive(
        val wifiQrBitmap: Bitmap,
        val downloadQrBitmap: Bitmap,
        val ssid: String,
        val password: String,
        val downloadUrl: String,
        val files: List<SharedFile>,
        val remainingSeconds: Long,
        val downloadCount: Int,
        val captivePortalAttempted: Boolean = false
    ) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
    object Stopped : ShareUiState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val app: Application,
    private val shareController: ShareController,
    private val networkManager: NetworkManager,
    private val hotspotManager: HotspotManager
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    
    init {
        viewModelScope.launch {
            shareController.downloadCount.collect { count ->
                val currentState = _uiState.value
                if (currentState is ShareUiState.SharingActive) {
                    _uiState.value = currentState.copy(downloadCount = count)
                }
            }
        }

        viewModelScope.launch {
            shareController.serverError.collect { error ->
                if (error != null) {
                    _uiState.value = ShareUiState.Error(error)
                    stopService()
                }
            }
        }
    }

    fun onFilesSelectedFromPicker(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) {
            _uiState.value = ShareUiState.Idle
            return
        }

        val sharedFiles = mutableListOf<SharedFile>()
        for (uri in uris) {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                val name = documentFile.name ?: "Unknown"
                val size = documentFile.length()
                val mimeType = documentFile.type ?: "application/octet-stream"
                val id = java.util.UUID.randomUUID().toString()

                sharedFiles.add(
                    SharedFile(
                        id = id,
                        uri = uri,
                        name = name,
                        size = size,
                        mimeType = mimeType,
                        dateModified = documentFile.lastModified()
                    )
                )
            }
        }

        if (sharedFiles.isEmpty()) {
             _uiState.value = ShareUiState.Error("Could not read selected files.")
        } else {
             _uiState.value = ShareUiState.FilesSelected(sharedFiles)
        }
    }

    fun onStartSharing() {
        val currentState = _uiState.value
        if (currentState !is ShareUiState.FilesSelected) return
        
        _uiState.value = ShareUiState.ServerStarting

        viewModelScope.launch {
            // Step 1 — Start local-only hotspot → get SSID, password, and AP gateway IP
            val hotspotInfo = try {
                hotspotManager.startHotspot()
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(
                    "Could not start hotspot: ${e.message}\n\n" +
                    "Make sure the required permission is granted and no other app is using the hotspot."
                )
                return@launch
            }

            val ip = hotspotInfo.ip
            val port = 8080

            // Step 2 — Start the file-serving foreground service
            val serviceIntent = Intent(app, ShareForegroundService::class.java).apply {
                val listToPass = java.util.ArrayList(currentState.files)
                putParcelableArrayListExtra(ShareForegroundService.EXTRA_FILES, listToPass)
                putExtra(ShareForegroundService.EXTRA_PORT, port)
                putExtra(ShareForegroundService.EXTRA_GATEWAY_IP, ip)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(serviceIntent)
            } else {
                app.startService(serviceIntent)
            }

            delay(500) // let service bind and create session

            val session = shareController.activeSession
            if (session == null || !shareController.serviceRunning.value) return@launch

            // Step 3 — Build URLs and generate both QR codes
            val downloadUrl = "http://$ip:$port/?token=${session.token}"
            // Standard Wi-Fi QR format — Android/iOS cameras auto-prompt to join the network
            val wifiQrContent = "WIFI:T:WPA2;S:${hotspotInfo.ssid};P:${hotspotInfo.password};;"

            val wifiQrBitmap = QrGenerator.generate(wifiQrContent)
            val downloadQrBitmap = QrGenerator.generate(downloadUrl)

            _uiState.value = ShareUiState.SharingActive(
                wifiQrBitmap = wifiQrBitmap,
                downloadQrBitmap = downloadQrBitmap,
                ssid = hotspotInfo.ssid,
                password = hotspotInfo.password,
                downloadUrl = downloadUrl,
                files = currentState.files,
                remainingSeconds = session.remainingSeconds(),
                downloadCount = 0,
                captivePortalAttempted = false
            )

            startCountdown()
        }
    }

    fun onStopSharing() {
        stopService()
        _uiState.value = ShareUiState.Stopped
        viewModelScope.launch {
            delay(1000)
            if (_uiState.value is ShareUiState.Stopped) {
                _uiState.value = ShareUiState.Idle
            }
        }
    }

    fun dismissError() {
         _uiState.value = ShareUiState.Idle
    }

    private fun stopService() {
        countdownJob?.cancel()
        countdownJob = null
        hotspotManager.stopHotspot()
        val serviceIntent = Intent(app, ShareForegroundService::class.java).apply {
            action = ShareForegroundService.ACTION_STOP
        }
        app.startService(serviceIntent)
        shareController.reset()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentState = _uiState.value
                val session = shareController.activeSession
                if (currentState is ShareUiState.SharingActive && session != null) {
                    val remaining = session.remainingSeconds()
                    if (remaining <= 0) {
                        onStopSharing()
                        break
                    }
                    _uiState.value = currentState.copy(remainingSeconds = remaining)
                } else {
                    break
                }
            }
        }
    }



    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
